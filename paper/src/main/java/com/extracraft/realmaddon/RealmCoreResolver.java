package com.extracraft.realmaddon;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class RealmCoreResolver {
    private final Logger logger;
    private final Plugin realmCore;

    public RealmCoreResolver(Plugin plugin) {
        this.logger = plugin.getLogger();
        this.realmCore = Bukkit.getPluginManager().getPlugin("RealmCore");
    }

    public Optional<String> resolveWorldName(Player player) {
        if (realmCore == null) {
            return Optional.empty();
        }
        UUID uuid = player.getUniqueId();
        Set<Object> visited = new HashSet<>();
        Deque<Object> queue = new ArrayDeque<>();
        queue.add(realmCore);
        while (!queue.isEmpty()) {
            Object current = queue.poll();
            if (current == null || visited.contains(current)) {
                continue;
            }
            visited.add(current);
            Optional<String> resolved = invokeCandidate(current, uuid, player);
            if (resolved.isPresent()) {
                return resolved;
            }
            for (Field field : current.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(current);
                    if (value != null && !visited.contains(value)) {
                        queue.add(value);
                    }
                } catch (IllegalAccessException ignored) {
                    // skip
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> invokeCandidate(Object target, UUID uuid, Player player) {
        Method[] methods = target.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (!String.class.equals(method.getReturnType())) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!looksRelevant(name)) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            try {
                method.setAccessible(true);
                if (params.length == 1 && params[0].equals(UUID.class)) {
                    String result = (String) method.invoke(target, uuid);
                    if (isValidWorldName(result)) {
                        return Optional.of(result);
                    }
                }
                if (params.length == 1 && params[0].equals(Player.class)) {
                    String result = (String) method.invoke(target, player);
                    if (isValidWorldName(result)) {
                        return Optional.of(result);
                    }
                }
                if (params.length == 0) {
                    String result = (String) method.invoke(target);
                    if (isValidWorldName(result)) {
                        return Optional.of(result);
                    }
                }
            } catch (ReflectiveOperationException ex) {
                logger.fine("RealmCore resolver skipped method " + method.getName() + ": " + ex.getMessage());
            }
        }
        return Optional.empty();
    }

    private boolean looksRelevant(String name) {
        Predicate<String> contains = namePart -> name.contains(namePart);
        return (contains.test("realm") && contains.test("world"))
                || contains.test("realmworld")
                || contains.test("worldname")
                || contains.test("realmname");
    }

    private boolean isValidWorldName(String name) {
        return name != null && !name.isBlank() && !name.contains(" ");
    }
}
