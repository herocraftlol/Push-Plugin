package com.arenapvp.plugin;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gere la collection d'arenes : creation, suppression, persistance dans config.yml,
 * cycle de vie des parties (lobby -> demarrage -> fin -> reset), et teleportations.
 */
public class ArenaManager {

    private final ArenaPvPPlugin plugin;
    private final Map<String, Arena> arenas = new HashMap<>();

    // Tache de compte a rebours / fin de partie par arene (pour pouvoir l'annuler si besoin)
    private final Map<String, BukkitTaskHandle> endTasks = new HashMap<>();

    public ArenaManager(ArenaPvPPlugin plugin) {
        this.plugin = plugin;
    }

    // ================= Persistance =================

    public void loadAll() {
        arenas.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("arenas");
        if (root == null) return;

        for (String name : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(name);
            if (sec == null) continue;

            Arena arena = new Arena(name);

            if (sec.contains("lobby")) {
                arena.setLobbyLocation(deserializeLocation(sec.getConfigurationSection("lobby")));
            }
            if (sec.contains("voidY")) {
                arena.setVoidY(sec.getDouble("voidY"));
            }
            arena.setTeamCount(sec.getInt("teamCount", 2));
            arena.setTeamSize(sec.getInt("teamSize", 8));
            arena.setMinPlayersToStart(sec.getInt("minPlayersToStart", 2));

            ConfigurationSection spawns = sec.getConfigurationSection("teamSpawns");
            if (spawns != null) {
                for (String idxStr : spawns.getKeys(false)) {
                    int idx = Integer.parseInt(idxStr);
                    Location loc = deserializeLocation(spawns.getConfigurationSection(idxStr));
                    arena.setTeamSpawn(idx, loc);
                }
            }

            arenas.put(name.toLowerCase(), arena);
        }
    }

    public void saveAll() {
        for (Arena arena : arenas.values()) {
            saveArena(arena);
        }
        plugin.saveConfig();
    }

    public void saveArena(Arena arena) {
        String path = "arenas." + arena.getName();
        plugin.getConfig().set(path + ".teamCount", arena.getTeamCount());
        plugin.getConfig().set(path + ".teamSize", arena.getTeamSize());
        plugin.getConfig().set(path + ".minPlayersToStart", arena.getMinPlayersToStart());
        if (arena.getLobbyLocation() != null) {
            serializeLocation(path + ".lobby", arena.getLobbyLocation());
        }
        if (arena.hasVoidY()) {
            plugin.getConfig().set(path + ".voidY", arena.getVoidY());
        }
        for (Map.Entry<Integer, Location> e : arena.getTeamSpawns().entrySet()) {
            serializeLocation(path + ".teamSpawns." + e.getKey(), e.getValue());
        }
        plugin.saveConfig();
    }

    public void deleteArenaFromConfig(String name) {
        plugin.getConfig().set("arenas." + name, null);
        plugin.saveConfig();
    }

    private void serializeLocation(String path, Location loc) {
        plugin.getConfig().set(path + ".world", loc.getWorld().getName());
        plugin.getConfig().set(path + ".x", loc.getX());
        plugin.getConfig().set(path + ".y", loc.getY());
        plugin.getConfig().set(path + ".z", loc.getZ());
        plugin.getConfig().set(path + ".yaw", loc.getYaw());
        plugin.getConfig().set(path + ".pitch", loc.getPitch());
    }

    private Location deserializeLocation(ConfigurationSection sec) {
        if (sec == null) return null;
        World world = Bukkit.getWorld(sec.getString("world", ""));
        if (world == null) return null;
        return new Location(world,
                sec.getDouble("x"), sec.getDouble("y"), sec.getDouble("z"),
                (float) sec.getDouble("yaw"), (float) sec.getDouble("pitch"));
    }

    // ================= CRUD arenes =================

    public Arena create(String name) {
        String key = name.toLowerCase();
        if (arenas.containsKey(key)) return null;
        Arena arena = new Arena(name);
        arenas.put(key, arena);
        saveArena(arena);
        return arena;
    }

