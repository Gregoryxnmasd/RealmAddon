package com.extracraft.realmaddon;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class WorldFolderWatcher {
    private final Plugin plugin;
    private final Logger logger;
    private final Executor executor;

    public WorldFolderWatcher(Plugin plugin, Executor executor) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.executor = executor;
    }

    public CompletableFuture<String> watchForNewWorld(Duration timeout) {
        return CompletableFuture.supplyAsync(() -> {
            Path container = Bukkit.getWorldContainer().toPath();
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                container.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
                long deadline = System.currentTimeMillis() + timeout.toMillis();
                while (System.currentTimeMillis() < deadline) {
                    WatchKey key = watchService.poll();
                    if (key == null) {
                        Thread.sleep(200);
                        continue;
                    }
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                            Path created = (Path) event.context();
                            if (created != null) {
                                return created.toString();
                            }
                        }
                    }
                    key.reset();
                }
            } catch (IOException | InterruptedException ex) {
                logger.warning("World folder watcher error: " + ex.getMessage());
                Thread.currentThread().interrupt();
            }
            return null;
        }, executor);
    }
}
