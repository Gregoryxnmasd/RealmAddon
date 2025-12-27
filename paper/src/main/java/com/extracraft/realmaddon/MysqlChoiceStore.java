package com.extracraft.realmaddon;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MysqlChoiceStore implements ChoiceStore {
    private final HikariDataSource dataSource;
    private final Executor executor;

    public MysqlChoiceStore(PluginConfig.MysqlSettings settings, Executor executor) {
        this.executor = executor;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + settings.host() + ":" + settings.port() + "/" + settings.database() + "?useSSL=" + settings.useSsl());
        config.setUsername(settings.user());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS realmaddon_pending_choice (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "generator_key VARCHAR(64) NOT NULL," +
                "created_at BIGINT NOT NULL" +
                ")";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create realmaddon_pending_choice table", ex);
        }
    }

    @Override
    public CompletableFuture<Void> setChoice(UUID playerId, String generatorKey) {
        return CompletableFuture.runAsync(() -> {
            String sql = "REPLACE INTO realmaddon_pending_choice (uuid, generator_key, created_at) VALUES (?, ?, ?)";
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, generatorKey);
                stmt.setLong(3, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("Failed to store generator choice", ex);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<String>> getChoice(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT generator_key FROM realmaddon_pending_choice WHERE uuid = ?";
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.ofNullable(rs.getString("generator_key"));
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("Failed to read generator choice", ex);
            }
            return Optional.empty();
        }, executor);
    }

    @Override
    public CompletableFuture<Void> clearChoice(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM realmaddon_pending_choice WHERE uuid = ?";
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("Failed to clear generator choice", ex);
            }
        }, executor);
    }

    @Override
    public void shutdown() {
        dataSource.close();
    }
}
