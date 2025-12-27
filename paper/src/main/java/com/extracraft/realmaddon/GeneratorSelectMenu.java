package com.extracraft.realmaddon;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class GeneratorSelectMenu implements Listener {
    private final PluginConfig config;
    private final Map<Integer, GeneratorOption> slotMap = new HashMap<>();
    private final BiConsumer<Player, GeneratorOption> selectionHandler;

    public GeneratorSelectMenu(PluginConfig config, BiConsumer<Player, GeneratorOption> selectionHandler) {
        this.config = config;
        this.selectionHandler = selectionHandler;
    }

    public Inventory createInventory() {
        int size = Math.max(9, ((config.generators().size() - 1) / 9 + 1) * 9);
        Inventory inventory = Bukkit.createInventory(null, size, ChatColor.DARK_GREEN + "Choose your Generator");
        slotMap.clear();
        int slot = 0;
        for (GeneratorOption option : config.generators()) {
            ItemStack item = new ItemStack(option.icon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + option.displayName());
                List<String> lore = option.lore().stream().map(line -> ChatColor.GRAY + line).toList();
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            slotMap.put(slot, option);
            slot++;
        }
        return inventory;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getView().getTitle().contains("Choose your Generator")) {
            event.setCancelled(true);
            GeneratorOption option = slotMap.get(event.getRawSlot());
            if (option != null) {
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Generator selected: " + option.displayName());
                selectionHandler.accept(player, option);
            }
        }
    }
}
