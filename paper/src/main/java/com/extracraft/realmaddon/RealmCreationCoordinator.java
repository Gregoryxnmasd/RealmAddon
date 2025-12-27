package com.extracraft.realmaddon;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class RealmCreationCoordinator {
    private final Plugin plugin;
    private final Logger logger;
    private final ChoiceStore choiceStore;
    private final PluginConfig config;
    private final RealmCoreResolver resolver;
    private final IrisWorldManager irisWorldManager;
    private final WorldFolderWatcher watcher;

    public RealmCreationCoordinator(Plugin plugin,
                                    ChoiceStore choiceStore,
                                    PluginConfig config,
                                    RealmCoreResolver resolver,
                                    IrisWorldManager irisWorldManager,
                                    WorldFolderWatcher watcher) {
        this.plugin = plugin;
        this.choiceStore = choiceStore;
        this.config = config;
        this.resolver = resolver;
        this.irisWorldManager = irisWorldManager;
        this.watcher = watcher;
        this.logger = plugin.getLogger();
    }

    public void beginCreation(Player player, GeneratorOption option) {
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Optional<String> resolvedName = resolver.resolveWorldName(player);
            if (resolvedName.isPresent()) {
                prepareWorld(playerId, resolvedName.get(), option);
            } else {
                watcher.watchForNewWorld(Duration.ofSeconds(config.timeoutSeconds()))
                        .thenAccept(worldName -> {
                            if (worldName != null) {
                                prepareWorld(playerId, worldName, option);
                            } else {
                                logDebug("No world folder detected for " + player.getName());
                            }
                        });
            }
        });
    }

    private void prepareWorld(UUID playerId, String worldName, GeneratorOption option) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!irisWorldManager.isAvailable()) {
                logger.warning("Iris not available. Cannot create Iris world " + worldName);
                finishCreation(playerId, worldName);
                return;
            }
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            World existing = Bukkit.getWorld(worldName);
            if (existing != null && irisWorldManager.isIrisWorld(existing)) {
                logDebug("Realm world already Iris: " + worldName);
                finishCreation(playerId, worldName);
                return;
            }
            if (existing != null) {
                Bukkit.unloadWorld(existing, false);
            }
            if (worldFolder.exists()) {
                deleteWorldFolder(worldFolder.toPath());
            }
            try {
                irisWorldManager.createWorld(worldName, option);
                logDebug("Created Iris world " + worldName + " with " + option.key());
            } catch (Exception ex) {
                logger.warning("Failed to create Iris world: " + ex.getMessage());
            }
            finishCreation(playerId, worldName);
        });
    }

    private void finishCreation(UUID playerId, String worldName) {
        choiceStore.clearChoice(playerId);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            Bukkit.dispatchCommand(player, config.homeCommand());
        }
    }

    private void deleteWorldFolder(Path folder) {
        File[] files = folder.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                deleteWorldFolder(file.toPath());
            }
        }
        folder.toFile().delete();
    }

    private void logDebug(String message) {
        if (config.debug()) {
            logger.info("[RealmAddon] " + message);
        }
    }
}
