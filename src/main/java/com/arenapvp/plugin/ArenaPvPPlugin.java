package com.arenapvp.plugin;

import com.arenapvp.plugin.gui.ArenaGUI;
import com.arenapvp.plugin.gui.ArenaGUIListener;
import com.arenapvp.plugin.gui.SpawnGUI;
import com.arenapvp.plugin.gui.SpawnGUIListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ArenaPvPPlugin extends JavaPlugin {

    private ArenaManager arenaManager;
    private StatsManager statsManager;
    private ArenaGUI arenaGUI;
    private SpawnGUI spawnGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        statsManager = new StatsManager(this);
        statsManager.load();

        arenaManager = new ArenaManager(this);
        arenaManager.loadAll();

        arenaGUI = new ArenaGUI(this);
        spawnGUI = new SpawnGUI(this);

        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new AdminItemListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaGUIListener(this, arenaGUI), this);
        getServer().getPluginManager().registerEvents(new SpawnGUIListener(this, spawnGUI), this);

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

    public SpawnGUI getSpawnGUI() {
        return spawnGUI;
    }
}
