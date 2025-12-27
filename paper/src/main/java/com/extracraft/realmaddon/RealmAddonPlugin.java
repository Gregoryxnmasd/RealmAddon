package com.extracraft.realmaddon;

import com.extracraft.realmaddon.common.PluginChannels;
import com.extracraft.realmaddon.common.PluginMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class RealmAddonPlugin extends JavaPlugin implements Listener, PluginMessageListener {
    private PluginConfig pluginConfig;
    private ChoiceStore choiceStore;
    private ExecutorService executor;
    private GeneratorSelectMenu menu;
    private RealmCreationCoordinator coordinator;
    private RealmCoreResolver resolver;
    private IrisWorldManager irisWorldManager;
    private WorldFolderWatcher folderWatcher;
    private final Map<UUID, String> pendingCommand = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfiguration();
        Logger logger = getLogger();
        if (Bukkit.getPluginManager().getPlugin("RealmCore") == null) {
            logger.warning("RealmCore not found. RealmAddon will not function correctly.");
        }
        if (Bukkit.getPluginManager().getPlugin("Iris") == null) {
            logger.warning("Iris not found. RealmAddon will not create Iris worlds.");
        }
        menu = new GeneratorSelectMenu(pluginConfig, this::handleSelection);
        resolver = new RealmCoreResolver(this);
        irisWorldManager = new IrisWorldManager(this);
        folderWatcher = new WorldFolderWatcher(this, executor);
        coordinator = new RealmCreationCoordinator(this, choiceStore, pluginConfig, resolver, irisWorldManager, folderWatcher);
        Bukkit.getPluginManager().registerEvents(menu, this);
        Bukkit.getPluginManager().registerEvents(this, this);
        registerCommands();
        if (pluginConfig.mode() == PluginConfig.Mode.NETWORK_MESSAGING) {
            Bukkit.getMessenger().registerIncomingPluginChannel(this, PluginChannels.REALMADDON, this);
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, PluginChannels.REALMADDON);
        }
    }

    @Override
    public void onDisable() {
        if (choiceStore != null) {
            choiceStore.shutdown();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        loadConfiguration();
        menu = new GeneratorSelectMenu(pluginConfig, this::handleSelection);
        coordinator = new RealmCreationCoordinator(this, choiceStore, pluginConfig, resolver, irisWorldManager, folderWatcher);
    }

    private void loadConfiguration() {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newFixedThreadPool(4);
        }
        pluginConfig = PluginConfig.from(getConfig());
        if (choiceStore != null) {
            choiceStore.shutdown();
        }
        choiceStore = switch (pluginConfig.mode()) {
            case SINGLE -> new InMemoryChoiceStore();
            case NETWORK_MYSQL -> new MysqlChoiceStore(pluginConfig.mysqlSettings(), executor);
            case NETWORK_MESSAGING -> new InMemoryChoiceStore();
        };
    }

    private void registerCommands() {
        PluginCommand command = getCommand("realmaddon");
        if (command != null) {
            RealmAddonCommand executor = new RealmAddonCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String raw = event.getMessage().startsWith("/") ? event.getMessage().substring(1) : event.getMessage();
        List<String> args = Arrays.asList(raw.split("\\s+"));
        if (matchesCommand(args, pluginConfig.createCommand()) || matchesCommand(args, pluginConfig.resetCommand())) {
            if (!player.hasPermission("realmaddon.use")) {
                return;
            }
            event.setCancelled(true);
            String command = raw;
            pendingCommand.put(player.getUniqueId(), command);
            choiceStore.getChoice(player.getUniqueId()).thenAccept(choice ->
                    Bukkit.getScheduler().runTask(this, () ->
                            handleChoiceForCommand(player, command, choice)));
        }
    }

    private void handleChoiceForCommand(Player player, String command, Optional<String> choice) {
        if (choice.isPresent()) {
            Optional<GeneratorOption> option = findGenerator(choice.get());
            if (option.isPresent()) {
                dispatchRealmCommand(player, command, option.get());
                return;
            }
        }
        if (pluginConfig.generators().isEmpty()) {
            player.sendMessage(ChatColor.RED + "No generators configured.");
            return;
        }
        player.openInventory(menu.createInventory());
    }

    private void dispatchRealmCommand(Player player, String command, GeneratorOption option) {
        Bukkit.dispatchCommand(player, command);
        coordinator.beginCreation(player, option);
        pendingCommand.remove(player.getUniqueId());
    }

    private boolean matchesCommand(List<String> args, String command) {
        List<String> parts = Arrays.asList(command.split("\\s+"));
        if (args.size() < parts.size()) {
            return false;
        }
        for (int i = 0; i < parts.size(); i++) {
            if (!args.get(i).equalsIgnoreCase(parts.get(i))) {
                return false;
            }
        }
        return true;
    }

    private void handleSelection(Player player, GeneratorOption option) {
        String command = pendingCommand.getOrDefault(player.getUniqueId(), pluginConfig.createCommand());
        storeChoice(player.getUniqueId(), option.key());
        if (pluginConfig.mode() == PluginConfig.Mode.NETWORK_MESSAGING) {
            sendChoiceMessage(player, option.key());
        }
        dispatchRealmCommand(player, command, option);
    }

    void storeChoice(UUID playerId, String key) {
        choiceStore.setChoice(playerId, key);
    }

    void runDebug(org.bukkit.command.CommandSender sender, Player player) {
        Optional<String> worldName = resolver.resolveWorldName(player);
        sender.sendMessage(ChatColor.YELLOW + "Realm world name: " + worldName.orElse("<unresolved>"));
        if (worldName.isPresent()) {
            org.bukkit.World world = Bukkit.getWorld(worldName.get());
            sender.sendMessage(ChatColor.YELLOW + "Loaded: " + (world != null));
            if (world != null) {
                sender.sendMessage(ChatColor.YELLOW + "Iris world: " + irisWorldManager.isIrisWorld(world));
            }
        }
        choiceStore.getChoice(player.getUniqueId())
                .thenAccept(choice -> Bukkit.getScheduler().runTask(this, () ->
                        sender.sendMessage(ChatColor.YELLOW + "Pending choice: " + choice.orElse("<none>"))));
    }

    Optional<GeneratorOption> findGenerator(String key) {
        return pluginConfig.generators().stream()
                .filter(option -> option.key().equalsIgnoreCase(key))
                .findFirst();
    }

    private void sendChoiceMessage(Player player, String key) {
        byte[] payload = PluginMessage.encodeChoice(player.getUniqueId(), key, pluginConfig.messagingTargetServer());
        player.sendPluginMessage(this, PluginChannels.REALMADDON, payload);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!PluginChannels.REALMADDON.equals(channel)) {
            return;
        }
        PluginMessage.Message decoded = PluginMessage.decode(message);
        if (decoded != null) {
            storeChoice(decoded.playerId(), decoded.generatorKey());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        pendingCommand.remove(playerId);
        choiceStore.clearChoice(playerId);
    }
}