    public boolean delete(String name) {
        String key = name.toLowerCase();
        Arena arena = arenas.remove(key);
        if (arena == null) return false;
        deleteArenaFromConfig(arena.getName());
        return true;
    }

    public Arena get(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Collection<Arena> getAll() {
        return arenas.values();
    }

    /** Trouve l'arene dans laquelle se trouve un joueur (lobby ou partie), ou null. */
    public Arena getArenaOf(UUID uuid) {
        for (Arena arena : arenas.values()) {
            if (arena.isPlaying(uuid)) return arena;
        }
        return null;
    }

    // ================= Cycle de vie =================

    /**
     * Fait rejoindre un joueur dans le lobby d'une arene.
     * Assigne automatiquement l'equipe la moins remplie.
     */
    public boolean joinLobby(Player player, Arena arena) {
        if (!arena.isFullyConfigured()) {
            player.sendMessage(ChatColor.RED + "Cette arene n'est pas entierement configuree.");
            return false;
        }
        if (arena.getState() != Arena.State.WAITING) {
            player.sendMessage(ChatColor.RED + "Une partie est deja en cours sur cette arene.");
            return false;
        }
        if (getArenaOf(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "Tu es deja dans une arene.");
            return false;
        }
        int team = arena.findAvailableTeam();
        if (team == -1) {
            player.sendMessage(ChatColor.RED + "Cette arene est pleine.");
            return false;
        }

        arena.setReturnLocation(player.getUniqueId(), player.getLocation());
        arena.addPlayer(player.getUniqueId(), team);

        resetPlayerForLobby(player);
        player.teleport(arena.getLobbyLocation());

        ChatColor color = getTeamColor(arena, team);
        String teamName = getTeamDisplayName(arena, team);
        player.sendMessage(ChatColor.GREEN + "Tu as rejoint l'arene " + ChatColor.YELLOW + arena.getName()
                + ChatColor.GREEN + " dans l'equipe " + color + teamName + ChatColor.GREEN + " !");

        broadcastToArena(arena, ChatColor.GRAY + player.getName() + " a rejoint le lobby ("
                + arena.countPlayers() + "/" + arena.getMaxPlayers() + ")");

        giveAdminStartItem(player, arena);

        if (arena.countPlayers() >= arena.getMinPlayersToStart()) {
            startGame(arena);
        }
        return true;
    }

    public void leave(Player player) {
        Arena arena = getArenaOf(player.getUniqueId());
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Tu n'es dans aucune arene.");
            return;
        }
        removePlayerFromArena(player, arena, true);
    }

    /**
     * Retire un joueur d'une arene : nettoie l'equipe scoreboard, l'inventaire,
     * et le teleporte a sa position d'origine si teleportBack est vrai.
     */
    public void removePlayerFromArena(Player player, Arena arena, boolean teleportBack) {
        UUID uuid = player.getUniqueId();
        Location ret = arena.getReturnLocation(uuid);
        arena.removePlayer(uuid);
        removeFromScoreboardTeam(player, arena);
        resetPlayerInventory(player);

        if (teleportBack && ret != null) {
            player.teleport(ret);
        }

        // Si la partie est en cours et qu'il ne reste plus assez de joueurs/equipes, on pourrait l'arreter.
        if (arena.getState() == Arena.State.RUNNING) {
            checkForWinnerAfterDisconnect(arena);
        }

        // Si l'arene se vide completement, on la remet a zero proprement.
        if (arena.countPlayers() == 0 && arena.getState() != Arena.State.WAITING) {
            arena.resetAll();
        }
    }

