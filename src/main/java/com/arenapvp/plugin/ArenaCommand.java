package com.arenapvp.plugin;

import com.arenapvp.plugin.gui.ArenaGUI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
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
            case "delspawn" -> handleDelSpawn(sender, args);
            case "spawns" -> handleSpawnsGui(sender, args);
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
            case "setpos1" -> handleSetPos(sender, args, 1);
            case "setpos2" -> handleSetPos(sender, args, 2);
            case "leaderboard" -> handleLeaderboard(sender, args);
            case "reload" -> handleReload(sender);
            case "gui" -> handleGui(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    // ---------------- Sous-commandes admin ----------------

    private void handleCreate(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /p create <nom>");
            return;
        }
        String name = args[1];
        Arena arena = manager.create(name);
        if (arena == null) {
            sender.sendMessage(ChatColor.RED + "Une arene avec ce nom existe deja.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Arene " + ChatColor.YELLOW + name + ChatColor.GREEN
                + " creee. Configure-la avec /p setlobby, setspawn, setvoid, setteams...");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /p delete <nom>");
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
            sender.sendMessage(ChatColor.RED + "Usage: /p setlobby <nom>");
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
            sender.sendMessage(ChatColor.RED + "Usage: /p setspawn <nom> <numeroEquipe 1-4> [numeroSpawn]");
            sender.sendMessage(ChatColor.GRAY + "Si [numeroSpawn] est omis, un nouveau spawn est ajoute a la suite "
                    + "(utile pour plusieurs joueurs par equipe).");
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
        int teamIndex = teamNumber - 1;

        int nextAvailable = arena.getSpawnCount(teamIndex) + 1;
        int spawnNumber;
        if (args.length >= 4) {
            try {
                spawnNumber = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Le numero de spawn doit etre un nombre entier (ex: 1, 2, 3...).");
                return;
            }
        } else {
            spawnNumber = nextAvailable;
        }

        boolean wasReplaced = spawnNumber <= (nextAvailable - 1);
        boolean ok = arena.setSpawn(teamIndex, spawnNumber, player.getLocation());
        if (!ok) {
            sender.sendMessage(ChatColor.RED + "Numero de spawn invalide. Le prochain numero disponible pour cette "
                    + "equipe est " + nextAvailable + ".");
            return;
        }
        manager.saveArena(arena);

        String colorName = Arena.spawnColorName(arena.getTeamCount(), teamIndex);
        sender.sendMessage(ChatColor.GREEN + "Spawn #" + spawnNumber + " de l'equipe " + teamNumber
                + ChatColor.GRAY + " (" + colorName + ")" + ChatColor.GREEN + " "
                + (wasReplaced ? "remplace" : "defini") + " pour " + arena.getName() + "."
                + ChatColor.GRAY + " (" + arena.getSpawnCount(teamIndex) + " spawn(s) au total pour cette equipe)");
    }

    private void handleDelSpawn(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /p delspawn <nom> <numeroEquipe 1-4> <numeroSpawn>");
            return;
        }
        Arena arena = getArenaOrError(sender, args[1]);
        if (arena == null) return;

        int teamNumber;
        int spawnNumber;
        try {
            teamNumber = Integer.parseInt(args[2]);
            spawnNumber = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Le numero d'equipe et le numero de spawn doivent etre des entiers.");
            return;
        }
        if (teamNumber < 1 || teamNumber > 4) {
            sender.sendMessage(ChatColor.RED + "Le numero d'equipe doit etre entre 1 et 4.");
            return;
        }
        int teamIndex = teamNumber - 1;

        boolean ok = arena.removeSpawn(teamIndex, spawnNumber);
        if (!ok) {
            sender.sendMessage(ChatColor.RED + "Numero de spawn invalide. Cette equipe a actuellement "
                    + arena.getSpawnCount(teamIndex) + " spawn(s).");
            return;
        }
        manager.saveArena(arena);
        sender.sendMessage(ChatColor.GREEN + "Spawn #" + spawnNumber + " de l'equipe " + teamNumber
                + " supprime pour " + arena.getName() + ".");
    }

    private void handleSpawnsGui(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit etre executee par un joueur.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /p spawns <nom>");
            return;
        }
        Arena arena = getArenaOrError(sender, args[1]);
        if (arena == null) return;

        plugin.getSpawnGUI().open(player, arena);
    }

    private void handleSetVoid(CommandSender sender, String[] args) {
        if (!requirePlayerAdmin(sender)) return;
        Player player = (Player) sender;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /p setvoid <nom> [hauteurY optionnelle]");
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
            sender.sendMessage(ChatColor.RED + "Usage: /p setteams <nom> <2-4>");
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
                + ". N'oublie pas de definir les spawns correspondants avec /p setspawn.");
    }

    private void handleSetTeamSize(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /p setteamsize <nom> <1-8>");
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
            sender.sendMessage(ChatColor.RED + "Usage: /p setminplayers <nom> <minimum>");
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

    private void handleSetPos(CommandSender sender, String[] args, int posNum) {
        if (!requirePlayerAdmin(sender)) return;
        Player player = (Player) sender;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /p setpos" + posNum + " <nom>");
            return;
        }
        Arena arena = getArenaOrError(sender, args[1]);
        if (arena == null) return;

        if (posNum == 1) {
            arena.setZonePos1(player.getLocation().getBlock().getLocation());
        } else {
            arena.setZonePos2(player.getLocation().getBlock().getLocation());
        }
        manager.saveArena(arena);

        String status = arena.hasZone()
                ? ChatColor.GREEN + " Zone completement definie !"
                : ChatColor.YELLOW + " Definis aussi /p setpos" + (posNum == 1 ? 2 : 1) + " pour completer la zone.";
        sender.sendMessage(ChatColor.GREEN + "Position " + posNum + " de la zone definie pour "
                + ChatColor.YELLOW + arena.getName() + ChatColor.GREEN + "." + status);
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
            sender.sendMessage(ChatColor.RED + "Usage: /p info <nom>");
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
            int count = arena.getSpawnCount(i);
            String colorName = Arena.spawnColorName(arena.getTeamCount(), i);
            sender.sendMessage(ChatColor.GRAY + "Spawns equipe " + (i + 1) + " (" + colorName + "): "
                    + (count > 0 ? ChatColor.GREEN + "" + count : ChatColor.RED + "0 - NON CONFIGUREE"));
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
            sender.sendMessage(ChatColor.RED + "Usage: /p join <nom>");
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

    private void handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit etre executee par un joueur.");
            return;
        }
        plugin.getArenaGUI().open(player);
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
            sender.sendMessage(ChatColor.RED + "Usage: /p stats <joueur>");
            return;
        }

        int wins = stats.getWins(targetUuid);
        int kills = stats.getKills(targetUuid);
        int damage = (int) Math.round(stats.getDamage(targetUuid));

        sender.sendMessage(ChatColor.GOLD + "=== Statistiques de " + ChatColor.YELLOW + targetName + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.GRAY + "Victoires : " + ChatColor.GREEN + wins);
        sender.sendMessage(ChatColor.GRAY + "Eliminations : " + ChatColor.RED + kills);
        sender.sendMessage(ChatColor.GRAY + "Degats totaux infliges : " + ChatColor.YELLOW + damage);
    }

    private void handleLeaderboard(CommandSender sender, String[] args) {
        if (!requirePlayerAdmin(sender)) return;
        Player player = (Player) sender;

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /p leaderboard <kills|wins|damage>");
            return;
        }

        StatsManager.StatType type;
        String typeName;
        switch (args[1].toLowerCase()) {
            case "kills" -> { type = StatsManager.StatType.KILLS; typeName = "Eliminations"; }
            case "wins"  -> { type = StatsManager.StatType.WINS;  typeName = "Victoires"; }
            case "damage"-> { type = StatsManager.StatType.DAMAGE; typeName = "Degats"; }
            default -> {
                sender.sendMessage(ChatColor.RED + "Type invalide. Utilise kills, wins ou damage.");
                return;
            }
        }

        StatsManager stats = plugin.getStatsManager();
        List<StatsManager.LeaderboardEntry> top = stats.getTopPlayers(type, 4);

        // Placer un panneau au mur devant le joueur
        Block target = player.getTargetBlockExact(5);
        Block signBlock;
        BlockFace face;

        if (target != null && target.getType().isSolid()) {
            // Panneau mural sur la face regardee
            face = getPlayerFacing(player);
            Block adjacent = target.getRelative(face);
            if (adjacent.getType() == Material.AIR) {
                signBlock = adjacent;
            } else {
                sender.sendMessage(ChatColor.RED + "Pas de place pour placer le panneau ici.");
                return;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Regarde un mur pour placer le panneau leaderboard.");
            return;
        }

        Material signMat = Material.OAK_WALL_SIGN;
        signBlock.setType(signMat);

        org.bukkit.block.data.BlockData bd = signBlock.getBlockData();
        if (bd instanceof WallSign ws) {
            ws.setFacing(face);
            signBlock.setBlockData(ws);
        }

        Sign sign = (Sign) signBlock.getState();
        String medal = switch (type) {
            case KILLS  -> "\u2694 TOP KILLS";
            case WINS   -> "\u2605 TOP WINS";
            case DAMAGE -> "\u2763 TOP DAMAGE";
        };
        sign.setLine(0, ChatColor.GOLD + "" + ChatColor.BOLD + medal);
        for (int i = 0; i < Math.min(top.size(), 3); i++) {
            StatsManager.LeaderboardEntry e = top.get(i);
            String prefix = switch (i) { case 0 -> ChatColor.GOLD+"#1 "; case 1 -> ChatColor.GRAY+"#2 "; default -> ChatColor.RED+"#3 "; };
            String valStr = (type == StatsManager.StatType.DAMAGE)
                    ? String.valueOf((int)Math.round(e.value()))
                    : String.valueOf((int)e.value());
            sign.setLine(i + 1, prefix + e.name() + " " + valStr);
        }
        sign.update();

        // Afficher aussi le top 5 en chat + stats du demandeur
        sender.sendMessage(ChatColor.GOLD + "=== Leaderboard : " + typeName + " ===");
        String[] medals = {"\u2741 ", "\u2742 ", "\u2743 ", "\u2744 ", "\u2745 "};
        for (int i = 0; i < top.size(); i++) {
            StatsManager.LeaderboardEntry e = top.get(i);
            String valStr = (type == StatsManager.StatType.DAMAGE)
                    ? String.valueOf((int)Math.round(e.value()))
                    : String.valueOf((int)e.value());
            ChatColor col = switch (i) { case 0 -> ChatColor.GOLD; case 1 -> ChatColor.GRAY; case 2 -> ChatColor.RED; default -> ChatColor.WHITE; };
            sender.sendMessage(col + "#" + (i+1) + " " + e.name() + ChatColor.WHITE + " - " + valStr);
        }
        // Stats perso du joueur qui execute la commande
        double myVal = switch (type) {
            case KILLS  -> stats.getKills(player.getUniqueId());
            case WINS   -> stats.getWins(player.getUniqueId());
            case DAMAGE -> stats.getDamage(player.getUniqueId());
        };
        String myValStr = (type == StatsManager.StatType.DAMAGE)
                ? String.valueOf((int)Math.round(myVal))
                : String.valueOf((int)myVal);
        sender.sendMessage(ChatColor.AQUA + "Tes " + typeName.toLowerCase() + " : " + ChatColor.WHITE + myValStr);
        sender.sendMessage(ChatColor.GREEN + "Panneau place devant toi.");
    }

    /** Retourne la direction a laquelle fait face le joueur (pour mur sign). */
    private BlockFace getPlayerFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) yaw += 360;
        if (yaw < 45 || yaw >= 315) return BlockFace.SOUTH;
        if (yaw < 135) return BlockFace.WEST;
        if (yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
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
                sender.sendMessage(ChatColor.RED + "Precise le nom de l'arene: /p forcestart <nom>");
                return;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /p forcestart <nom>");
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
        sender.sendMessage(ChatColor.YELLOW + "/p list" + ChatColor.GRAY + " - liste des arenes");
        sender.sendMessage(ChatColor.YELLOW + "/p info <nom>" + ChatColor.GRAY + " - details d'une arene");
        sender.sendMessage(ChatColor.YELLOW + "/p join <nom>" + ChatColor.GRAY + " - rejoindre le lobby");
        sender.sendMessage(ChatColor.YELLOW + "/p leave" + ChatColor.GRAY + " - quitter l'arene actuelle");
        sender.sendMessage(ChatColor.YELLOW + "/p stats [joueur]" + ChatColor.GRAY + " - victoires et eliminations");
        sender.sendMessage(ChatColor.YELLOW + "/p gui" + ChatColor.GRAY + " - ouvrir le menu des arenes");
        if (sender.hasPermission("arenapvp.admin")) {
            sender.sendMessage(ChatColor.AQUA + "--- Admin ---");
            sender.sendMessage(ChatColor.YELLOW + "/p create <nom>");
            sender.sendMessage(ChatColor.YELLOW + "/p delete <nom>");
            sender.sendMessage(ChatColor.YELLOW + "/p setlobby <nom>");
            sender.sendMessage(ChatColor.YELLOW + "/p setspawn <nom> <1-4> [numeroSpawn]" + ChatColor.GRAY
                    + " - ajoute/remplace un spawn (plusieurs possibles par equipe)");
            sender.sendMessage(ChatColor.YELLOW + "/p delspawn <nom> <1-4> <numeroSpawn>" + ChatColor.GRAY
                    + " - supprime un spawn d'equipe");
            sender.sendMessage(ChatColor.YELLOW + "/p spawns <nom>" + ChatColor.GRAY
                    + " - ouvrir le menu visuel des spawns (vitres colorees par equipe)");
            sender.sendMessage(ChatColor.YELLOW + "/p setvoid <nom> [y]");
            sender.sendMessage(ChatColor.YELLOW + "/p setpos1 <nom>" + ChatColor.GRAY + " - coin 1 de la zone protegee");
            sender.sendMessage(ChatColor.YELLOW + "/p setpos2 <nom>" + ChatColor.GRAY + " - coin 2 de la zone protegee");
            sender.sendMessage(ChatColor.YELLOW + "/p setteams <nom> <2-4>");
            sender.sendMessage(ChatColor.YELLOW + "/p setteamsize <nom> <1-8>");
            sender.sendMessage(ChatColor.YELLOW + "/p setminplayers <nom> <min>");
            sender.sendMessage(ChatColor.YELLOW + "/p forcestart [nom]");
            sender.sendMessage(ChatColor.YELLOW + "/p leaderboard <kills|wins|damage>" + ChatColor.GRAY + " - panneau top joueurs");
            sender.sendMessage(ChatColor.YELLOW + "/p reload");
        }
    }

    // ---------------- Tab completion ----------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> subs = new ArrayList<>(List.of("list", "info", "join", "leave", "stats", "gui"));
        if (sender.hasPermission("arenapvp.admin")) {
            subs.addAll(List.of("create", "delete", "setlobby", "setspawn", "delspawn", "spawns", "setvoid",
                    "setteams", "setteamsize", "setminplayers", "forcestart", "setpos1", "setpos2", "leaderboard", "reload"));
        }

        if (args.length == 1) {
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && List.of("delete", "setlobby", "setspawn", "delspawn", "spawns", "setvoid", "setteams",
                "setteamsize", "setminplayers", "forcestart", "setpos1", "setpos2", "info", "join").contains(args[0].toLowerCase())) {
            return manager.getAll().stream()
                    .map(Arena::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("setspawn") || args[0].equalsIgnoreCase("delspawn"))) {
            return List.of("1", "2", "3", "4");
        }
        if (args.length == 4 && (args[0].equalsIgnoreCase("setspawn") || args[0].equalsIgnoreCase("delspawn"))) {
            Arena arena = manager.get(args[1]);
            int nextAvailable = 1;
            if (arena != null) {
                try {
                    int teamIndex = Integer.parseInt(args[2]) - 1;
                    nextAvailable = arena.getSpawnCount(teamIndex) + 1;
                } catch (NumberFormatException ignored) {
                }
            }
            return List.of(String.valueOf(nextAvailable));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setteams")) {
            return List.of("2", "3", "4");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setteamsize")) {
            return List.of("1", "2", "3", "4", "5", "6", "7", "8");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("leaderboard")) {
            return List.of("kills", "wins", "damage").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
