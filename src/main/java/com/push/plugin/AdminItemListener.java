package com.push.plugin;

import com.push.plugin.gui.TeamSelectGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Gere les items speciaux du lobby d'attente : le diamant admin (slot 0, lance la partie
 * immediatement), la barriere pour quitter (slot 5) et l'item de selection d'equipe (slot 6).
 */
public class AdminItemListener implements Listener {

    private final PushPlugin plugin;

    public AdminItemListener(PushPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        Player player = event.getPlayer();
        String name = item.getItemMeta().getDisplayName();

        if (item.getType() == Material.DIAMOND && name.contains("Lancer la partie maintenant")) {
            event.setCancelled(true);
            handleForceStart(player);
            return;
        }

        if (item.getType() == Material.BARRIER && ChatColor.stripColor(name).equalsIgnoreCase("Quitter")) {
            event.setCancelled(true);
            plugin.getArenaManager().leave(player);
            return;
        }

        if (ChatColor.stripColor(name).startsWith("Equipe : ")) {
            event.setCancelled(true);
            handleTeamSelect(player);
        }
    }

    private void handleForceStart(Player player) {
        if (!player.hasPermission("push.admin")) {
            return;
        }

        ArenaManager manager = plugin.getArenaManager();
        Arena arena = manager.getArenaOf(player.getUniqueId());
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Tu n'es dans aucun lobby d'arene.");
            return;
        }
        if (arena.getState() != Arena.State.WAITING) {
            player.sendMessage(ChatColor.RED + "La partie a deja demarre sur cette arene.");
            return;
        }

        manager.forceStart(arena);
        player.sendMessage(ChatColor.GREEN + "Tu as force le demarrage de la partie.");
    }

    private void handleTeamSelect(Player player) {
        Arena arena = plugin.getArenaManager().getArenaOf(player.getUniqueId());
        if (arena == null || arena.getState() != Arena.State.WAITING) return;
        new TeamSelectGUI(plugin).open(player, arena);
    }
}

