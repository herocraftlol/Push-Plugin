package com.push.plugin.gui;

import com.push.plugin.Arena;
import com.push.plugin.ArenaManager;
import com.push.plugin.PushPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** GUI ouverte en cliquant l'item "Equipe" du lobby : choisir sa couleur d'equipe. */
public class TeamSelectGUI {

    public static final String GUI_TITLE = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Choisis ta couleur";

    private final PushPlugin plugin;

    public TeamSelectGUI(PushPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isTeamSelectTitle(String title) {
        return GUI_TITLE.equals(title);
    }

    public void open(Player player, Arena arena) {
        int size = Math.max(9, arena.getTeamCount() <= 4 ? 9 : 18);
        Inventory inv = Bukkit.createInventory(null, size, GUI_TITLE);
        ArenaManager manager = plugin.getArenaManager();
        int currentTeam = arena.getTeamOf(player.getUniqueId());

        for (int i = 0; i < arena.getTeamCount(); i++) {
            ChatColor color = manager.getTeamColor(arena, i);
            String name = manager.getTeamDisplayName(arena, i);
            int count = arena.getPlayersInTeam(i).size();
            int max = arena.getTeamSize();
            boolean full = count >= max;
            boolean mine = i == currentTeam;

            ItemStack item = new ItemStack(woolFor(color));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(color + "" + ChatColor.BOLD + name + (mine ? ChatColor.GREEN + " (ton equipe)" : ""));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Joueurs : " + (full ? ChatColor.RED : ChatColor.WHITE) + count + ChatColor.GRAY + "/" + max);
            lore.add("");
            if (mine) {
                lore.add(ChatColor.GREEN + "\u2714 C'est deja ton equipe");
            } else if (full) {
                lore.add(ChatColor.RED + "Equipe complete");
            } else {
                lore.add(ChatColor.YELLOW + "\u25B6 Clique pour rejoindre");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        player.openInventory(inv);
    }

    public Integer getTeamIndexAt(int slot, Arena arena) {
        return slot >= 0 && slot < arena.getTeamCount() ? slot : null;
    }

    private Material woolFor(ChatColor color) {
        return switch (color) {
            case DARK_PURPLE -> Material.MAGENTA_WOOL;
            case LIGHT_PURPLE -> Material.PINK_WOOL;
            case BLUE -> Material.PURPLE_WOOL;
            case DARK_BLUE -> Material.BLUE_WOOL;
            case RED -> Material.RED_WOOL;
            case GREEN -> Material.GREEN_WOOL;
            case YELLOW -> Material.YELLOW_WOOL;
            case AQUA -> Material.CYAN_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }
}
