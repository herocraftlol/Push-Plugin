package com.push.plugin.gui;

import com.push.plugin.PushPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Commande /p, dediee pour l'instant a l'ouverture du GUI des arenes via /p gui.
 */
public class PCommand implements CommandExecutor, TabCompleter {

    private final PushPlugin plugin;

    public PCommand(PushPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("gui")) {
            sender.sendMessage(ChatColor.RED + "Usage: /p gui");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit etre executee par un joueur.");
            return true;
        }

        plugin.getArenaGUI().open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("gui").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
