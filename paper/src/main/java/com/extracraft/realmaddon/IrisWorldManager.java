package com.extracraft.realmaddon;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class IrisWorldManager {
    private final Plugin plugin;
    private final Logger logger;
    private final List<String> toolbeltClassNames = List.of(
            "com.volmit.iris.core.tools.IrisToolbelt",
            "com.volmit.iris.tools.IrisToolbelt",
            "dev.iris.IrisToolbelt"
    );

    public IrisWorldManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("Iris") != null && findToolbeltClass().isPresent();
    }

    public boolean isIrisWorld(World world) {
        Optional<Class<?>> toolbelt = findToolbeltClass();
        if (toolbelt.isEmpty()) {
            return false;
        }
        try {
            Method method = toolbelt.get().getMethod("isIrisWorld", World.class);
            return (boolean) method.invoke(null, world);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            logger.warning("Failed to check Iris world: " + ex.getMessage());
            return false;
        }
    }

    public void createWorld(String worldName, GeneratorOption option) {
        Optional<Class<?>> toolbelt = findToolbeltClass();
        if (toolbelt.isEmpty()) {
            throw new IllegalStateException("IrisToolbelt not found");
        }
        try {
            Method createWorld = toolbelt.get().getMethod("createWorld");
            Object builder = createWorld.invoke(null);
            builder = invokeBuilder(builder, "name", String.class, worldName);
            builder = invokeBuilder(builder, "dimension", String.class, option.irisDimensionId());
            long seed = option.seedMode() == GeneratorOption.SeedMode.FIXED
                    ? option.fixedSeed()
                    : System.currentTimeMillis();
            builder = invokeBuilder(builder, "seed", long.class, seed);
            if (option.pregenRadiusChunks() > 0) {
                builder = invokeBuilder(builder, "pregen", int.class, option.pregenRadiusChunks());
            }
            Method create = builder.getClass().getMethod("create");
            create.invoke(builder);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to create Iris world", ex);
        }
    }

    private Object invokeBuilder(Object builder, String methodName, Class<?> paramType, Object value)
            throws ReflectiveOperationException {
        Method method = builder.getClass().getMethod(methodName, paramType);
        return method.invoke(builder, value);
    }

    private Optional<Class<?>> findToolbeltClass() {
        Plugin iris = Bukkit.getPluginManager().getPlugin("Iris");
        if (iris == null) {
            return Optional.empty();
        }
        ClassLoader loader = iris.getClass().getClassLoader();
        for (String className : toolbeltClassNames) {
            try {
                return Optional.of(Class.forName(className, true, loader));
            } catch (ClassNotFoundException ignored) {
                // try next
            }
        }
        return Optional.empty();
    }
}
