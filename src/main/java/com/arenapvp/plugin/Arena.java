package com.arenapvp.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

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

    // spawns[teamIndex] -> liste de Location possibles (on prend juste la 1ere definie en general)
    private final Map<Integer, Location> teamSpawns = new HashMap<>();

    // Etat de partie
    private State state = State.WAITING;
    private final Map<UUID, Integer> playerTeam = new HashMap<>(); // joueur -> index equipe (0-based)
    private final Map<Integer, Integer> teamScores = new HashMap<>();
    private final Map<UUID, Location> returnLocations = new HashMap<>(); // ou teleporter le joueur a la fin/depart

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

    public Map<Integer, Location> getTeamSpawns() {
        return teamSpawns;
    }

    public void setTeamSpawn(int teamIndex, Location loc) {
        teamSpawns.put(teamIndex, loc);
    }

    public boolean isFullyConfigured() {
        if (lobbyLocation == null || !hasVoidY()) return false;
        for (int i = 0; i < teamCount; i++) {
            if (!teamSpawns.containsKey(i)) return false;
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

    public void resetAll() {
        playerTeam.clear();
        returnLocations.clear();
        teamScores.clear();
        state = State.WAITING;
    }

    public static ChatColor[] defaultColorOrder() {
        return new ChatColor[]{ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE, ChatColor.DARK_BLUE, ChatColor.BLUE};
    }
}
