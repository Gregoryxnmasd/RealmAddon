package com.extracraft.realmaddon;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PluginConfig {
    public enum Mode {
        SINGLE,
        NETWORK_MYSQL,
        NETWORK_MESSAGING
    }

    private final Mode mode;
    private final MysqlSettings mysqlSettings;
    private final List<GeneratorOption> generators;
    private final String createCommand;
    private final String homeCommand;
    private final String resetCommand;
    private final String messagingTargetServer;
    private final int timeoutSeconds;
    private final boolean debug;

    public PluginConfig(Mode mode,
                        MysqlSettings mysqlSettings,
                        List<GeneratorOption> generators,
                        String createCommand,
                        String homeCommand,
                        String resetCommand,
                        String messagingTargetServer,
                        int timeoutSeconds,
                        boolean debug) {
        this.mode = mode;
        this.mysqlSettings = mysqlSettings;
        this.generators = generators;
        this.createCommand = createCommand;
        this.homeCommand = homeCommand;
        this.resetCommand = resetCommand;
        this.messagingTargetServer = messagingTargetServer;
        this.timeoutSeconds = timeoutSeconds;
        this.debug = debug;
    }

    public static PluginConfig from(FileConfiguration config) {
        Mode mode = Mode.valueOf(config.getString("mode", "SINGLE").toUpperCase(Locale.ROOT));
        MysqlSettings mysqlSettings = MysqlSettings.from(config.getConfigurationSection("mysql"));
        List<GeneratorOption> generators = parseGenerators(config.getConfigurationSection("generators"));
        String createCommand = config.getString("integration.realmcoreCreateCommand", "realm create");
        String homeCommand = config.getString("integration.realmcoreHomeCommand", "realm home");
        String resetCommand = config.getString("integration.realmcoreResetCommand", "realm reset");
        String messagingTargetServer = config.getString("integration.messagingTargetServer", "");
        int timeoutSeconds = config.getInt("integration.timeoutSeconds", 15);
        boolean debug = config.getBoolean("debug", false);
        return new PluginConfig(mode, mysqlSettings, generators, createCommand, homeCommand, resetCommand, messagingTargetServer, timeoutSeconds, debug);
    }

    private static List<GeneratorOption> parseGenerators(ConfigurationSection section) {
        if (section == null) {
            return Collections.emptyList();
        }
        List<GeneratorOption> options = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection generator = section.getConfigurationSection(key);
            if (generator == null) {
                continue;
            }
            String displayName = generator.getString("displayName", key);
            List<String> lore = generator.getStringList("lore");
            String irisDimensionId = generator.getString("irisDimensionId", key);
            String seedModeRaw = generator.getString("seedMode", "RANDOM").toUpperCase(Locale.ROOT);
            GeneratorOption.SeedMode seedMode = GeneratorOption.SeedMode.valueOf(seedModeRaw);
            long fixedSeed = generator.getLong("fixedSeed", 0L);
            int pregenRadius = generator.getInt("pregenRadiusChunks", 0);
            Material icon = Material.matchMaterial(generator.getString("icon", "GRASS_BLOCK"));
            if (icon == null) {
                icon = Material.GRASS_BLOCK;
            }
            options.add(new GeneratorOption(key, displayName, lore, irisDimensionId, seedMode, fixedSeed, pregenRadius, icon));
        }
        return options;
    }

    public Mode mode() {
        return mode;
    }

    public MysqlSettings mysqlSettings() {
        return mysqlSettings;
    }

    public List<GeneratorOption> generators() {
        return generators;
    }

    public String createCommand() {
        return createCommand;
    }

    public String homeCommand() {
        return homeCommand;
    }

    public String resetCommand() {
        return resetCommand;
    }

    public String messagingTargetServer() {
        return messagingTargetServer;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean debug() {
        return debug;
    }

    public record MysqlSettings(String host, int port, String database, String user, String password, boolean useSsl) {
        static MysqlSettings from(ConfigurationSection section) {
            if (section == null) {
                return new MysqlSettings("localhost", 3306, "realmaddon", "root", "", false);
            }
            return new MysqlSettings(
                    section.getString("host", "localhost"),
                    section.getInt("port", 3306),
                    section.getString("database", "realmaddon"),
                    section.getString("user", "root"),
                    section.getString("password", ""),
                    section.getBoolean("useSSL", false)
            );
        }
    }
}