    private void giveAdminStartItem(Player player, Arena arena) {
        if (player.hasPermission("arenapvp.admin")) {
            ItemStack diamond = new ItemStack(Material.DIAMOND, 1);
            org.bukkit.inventory.meta.ItemMeta meta = diamond.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + "Lancer la partie maintenant");
            meta.setLore(List.of(ChatColor.GRAY + "Clique pour demarrer la partie immediatement."));
            diamond.setItemMeta(meta);
            player.getInventory().setItem(0, diamond);
        }
    }

    private void resetPlayerForLobby(Player player) {
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.ADVENTURE);
        for (PotionEffect eff : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(eff.getType());
        }
    }

    private void resetPlayerInventory(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        for (PotionEffect eff : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(eff.getType());
        }
    }

    /** Lance manuellement la partie (utilise par le diamant admin ou /arena forcestart). */
    public void forceStart(Arena arena) {
        if (arena.getState() != Arena.State.WAITING) return;
        if (arena.countPlayers() < 1) return;
        startGame(arena);
    }

    private void startGame(Arena arena) {
        if (arena.getState() != Arena.State.WAITING) return;
        arena.setState(Arena.State.RUNNING);
        arena.resetScores();

        setupScoreboardTeams(arena);

        for (Map.Entry<UUID, Integer> entry : new HashMap<>(arena.getPlayerTeamMap()).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;
            int team = entry.getValue();
            teleportToTeamSpawn(player, arena, team);
            giveKit(player, arena);
            addToScoreboardTeam(player, arena, team);
        }

        broadcastToArena(arena, ChatColor.GOLD + "La partie commence ! Premiere equipe a "
                + plugin.getConfig().getInt("points-to-win", 5) + " points gagne !");
    }

    private void teleportToTeamSpawn(Player player, Arena arena, int team) {
        Location spawn = arena.getTeamSpawns().get(team);
        if (spawn != null) {
            player.teleport(spawn);
        }
    }

    public void giveKit(Player player, Arena arena) {
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);

        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        org.bukkit.inventory.meta.ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setUnbreakable(true);
        swordMeta.setDisplayName(ChatColor.GRAY + "Epee d'arene");
        sword.setItemMeta(swordMeta);

        ItemStack bow = new ItemStack(Material.BOW);
        org.bukkit.inventory.meta.ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.setUnbreakable(true);
        bowMeta.setDisplayName(ChatColor.GRAY + "Arc d'arene");
        bow.setItemMeta(bowMeta);

        ItemStack arrow = new ItemStack(Material.ARROW, 1);
        org.bukkit.inventory.meta.ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.setDisplayName(ChatColor.GRAY + "Fleche d'arene");
        arrow.setItemMeta(arrowMeta);

        player.getInventory().setItem(0, sword);
        player.getInventory().setItem(1, bow);
        player.getInventory().setItem(8, arrow);
        // Cette fleche n'est jamais consommee (voir GameListener#onShootBow qui annule sa
        // consommation a chaque tir) : elle sert seulement a autoriser Bukkit a declencher le tir.
    }

    /** Appelee quand un point est marque (kill ou chute dans le vide). */
    public void addPointAndCheckWin(Arena arena, int scoringTeam, Player victim) {
        int pointsToWin = plugin.getConfig().getInt("points-to-win", 5);
        int newScore = arena.addPoint(scoringTeam);

        ChatColor color = getTeamColor(arena, scoringTeam);
        String teamName = getTeamDisplayName(arena, scoringTeam);
        broadcastToArena(arena, color + teamName + ChatColor.WHITE + " marque un point ! ("
                + newScore + "/" + pointsToWin + ")");

        if (newScore >= pointsToWin) {
            endGame(arena, scoringTeam);
        }
    }

    /** Si une equipe entiere se deconnecte / quitte, on verifie s'il ne reste qu'une equipe vivante. */
    private void checkForWinnerAfterDisconnect(Arena arena) {
        Set<Integer> teamsWithPlayers = new HashSet<>();
        for (int team : arena.getPlayerTeamMap().values()) {
            teamsWithPlayers.add(team);
        }
        if (teamsWithPlayers.size() == 1) {
            int winner = teamsWithPlayers.iterator().next();
            endGame(arena, winner);
        } else if (teamsWithPlayers.isEmpty()) {
            arena.resetAll();
        }
    }

    private void endGame(Arena arena, int winningTeam) {
        if (arena.getState() != Arena.State.RUNNING) return;
        arena.setState(Arena.State.ENDING);

        ChatColor color = getTeamColor(arena, winningTeam);
        String teamName = getTeamDisplayName(arena, winningTeam);
        broadcastToArena(arena, ChatColor.GOLD + "" + ChatColor.BOLD + "Victoire de l'equipe "
                + color + teamName + ChatColor.GOLD + " !");

        int delay = plugin.getConfig().getInt("end-delay-seconds", 5);
        BukkitTaskHandle handle = new BukkitTaskHandle();
        endTasks.put(arena.getName().toLowerCase(), handle);

        handle.taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            finishAndReset(arena);
            endTasks.remove(arena.getName().toLowerCase());
        }, delay * 20L);
    }

    private void finishAndReset(Arena arena) {
        for (UUID uuid : new ArrayList<>(arena.getPlayerTeamMap().keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeFromScoreboardTeam(player, arena);
                resetPlayerInventory(player);
                Location ret = arena.getReturnLocation(uuid);
                if (ret != null) {
                    player.teleport(ret);
                }
                player.sendMessage(ChatColor.GREEN + "Tu as ete teleporte a ta position de depart.");
            }
        }
        arena.resetAll();
    }

    // ================= Scoreboard / equipes visuelles =================

    private String scoreboardTeamName(Arena arena, int teamIndex) {
        // Limite Bukkit : 16 caracteres pour le nom d'equipe scoreboard (1.21 tolere plus, mais on reste safe)
        String base = "apvp_" + arena.getName() + "_" + teamIndex;
        return base.length() > 16 ? base.substring(0, 16) : base;
    }

    private void setupScoreboardTeams(Arena arena) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (int i = 0; i < arena.getTeamCount(); i++) {
            String name = scoreboardTeamName(arena, i);
            Team team = board.getTeam(name);
            if (team == null) {
                team = board.registerNewTeam(name);
            }
            team.setColor(getTeamColor(arena, i));
            team.setAllowFriendlyFire(false);
            team.setCanSeeFriendlyInvisibles(true);
        }
    }

    private void addToScoreboardTeam(Player player, Arena arena, int teamIndex) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(scoreboardTeamName(arena, teamIndex));
        if (team != null) {
            team.addEntry(player.getName());
        }
    }

    private void removeFromScoreboardTeam(Player player, Arena arena) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (int i = 0; i < arena.getTeamCount(); i++) {
            Team team = board.getTeam(scoreboardTeamName(arena, i));
            if (team != null) {
                team.removeEntry(player.getName());
            }
        }
    }

    // ================= Couleurs / noms d'equipe =================

    public ChatColor getTeamColor(Arena arena, int teamIndex) {
        List<String> colors = plugin.getConfig().getStringList("team-colors");
        if (teamIndex < colors.size()) {
            try {
                return ChatColor.valueOf(colors.get(teamIndex).toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        ChatColor[] fallback = Arena.defaultColorOrder();
        return fallback[teamIndex % fallback.length];
    }

    public String getTeamDisplayName(Arena arena, int teamIndex) {
        List<String> names = plugin.getConfig().getStringList("team-display-names");
        if (teamIndex < names.size()) {
            return names.get(teamIndex);
        }
        return "Equipe " + (teamIndex + 1);
    }

    // ================= Utilitaires =================

    public void broadcastToArena(Arena arena, String message) {
        for (UUID uuid : arena.getPlayerTeamMap().keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    public List<Player> getOnlinePlayersInArena(Arena arena) {
        return arena.getPlayerTeamMap().keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /** Petite enveloppe pour garder une reference d'ID de tache planifiee. */
    private static class BukkitTaskHandle {
        int taskId;
    }
}
