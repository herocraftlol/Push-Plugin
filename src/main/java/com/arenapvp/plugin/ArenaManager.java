package com.arenapvp.plugin;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
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

        // Le joueur voit desormais le scoreboard prive de cette arene (sidebar des scores)
        player.setScoreboard(arena.getScoreboard());
        updateSidebar(arena);

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

        // Le joueur retrouve le scoreboard standard du serveur en quittant l'arene
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

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
        } else if (arena.getState() == Arena.State.WAITING) {
            updateSidebar(arena);
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
        clearArmor(player);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.ADVENTURE);
        for (PotionEffect eff : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(eff.getType());
        }
    }

    private void resetPlayerInventory(Player player) {
        player.getInventory().clear();
        clearArmor(player);
        player.setGameMode(GameMode.SURVIVAL);
        for (PotionEffect eff : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(eff.getType());
        }
    }

    /** Retire les 4 pieces d'armure du joueur (PlayerInventory#clear() ne touche pas les slots d'armure). */
    private void clearArmor(Player player) {
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
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
        arena.resetDamage();

        setupScoreboardTeams(arena);

        for (Map.Entry<UUID, Integer> entry : new HashMap<>(arena.getPlayerTeamMap()).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;
            int team = entry.getValue();
            teleportToTeamSpawn(player, arena, team);
            giveKit(player, arena);
            addToScoreboardTeam(player, arena, team);
        }

        updateSidebar(arena);

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
        clearArmor(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);

        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setUnbreakable(true);
        swordMeta.setDisplayName(ChatColor.GRAY + "Epee d'arene");
        sword.setItemMeta(swordMeta);

        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.setUnbreakable(true);
        bowMeta.setDisplayName(ChatColor.GRAY + "Arc d'arene");
        bow.setItemMeta(bowMeta);

        ItemStack arrow = createArenaArrow();

        int team = arena.getTeamOf(player.getUniqueId());
        Color armorColor = chatColorToColor(getTeamColor(arena, team));
        ItemStack helmet = createColoredArmor(Material.LEATHER_HELMET, armorColor);
        ItemStack chestplate = createColoredArmor(Material.LEATHER_CHESTPLATE, armorColor);
        ItemStack leggings = createColoredArmor(Material.LEATHER_LEGGINGS, armorColor);
        ItemStack boots = createColoredArmor(Material.LEATHER_BOOTS, armorColor);

        player.getInventory().setItem(0, sword);
        player.getInventory().setItem(1, bow);
        player.getInventory().setItem(8, arrow);
        // Cette fleche n'est jamais consommee (voir GameListener#onShootBow qui annule sa
        // consommation a chaque tir, et le controle periodique qui la restaure si besoin) :
        // elle sert seulement a autoriser Bukkit a declencher le tir.

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }

    /** Cree la fleche dediee d'arene (jamais reellement consommee, voir GameListener). */
    public static ItemStack createArenaArrow() {
        ItemStack arrow = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = arrow.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Fleche d'arene");
        arrow.setItemMeta(meta);
        return arrow;
    }

    /** Cree une piece d'armure en cuir, teintee a la couleur de l'equipe, et incassable. */
    private ItemStack createColoredArmor(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        meta.setUnbreakable(true);
        meta.setDisplayName(ChatColor.GRAY + "Armure d'arene");
        item.setItemMeta(meta);
        return item;
    }

    /** Convertit une ChatColor (texte) en une couleur RGB exploitable pour teindre une armure en cuir. */
    private Color chatColorToColor(ChatColor chatColor) {
        return switch (chatColor) {
            case BLACK -> Color.fromRGB(0, 0, 0);
            case DARK_BLUE -> Color.fromRGB(0, 0, 170);
            case DARK_GREEN -> Color.fromRGB(0, 170, 0);
            case DARK_AQUA -> Color.fromRGB(0, 170, 170);
            case DARK_RED -> Color.fromRGB(170, 0, 0);
            case DARK_PURPLE -> Color.fromRGB(170, 0, 170);
            case GOLD -> Color.fromRGB(255, 170, 0);
            case GRAY -> Color.fromRGB(170, 170, 170);
            case DARK_GRAY -> Color.fromRGB(85, 85, 85);
            case BLUE -> Color.fromRGB(85, 85, 255);
            case GREEN -> Color.fromRGB(85, 255, 85);
            case AQUA -> Color.fromRGB(85, 255, 255);
            case RED -> Color.fromRGB(255, 85, 85);
            case LIGHT_PURPLE -> Color.fromRGB(255, 85, 255);
            case YELLOW -> Color.fromRGB(255, 255, 85);
            case WHITE -> Color.fromRGB(255, 255, 255);
            default -> Color.fromRGB(255, 255, 255);
        };
    }

    /** Appelee quand un point est marque (kill ou chute dans le vide). */
    public void addPointAndCheckWin(Arena arena, int scoringTeam, Player victim) {
        int pointsToWin = plugin.getConfig().getInt("points-to-win", 5);
        int newScore = arena.addPoint(scoringTeam);
        updateSidebar(arena);

        ChatColor color = getTeamColor(arena, scoringTeam);
        String teamName = getTeamDisplayName(arena, scoringTeam);
        broadcastToArena(arena, color + teamName + ChatColor.WHITE + " marque un point ! ("
                + newScore + "/" + pointsToWin + ")");

        if (newScore >= pointsToWin) {
            endGame(arena, scoringTeam);
        }
    }

    /** Enregistre les degats infliges par une equipe et rafraichit le sidebar. Appele a chaque coup porte. */
    public void addDamage(Arena arena, int teamIndex, double amount) {
        if (teamIndex < 0 || amount <= 0) return;
        arena.addDamage(teamIndex, amount);
        updateSidebar(arena);
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

        // Tous les joueurs de l'arene (gagnants comme perdants) passent en spectateur immediatement
        for (UUID uuid : arena.getPlayerTeamMap().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setGameMode(GameMode.SPECTATOR);
            }
        }

        ChatColor color = getTeamColor(arena, winningTeam);
        String teamName = getTeamDisplayName(arena, winningTeam);

        List<String> winnerNames = new ArrayList<>();
        for (UUID uuid : arena.getPlayersInTeam(winningTeam)) {
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : "Inconnu";
            winnerNames.add(name);
            plugin.getStatsManager().addWin(uuid, name);
        }

        broadcastToArena(arena, ChatColor.GOLD + "" + ChatColor.BOLD + "Victoire de l'equipe "
                + color + teamName + ChatColor.GOLD + " !");
        broadcastToArena(arena, color + "" + ChatColor.BOLD + teamName + ChatColor.GRAY
                + " : " + ChatColor.WHITE + String.join(ChatColor.GRAY + ", " + ChatColor.WHITE, winnerNames));

        updateSidebar(arena);

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
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
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
        Scoreboard board = arena.getScoreboard();
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
        Scoreboard board = arena.getScoreboard();
        Team team = board.getTeam(scoreboardTeamName(arena, teamIndex));
        if (team != null) {
            team.addEntry(player.getName());
        }
    }

    private void removeFromScoreboardTeam(Player player, Arena arena) {
        Scoreboard board = arena.getScoreboard();
        for (int i = 0; i < arena.getTeamCount(); i++) {
            Team team = board.getTeam(scoreboardTeamName(arena, i));
            if (team != null) {
                team.removeEntry(player.getName());
            }
        }
    }

    // ================= Sidebar (points + degats) =================

    private static final String SIDEBAR_OBJECTIVE = "apvp_side";

    /** Cree l'objectif sidebar de l'arene s'il n'existe pas encore. */
    private void ensureSidebar(Arena arena) {
        Scoreboard board = arena.getScoreboard();
        if (board.getObjective(SIDEBAR_OBJECTIVE) == null) {
            Objective obj = board.registerNewObjective(SIDEBAR_OBJECTIVE, Criteria.DUMMY,
                    ChatColor.GOLD + "" + ChatColor.BOLD + arena.getName());
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
    }

    /**
     * Rafraichit le contenu du sidebar de l'arene : pendant l'attente, affiche le nombre de
     * joueurs ; pendant/apres la partie, affiche pour chaque equipe ses points et le total de
     * degats infliges.
     */
    public void updateSidebar(Arena arena) {
        ensureSidebar(arena);
        Scoreboard board = arena.getScoreboard();
        Objective obj = board.getObjective(SIDEBAR_OBJECTIVE);
        if (obj == null) return;

        // On efface les anciennes lignes avant de reecrire, car les entrees sont des chaines libres
        for (String oldEntry : new ArrayList<>(arena.getSidebarEntries())) {
            board.resetScores(oldEntry);
        }
        arena.getSidebarEntries().clear();

        if (arena.getState() == Arena.State.WAITING) {
            String line = ChatColor.YELLOW + "En attente de joueurs... " + ChatColor.WHITE
                    + arena.countPlayers() + "/" + arena.getMaxPlayers();
            obj.getScore(line).setScore(0);
            arena.getSidebarEntries().add(line);
        } else {
            for (int i = 0; i < arena.getTeamCount(); i++) {
                ChatColor color = getTeamColor(arena, i);
                String teamName = getTeamDisplayName(arena, i);
                int points = arena.getScore(i);
                int damage = (int) Math.round(arena.getDamage(i));

                // Le code couleur final (invisible, sans texte derriere) garantit l'unicite de
                // la ligne meme si deux equipes affichaient par coincidence le meme texte.
                String line = color + teamName + ChatColor.GRAY + ": " + ChatColor.WHITE + points
                        + ChatColor.GRAY + " pts" + ChatColor.DARK_GRAY + " | " + ChatColor.RED
                        + damage + ChatColor.GRAY + " dgts" + ChatColor.values()[i % ChatColor.values().length];

                obj.getScore(line).setScore(points);
                arena.getSidebarEntries().add(line);
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
