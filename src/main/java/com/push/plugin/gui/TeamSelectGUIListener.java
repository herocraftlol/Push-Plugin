package com.push.plugin.gui;

import com.push.plugin.Arena;
import com.push.plugin.ArenaManager;
import com.push.plugin.PushPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class TeamSelectGUIListener implements Listener {

    private final PushPlugin plugin;
    private final TeamSelectGUI gui;

    public TeamSelectGUIListener(PushPlugin plugin, TeamSelectGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!gui.isTeamSelectTitle(event.getView().getTitle())) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        ArenaManager manager = plugin.getArenaManager();
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena == null || arena.getState() != Arena.State.WAITING) {
            player.closeInventory();
            return;
        }

        Integer teamIndex = gui.getTeamIndexAt(event.getRawSlot(), arena);
        if (teamIndex == null) return;

        if (manager.changeTeam(player, arena, teamIndex)) {
            player.closeInventory();
        } else {
            gui.open(player, arena);
        }
    }
}
