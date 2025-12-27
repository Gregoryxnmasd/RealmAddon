package com.extracraft.realmaddon;

import org.bukkit.Material;

import java.util.List;

public record GeneratorOption(
        String key,
        String displayName,
        List<String> lore,
        String irisDimensionId,
        SeedMode seedMode,
        long fixedSeed,
        int pregenRadiusChunks,
        Material icon
) {
    public enum SeedMode {
        RANDOM,
        FIXED
    }
}
