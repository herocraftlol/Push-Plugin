package com.arenapvp.plugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Gere les statistiques persistantes des joueurs (nombre de victoires et
 * d'eliminations cumulees sur toutes les arenes), stockees dans stats.yml
 * a la racine du dossier du plugin.
 */
public class StatsManager {

    private final ArenaPvPPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public StatsManager(ArenaPvPPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Impossible de creer stats.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        if (config == null) return;
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de sauvegarder stats.yml", e);
        }
    }

    private String basePath(UUID uuid) {
        return "players." + uuid;
    }

    /** Incremente le nombre d'eliminations du joueur et persiste immediatement. */
    public void addKill(UUID uuid, String name) {
        int kills = getKills(uuid) + 1;
        config.set(basePath(uuid) + ".kills", kills);
        config.set(basePath(uuid) + ".name", name);
        save();
    }

    /** Incremente le nombre de victoires du joueur et persiste immediatement. */
    public void addWin(UUID uuid, String name) {
        int wins = getWins(uuid) + 1;
        config.set(basePath(uuid) + ".wins", wins);
        config.set(basePath(uuid) + ".name", name);
        save();
    }

    public int getKills(UUID uuid) {
        return config.getInt(basePath(uuid) + ".kills", 0);
    }

    public int getWins(UUID uuid) {
        return config.getInt(basePath(uuid) + ".wins", 0);
    }

    /**
     * Recherche l'UUID associe a un pseudo deja vu par le plugin (joueur deja
     * connecte au moins une fois alors qu'il etait dans une arene).
     * Retourne null si introuvable.
     */
    public UUID findUuidByName(String name) {
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) return null;
        for (String key : players.getKeys(false)) {
            String stored = players.getString(key + ".name", "");
            if (stored.equalsIgnoreCase(name)) {
                try {
                    return UUID.fromString(key);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return null;
    }
}
