package com.extracraft.realmaddon;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryChoiceStore implements ChoiceStore {
    private final Map<UUID, String> choices = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Void> setChoice(UUID playerId, String generatorKey) {
        choices.put(playerId, generatorKey);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Optional<String>> getChoice(UUID playerId) {
        return CompletableFuture.completedFuture(Optional.ofNullable(choices.get(playerId)));
    }

    @Override
    public CompletableFuture<Void> clearChoice(UUID playerId) {
        choices.remove(playerId);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void shutdown() {
        choices.clear();
    }
}
