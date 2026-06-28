package com.arenapvp.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ArenaCommand implements CommandExecutor, TabCompleter {

    private final ArenaPvPPlugin plugin;
    private final ArenaManager manager;

    public ArenaCommand(ArenaPvPPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getArenaManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setlobby" -> handleSetLobby(sender, args);
            case "setspawn" -> handleSetSpawn(sender, args);
            case "setvoid" -> handleSetVoid(sender, args);
            case "setteams" -> handleSetTeams(sender, args);
            case "setteamsize" -> handleSetTeamSize(sender, args);
            case "setminplayers" -> handleSetMinPlayers(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "join" -> handleJoin(sender, args);
            case "leave" -> handleLeave(sender);
            case "stats" -> handleStats(sender, args);
            case "forcestart" -> handleForceStart(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    // ---------------- Sous-commandes admin ----------------

    private void handleCreate(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /arena create <nom>");
            return;
        }
        String name = args[1];
        Arena arena = manager.create(name);
        if (arena == null) {
            sender.sendMessage(ChatColor.RED + "Une arene avec ce nom existe deja.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Arene " + ChatColor.YELLOW + name + ChatColor.GREEN
                + " creee. Configure-la avec /arena setlobby, setspawn, setvoid, setteams...");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /arena delete <nom>");
            return;
        }
        boolean ok = manager.delete(args[1]);
        sender.sendMessage(ok ? ChatColor.GREEN + "Arene supprimee."
                : ChatColor.RED + "Arene introuvable.");
    }

    private void handleSetLobby(CommandSender sender, String[] args) {
        if (!requirePlayerAdmin(sender)) return;
        Player player = (Player) sender;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /arena setlobby <nom>");
            return;
        }
        Arena arena = getArenaOrError(sender, args[1]);
        if (arena == null) return;

        arena.setLobbyLocation(player.getLocation());
        manager.saveArena(arena);
        sender.sendMessage(ChatColor.GREEN + "Point de lobby defini pour " + arena.getName() + ".");
    }

    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!requirePlayerAdmin(sender)) return;
        Player player = (Player) sender;
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /arena setspawn <nom> <numeroEquipe 1-4>");
            return;
        }
        Arena arena = getArenaOrError(sender, args[1]);
        if (arena == null) return;

        int teamNumber;
        try {
            teamNumber = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Le numero d'equipe doit etre un entier entre 1 et 4.");
            return;
        }
        if (teamNumber < 1 || teamNumber > 4) {
            sender.sendMessage(ChatColor.RED + "Le numero d'equipe doit etre entre 1 et 4.");
            return;
        }

        arena.setTeamSpawn(teamNumber - 1, player.getLocation());
        manager.saveArena(arena);
        sender.sendMessage(ChatColor.GREEN + "Spawn de l'equipe " + teamNumber + " defini pour "
                + arena.getName() + ".");
    }

    private void handleSetVoid(CommandSender sender, String[] args) {
        if (!requirePlayerAdmin(sender)) return;
        Player player = (Player) sender;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /arena setvoid <nom> [hauteurY optionnelle]");
            return;
        }
        Arena arena = getArenaOrError(sender, args[1]);
        if (arena == null) return;

        double y;
        if (args.length >= 3) {
            try {
                y = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Hauteur Y invalide.");
                return;
            }
        } else {
            y = player.getLocation().getY();
        }

        arena.setVoidY(y);
        manager.saveArena(arena);
        sender.sendMessage(ChatColor.GREEN + "Limite de vide definie a Y=" + y + " pour " + arena.getName()
                + ". Tout joueur descendant sous cette hauteur sera elimine.");
    }

    private void handleSetTeams(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /arena setteams <nom> <2-4>");
            return;
        }
        Arena arena = getArenaOrError(sender, args[1]);
        if (arena == null) return;

        int count;
        try {
            count = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Le nombre d'equipes doit etre un entier entre 2 et 4.");
            return;
        }
        if (count < 2 || count > 4) {
            sender.sendMessage(ChatColor.RED + "Le nombre d'equipes doit etre entre 2 et 4.");
            return;
        }
        arena.setTeamCount(count);
        manager.saveArena(arena);
        sender.sendMessage(ChatColor.GREEN + "Nombre d'equipes defini a " + count + " pour " + arena.getName()
                + ". N'oublie pas de definir les spawns correspondants avec /arena setspawn.");
    }

    private void handleSetTeamSize(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /arena setteamsize <nom> <1-8>");
            return;
        }
        Arena arena = getArenaOrError(sender, args[1]);
        if (arena == null) return;

        int size;
        try {
            size = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "La taille d'equipe doit etre un entier entre 1 et 8.");
            return;
        }
        if (size < 1 || size > 8) {
            sender.sendMessage(ChatColor.RED + "La taille d'equipe doit etre entre 1 et 8.");
            return;
        }
        arena.setTeamSize(size);
        manager.saveArena(arena);
        sender.sendMessage(ChatColor.GREEN + "Taille d'equipe definie a " + size + " joueur(s) max pour "
                + arena.getName() + ".");
    }

    private void handleSetMinPlayers(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /arena setminplayers <nom> <minimum>");
            return;
        }
        Arena arena = getArenaOrError(sender, args[1]);
        if (arena == null) return;

        int min;
        try {
            min = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Le minimum doit etre un entier.");
            return;
        }
        if (min < 2) {
            sender.sendMessage(ChatColor.RED + "Le minimum est de 2 joueurs.");
            return;
        }
        arena.setMinPlayersToStart(min);
        manager.saveArena(arena);
        sender.sendMessage(ChatColor.GREEN + "Minimum de joueurs pour demarrer automatiquement defini a "
                + min + " pour " + arena.getName() + ".");
    }

    private void handleReload(CommandSender sender) {
        if (!requireAdmin(sender)) return;
        plugin.reloadConfig();
        manager.loadAll();
        sender.sendMessage(ChatColor.GREEN + "Configuration rechargee.");
    }

    // ---------------- Sous-commandes joueur ----------------

    private void handleList(CommandSender sender) {
        if (manager.getAll().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Aucune arene n'a ete creee.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "Arenes disponibles :");
        for (Arena arena : manager.getAll()) {
            String status = switch (arena.getState()) {
                case WAITING -> ChatColor.GREEN + "en attente";
                case RUNNING -> ChatColor.RED + "en cours";
                case ENDING -> ChatColor.YELLOW + "termine";
            };
            sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + arena.getName()
                    + ChatColor.GRAY + " (" + status + ChatColor.GRAY + ", "
                    + arena.countPlayers() + "/" + arena.getMaxPlayers() + " joueurs, "
                    + arena.getTeamCount() + " equipes)"
                    + (arena.isFullyConfigured() ? "" : ChatColor.RED + " [non configuree]"));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /arena info <nom>");
            return;
        }
        Arena arena = getArenaOrError(sender, args[1]);
        if (arena == null) return;

        sender.sendMessage(ChatColor.GOLD + "=== Arene " + arena.getName() + " ===");
        sender.sendMessage(ChatColor.GRAY + "Etat: " + arena.getState());
        sender.sendMessage(ChatColor.GRAY + "Equipes: " + arena.getTeamCount()
                + " | Taille max/equipe: " + arena.getTeamSize()
                + " | Min pour demarrer: " + arena.getMinPlayersToStart());
        sender.sendMessage(ChatColor.GRAY + "Lobby defini: " + (arena.getLobbyLocation() != null ? "oui" : "NON"));
        sender.sendMessage(ChatColor.GRAY + "Limite de vide: " + (arena.hasVoidY() ? "Y=" + arena.getVoidY() : "NON"));
        for (int i = 0; i < arena.getTeamCount(); i++) {
            boolean has = arena.getTeamSpawns().containsKey(i);
            sender.sendMessage(ChatColor.GRAY + "Spawn equipe " + (i + 1) + ": " + (has ? "oui" : "NON"));
        }
        sender.sendMessage(ChatColor.GRAY + "Entierement configuree: "
                + (arena.isFullyConfigured() ? ChatColor.GREEN + "oui" : ChatColor.RED + "non"));
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Seul un joueur peut rejoindre une arene.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /arena join <nom>");
            return;
        }
        Arena arena = getArenaOrError(sender, args[1]);
        if (arena == null) return;

        manager.joinLobby(player, arena);
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Seul un joueur peut quitter une arene.");
            return;
        }
        manager.leave(player);
    }

    private void handleStats(CommandSender sender, String[] args) {
        // Accessible a tous les joueurs, pas besoin de permission admin
        StatsManager stats = plugin.getStatsManager();

        UUID targetUuid;
        String targetName;

        if (args.length >= 2) {
            targetName = args[1];
            Player online = Bukkit.getPlayer(targetName);
            if (online != null) {
                targetUuid = online.getUniqueId();
                targetName = online.getName();
            } else {
                UUID found = stats.findUuidByName(targetName);
                if (found == null) {
                    sender.sendMessage(ChatColor.RED + "Aucune statistique connue pour " + targetName + ".");
                    return;
                }
                targetUuid = found;
            }
        } else if (sender instanceof Player player) {
            targetUuid = player.getUniqueId();
            targetName = player.getName();
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /arena stats <joueur>");
            return;
        }

        int wins = stats.getWins(targetUuid);
        int kills = stats.getKills(targetUuid);

        sender.sendMessage(ChatColor.GOLD + "=== Statistiques de " + ChatColor.YELLOW + targetName + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.GRAY + "Victoires : " + ChatColor.GREEN + wins);
        sender.sendMessage(ChatColor.GRAY + "Eliminations : " + ChatColor.RED + kills);
    }

    private void handleForceStart(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        Arena arena;
        if (args.length >= 2) {
            arena = getArenaOrError(sender, args[1]);
            if (arena == null) return;
        } else if (sender instanceof Player player) {
            arena = manager.getArenaOf(player.getUniqueId());
            if (arena == null) {
                sender.sendMessage(ChatColor.RED + "Precise le nom de l'arene: /arena forcestart <nom>");
                return;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /arena forcestart <nom>");
            return;
        }
        manager.forceStart(arena);
        sender.sendMessage(ChatColor.GREEN + "Demarrage force pour " + arena.getName() + ".");
    }

    // ---------------- Helpers ----------------

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("arenapvp.admin")) {
            sender.sendMessage(ChatColor.RED + "Tu n'as pas la permission d'utiliser cette commande.");
            return false;
        }
        return true;
    }

    private boolean requirePlayerAdmin(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit etre executee par un joueur en jeu.");
            return false;
        }
        return requireAdmin(sender);
    }

    private Arena getArenaOrError(CommandSender sender, String name) {
        Arena arena = manager.get(name);
        if (arena == null) {
            sender.sendMessage(ChatColor.RED + "Arene inconnue: " + name);
        }
        return arena;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== ArenaPvP ===");
        sender.sendMessage(ChatColor.YELLOW + "/arena list" + ChatColor.GRAY + " - liste des arenes");
        sender.sendMessage(ChatColor.YELLOW + "/arena info <nom>" + ChatColor.GRAY + " - details d'une arene");
        sender.sendMessage(ChatColor.YELLOW + "/arena join <nom>" + ChatColor.GRAY + " - rejoindre le lobby");
        sender.sendMessage(ChatColor.YELLOW + "/arena leave" + ChatColor.GRAY + " - quitter l'arene actuelle");
        sender.sendMessage(ChatColor.YELLOW + "/arena stats [joueur]" + ChatColor.GRAY + " - victoires et eliminations");
        if (sender.hasPermission("arenapvp.admin")) {
            sender.sendMessage(ChatColor.AQUA + "--- Admin ---");
            sender.sendMessage(ChatColor.YELLOW + "/arena create <nom>");
            sender.sendMessage(ChatColor.YELLOW + "/arena delete <nom>");
            sender.sendMessage(ChatColor.YELLOW + "/arena setlobby <nom>");
            sender.sendMessage(ChatColor.YELLOW + "/arena setspawn <nom> <1-4>");
            sender.sendMessage(ChatColor.YELLOW + "/arena setvoid <nom> [y]");
            sender.sendMessage(ChatColor.YELLOW + "/arena setteams <nom> <2-4>");
            sender.sendMessage(ChatColor.YELLOW + "/arena setteamsize <nom> <1-8>");
            sender.sendMessage(ChatColor.YELLOW + "/arena setminplayers <nom> <min>");
            sender.sendMessage(ChatColor.YELLOW + "/arena forcestart [nom]");
            sender.sendMessage(ChatColor.YELLOW + "/arena reload");
        }
    }

    // ---------------- Tab completion ----------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> subs = new ArrayList<>(List.of("list", "info", "join", "leave", "stats"));
        if (sender.hasPermission("arenapvp.admin")) {
            subs.addAll(List.of("create", "delete", "setlobby", "setspawn", "setvoid",
                    "setteams", "setteamsize", "setminplayers", "forcestart", "reload"));
        }

        if (args.length == 1) {
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && List.of("delete", "setlobby", "setspawn", "setvoid", "setteams",
                "setteamsize", "setminplayers", "forcestart", "info", "join").contains(args[0].toLowerCase())) {
            return manager.getAll().stream()
                    .map(Arena::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setspawn")) {
            return List.of("1", "2", "3", "4");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setteams")) {
            return List.of("2", "3", "4");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setteamsize")) {
            return List.of("1", "2", "3", "4", "5", "6", "7", "8");
        }

        return Collections.emptyList();
    }
}
