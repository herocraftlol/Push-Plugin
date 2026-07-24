package com.arenapvp.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represente une arene de jeu : sa configuration (spawns, lobby, limite de vide,
 * nombre d'equipes, taille des equipes, couleurs) et son etat courant
 * (joueurs en lobby, joueurs en partie, scores, etc).
 */
public class Arena {

    public enum State {
        WAITING,   // lobby d'attente, partie pas encore lancee
        RUNNING,   // partie en cours
        ENDING     // partie terminee, teleportation de retour en cours
    }

    private final String name;

    // Configuration
    private Location lobbyLocation;
    private double voidY = Double.NaN; // hauteur sous laquelle un joueur est considere "tombe dans le vide"
    private int teamCount = 2;         // entre 2 et 4
    private int teamSize = 8;          // joueurs max par equipe, entre 1 et 8
    private int minPlayersToStart = 2; // minimum global de joueurs pour lancer automatiquement

    // Zone protegee : deux coins definissant le cuboid de l'arene
    private Location zonePos1 = null;
    private Location zonePos2 = null;

    // spawns[teamIndex] -> liste de Location possibles. Plusieurs spawns par equipe
    // permettent d'eviter que tous les joueurs d'une meme equipe apparaissent au meme
    // endroit quand teamSize > 1 (un spawn est tire au hasard parmi la liste a chaque
    // teleportation, comme dans HikaBrain).
    private final Map<Integer, List<Location>> teamSpawns = new HashMap<>();

    // Etat de partie
    private State state = State.WAITING;
    private final Map<UUID, Integer> playerTeam = new HashMap<>(); // joueur -> index equipe (0-based)
    private final Map<Integer, Integer> teamScores = new HashMap<>();
    private final Map<Integer, Double> teamDamage = new HashMap<>(); // degats infliges par chaque equipe durant la partie
    private final Map<UUID, Location> returnLocations = new HashMap<>(); // ou teleporter le joueur a la fin/depart

    // Scoreboard dedie a cette arene (sidebar des scores + equipes colorees), cree a la demande
    private Scoreboard scoreboard;
    // Memorise les lignes actuellement affichees sur le sidebar, pour pouvoir les effacer proprement
    private final List<String> sidebarEntries = new ArrayList<>();

    public Arena(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Location getLobbyLocation() {
        return lobbyLocation;
    }

    public void setLobbyLocation(Location lobbyLocation) {
        this.lobbyLocation = lobbyLocation;
    }

    public double getVoidY() {
        return voidY;
    }

    public void setVoidY(double voidY) {
        this.voidY = voidY;
    }

    public boolean hasVoidY() {
        return !Double.isNaN(voidY);
    }

    public int getTeamCount() {
        return teamCount;
    }

    public void setTeamCount(int teamCount) {
        this.teamCount = Math.max(2, Math.min(4, teamCount));
    }

    public int getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(int teamSize) {
        this.teamSize = Math.max(1, Math.min(8, teamSize));
    }

    public int getMinPlayersToStart() {
        return minPlayersToStart;
    }

    public void setMinPlayersToStart(int minPlayersToStart) {
        this.minPlayersToStart = Math.max(2, minPlayersToStart);
    }

    public int getMaxPlayers() {
        return teamCount * teamSize;
    }

    /**
     * Acces brut a la structure de spawns (utilise pour la sauvegarde/le chargement).
     */
    public Map<Integer, List<Location>> getTeamSpawns() {
        return teamSpawns;
    }

    /**
     * Retourne la liste (non modifiable) des spawns configures pour une equipe, dans
     * l'ordre de leur index (le premier element correspond au spawn "1").
     */
    public List<Location> getSpawns(int teamIndex) {
        List<Location> list = teamSpawns.get(teamIndex);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    /**
     * Nombre de spawns actuellement configures pour une equipe.
     */
    public int getSpawnCount(int teamIndex) {
        List<Location> list = teamSpawns.get(teamIndex);
        return list == null ? 0 : list.size();
    }

    /**
     * Definit (ou remplace) le spawn d'une equipe a l'index donne (1-based, comme vu par
     * l'admin en commande). Si l'index correspond au prochain spawn disponible
     * (liste.size() + 1), il est ajoute a la suite. Les "trous" ne sont pas autorises :
     * un admin doit definir le spawn 1 avant de pouvoir definir le spawn 2, etc.
     *
     * Renvoie false si l'index demande est invalide (trop grand, ou inferieur a 1).
     */
    public boolean setSpawn(int teamIndex, int spawnNumber, Location loc) {
        if (spawnNumber < 1) return false;
        List<Location> list = teamSpawns.computeIfAbsent(teamIndex, k -> new ArrayList<>());
        if (spawnNumber <= list.size()) {
            list.set(spawnNumber - 1, loc);
            return true;
        }
        if (spawnNumber == list.size() + 1) {
            list.add(loc);
            return true;
        }
        return false;
    }

    /**
     * Supprime le spawn d'une equipe a l'index donne (1-based). Renvoie false si l'index
     * est invalide.
     */
    public boolean removeSpawn(int teamIndex, int spawnNumber) {
        List<Location> list = teamSpawns.get(teamIndex);
        if (list == null || spawnNumber < 1 || spawnNumber > list.size()) return false;
        list.remove(spawnNumber - 1);
        return true;
    }

    /**
     * Renvoie un spawn choisi aleatoirement parmi tous ceux configures pour cette equipe.
     * Utile quand il y a plusieurs joueurs par equipe : ca evite qu'ils apparaissent tous
     * exactement au meme endroit. Renvoie null si aucun spawn n'est configure pour l'equipe.
     */
    public Location getRandomSpawn(int teamIndex) {
        List<Location> list = teamSpawns.get(teamIndex);
        if (list == null || list.isEmpty()) return null;
        if (list.size() == 1) return list.get(0);
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    public boolean isFullyConfigured() {
        if (lobbyLocation == null || !hasVoidY()) return false;
        for (int i = 0; i < teamCount; i++) {
            if (getSpawnCount(i) == 0) return false;
        }
        return true;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    // ---- Gestion des joueurs ----

    public Map<UUID, Integer> getPlayerTeamMap() {
        return playerTeam;
    }

    public void setReturnLocation(UUID uuid, Location loc) {
        returnLocations.put(uuid, loc);
    }

    public Location getReturnLocation(UUID uuid) {
        return returnLocations.get(uuid);
    }

    public List<UUID> getPlayersInTeam(int teamIndex) {
        List<UUID> list = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : playerTeam.entrySet()) {
            if (e.getValue() == teamIndex) list.add(e.getKey());
        }
        return list;
    }

    public int getTeamOf(UUID uuid) {
        return playerTeam.getOrDefault(uuid, -1);
    }

    public int countPlayers() {
        return playerTeam.size();
    }

    /**
     * Choisit l'equipe la moins remplie qui a encore de la place.
     * Retourne -1 si toutes les equipes sont pleines.
     */
    public int findAvailableTeam() {
        int bestTeam = -1;
        int bestCount = Integer.MAX_VALUE;
        for (int i = 0; i < teamCount; i++) {
            int count = getPlayersInTeam(i).size();
            if (count < teamSize && count < bestCount) {
                bestCount = count;
                bestTeam = i;
            }
        }
        return bestTeam;
    }

    public void addPlayer(UUID uuid, int teamIndex) {
        playerTeam.put(uuid, teamIndex);
    }

    public void removePlayer(UUID uuid) {
        playerTeam.remove(uuid);
        returnLocations.remove(uuid);
    }

    public boolean isPlaying(UUID uuid) {
        return playerTeam.containsKey(uuid);
    }

    // ---- Scores ----

    public int getScore(int teamIndex) {
        return teamScores.getOrDefault(teamIndex, 0);
    }

    public int addPoint(int teamIndex) {
        int newScore = getScore(teamIndex) + 1;
        teamScores.put(teamIndex, newScore);
        return newScore;
    }

    public void resetScores() {
        teamScores.clear();
    }

    // ---- Degats infliges (pour le sidebar) ----

    public double getDamage(int teamIndex) {
        return teamDamage.getOrDefault(teamIndex, 0.0);
    }

    public double addDamage(int teamIndex, double amount) {
        double newTotal = getDamage(teamIndex) + amount;
        teamDamage.put(teamIndex, newTotal);
        return newTotal;
    }

    public void resetDamage() {
        teamDamage.clear();
    }

    // ---- Scoreboard dedie (sidebar + equipes colorees) ----

    /** Cree (si besoin) et retourne le scoreboard prive de cette arene. */
    public Scoreboard getScoreboard() {
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        }
        return scoreboard;
    }

    public List<String> getSidebarEntries() {
        return sidebarEntries;
    }

    public void resetAll() {
        playerTeam.clear();
        returnLocations.clear();
        teamScores.clear();
        teamDamage.clear();
        state = State.WAITING;
    }

    public static ChatColor[] defaultColorOrder() {
        return new ChatColor[]{ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE, ChatColor.DARK_BLUE, ChatColor.BLUE};
    }

    /**
     * Palette de vitres teintees utilisee pour representer chaque equipe dans les GUIs
     * de configuration des spawns (voir gui/SpawnGUI). La palette depend du nombre
     * d'equipes de l'arene :
     *  - 2 equipes  -> Magenta / Rose (deux teintes bien distinctes, ideal pour un duel)
     *  - 3/4 equipes -> Violet / Bleu clair / Bleu (degrade violet-bleu, plus d'equipes
     *                    a differencier donc on etale sur plusieurs teintes de bleu)
     */
    public static Material[] spawnGlassColors(int teamCount) {
        Material[] palette;
        if (teamCount <= 2) {
            palette = new Material[]{Material.MAGENTA_STAINED_GLASS_PANE, Material.PINK_STAINED_GLASS_PANE};
        } else {
            palette = new Material[]{Material.PURPLE_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                    Material.BLUE_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE};
        }
        return Arrays.copyOf(palette, Math.min(teamCount, palette.length));
    }

    /**
     * Nom lisible (en francais) de la couleur associee a une equipe dans la palette
     * ci-dessus, pour affichage dans les GUIs/messages.
     */
    public static String spawnColorName(int teamCount, int teamIndex) {
        if (teamCount <= 2) {
            return teamIndex == 0 ? "Magenta" : "Rose";
        }
        String[] names = {"Violet", "Bleu clair", "Bleu", "Cyan"};
        return teamIndex < names.length ? names[teamIndex] : ("Equipe " + (teamIndex + 1));
    }

    // ---- Zone protegee ----

    public Location getZonePos1() { return zonePos1; }
    public void setZonePos1(Location loc) { this.zonePos1 = loc; }

    public Location getZonePos2() { return zonePos2; }
    public void setZonePos2(Location loc) { this.zonePos2 = loc; }

    public boolean hasZone() {
        return zonePos1 != null && zonePos2 != null
                && zonePos1.getWorld() != null && zonePos2.getWorld() != null
                && zonePos1.getWorld().equals(zonePos2.getWorld());
    }

    /**
     * Retourne true si la location est a l'interieur du cuboid defini par pos1 et pos2.
     */
    public boolean isInZone(Location loc) {
        if (!hasZone()) return false;
        if (loc.getWorld() == null || !loc.getWorld().equals(zonePos1.getWorld())) return false;

        double minX = Math.min(zonePos1.getX(), zonePos2.getX());
        double maxX = Math.max(zonePos1.getX(), zonePos2.getX());
        double minY = Math.min(zonePos1.getY(), zonePos2.getY());
        double maxY = Math.max(zonePos1.getY(), zonePos2.getY());
        double minZ = Math.min(zonePos1.getZ(), zonePos2.getZ());
        double maxZ = Math.max(zonePos1.getZ(), zonePos2.getZ());

        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
}
