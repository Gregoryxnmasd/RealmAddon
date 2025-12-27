package com.extracraft.realmaddon.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class PluginMessage {
    private static final Gson GSON = new Gson();

    private PluginMessage() {
    }

    public static byte[] encodeChoice(UUID playerId, String generatorKey, String targetServer) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "choice");
        payload.addProperty("playerId", playerId.toString());
        payload.addProperty("generatorKey", generatorKey);
        if (targetServer != null && !targetServer.isBlank()) {
            payload.addProperty("targetServer", targetServer);
        }
        return GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
    }

    public static Message decode(byte[] data) {
        String json = new String(data, StandardCharsets.UTF_8);
        JsonObject payload = GSON.fromJson(json, JsonObject.class);
        String type = payload.has("type") ? payload.get("type").getAsString() : "";
        if (!"choice".equalsIgnoreCase(type)) {
            return null;
        }
        UUID playerId = UUID.fromString(payload.get("playerId").getAsString());
        String generatorKey = payload.get("generatorKey").getAsString();
        String targetServer = payload.has("targetServer") ? payload.get("targetServer").getAsString() : "";
        return new Message(playerId, generatorKey, targetServer);
    }

    public record Message(UUID playerId, String generatorKey, String targetServer) {
    }
}
