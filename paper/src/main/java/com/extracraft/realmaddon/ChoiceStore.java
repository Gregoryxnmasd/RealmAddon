package com.extracraft.realmaddon;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ChoiceStore {
    CompletableFuture<Void> setChoice(UUID playerId, String generatorKey);

    CompletableFuture<Optional<String>> getChoice(UUID playerId);

    CompletableFuture<Void> clearChoice(UUID playerId);

    void shutdown();
}
