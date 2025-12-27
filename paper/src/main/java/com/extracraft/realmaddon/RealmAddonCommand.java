package com.extracraft.realmaddon;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class RealmAddonCommand implements CommandExecutor, TabCompleter {
    private final RealmAddonPlugin plugin;

    public RealmAddonCommand(RealmAddonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/realmaddon reload | set <player> <generatorKey> | debug <player>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("realmaddon.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(ChatColor.GREEN + "RealmAddon reloaded.");
                return true;
            }
            case "set" -> {
                if (!sender.hasPermission("realmaddon.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /realmaddon set <player> <generatorKey>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                String key = args[2];
                Optional<GeneratorOption> option = plugin.findGenerator(key);
                if (option.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "Unknown generator key.");
                    return true;
                }
                plugin.storeChoice(target.getUniqueId(), key);
                sender.sendMessage(ChatColor.GREEN + "Choice stored for " + target.getName());
                return true;
            }
            case "debug" -> {
                if (!sender.hasPermission("realmaddon.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /realmaddon debug <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                plugin.runDebug(sender, target);
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("reload");
            options.add("set");
            options.add("debug");
        } else if (args.length == 3 && "set".equalsIgnoreCase(args[0])) {
            if (plugin.getConfig().getConfigurationSection("generators") != null) {
                plugin.getConfig().getConfigurationSection("generators").getKeys(false)
                        .forEach(options::add);
            }
        }
        return options;
    }
}
