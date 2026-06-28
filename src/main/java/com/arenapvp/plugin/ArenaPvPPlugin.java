package com.arenapvp.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class ArenaPvPPlugin extends JavaPlugin {

    private ArenaManager arenaManager;
    private StatsManager statsManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        statsManager = new StatsManager(this);
        statsManager.load();

        arenaManager = new ArenaManager(this);
        arenaManager.loadAll();

        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new AdminItemListener(this), this);

        ArenaCommand arenaCommand = new ArenaCommand(this);
        getCommand("arena").setExecutor(arenaCommand);
        getCommand("arena").setTabCompleter(arenaCommand);

        getLogger().info("ArenaPvP active. " + arenaManager.getAll().size() + " arene(s) chargee(s).");
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            arenaManager.saveAll();
        }
        if (statsManager != null) {
            statsManager.save();
        }
        getLogger().info("ArenaPvP desactive.");
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }
}
