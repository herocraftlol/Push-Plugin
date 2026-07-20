package com.push.plugin;

import com.push.plugin.gui.ArenaGUI;
import com.push.plugin.gui.ArenaGUIListener;
import org.bukkit.plugin.java.JavaPlugin;

public class PushPlugin extends JavaPlugin {

    private ArenaManager arenaManager;
    private StatsManager statsManager;
    private ArenaGUI arenaGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        statsManager = new StatsManager(this);
        statsManager.load();

        arenaManager = new ArenaManager(this);
        arenaManager.loadAll();

        arenaGUI = new ArenaGUI(this);

        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new AdminItemListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaGUIListener(this, arenaGUI), this);

        ArenaCommand arenaCommand = new ArenaCommand(this);
        getCommand("p").setExecutor(arenaCommand);
        getCommand("p").setTabCompleter(arenaCommand);

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

    public ArenaGUI getArenaGUI() {
        return arenaGUI;
    }
}
