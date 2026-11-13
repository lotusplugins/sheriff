package com.lotusplugins.sheriff.freeze;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.h2.Driver;

public final class FreezeStore implements AutoCloseable {
    private static final String EXISTS = "SELECT 1 FROM frozen WHERE target = ?";
    private static final String INSERT = "INSERT INTO frozen (target, frozen_by, frozen_at) VALUES (?, ?, ?)";
    private static final String DELETE = "DELETE FROM frozen WHERE target = ?";

    private final Connection connection;
    private final Executor async;

    public FreezeStore(String jdbcUrl, Executor async) throws SQLException {
        Driver.load();
        this.connection = DriverManager.getConnection(jdbcUrl);
        this.async = async;
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS frozen ("
                    + "target UUID PRIMARY KEY, frozen_by UUID NOT NULL, frozen_at TIMESTAMP WITH TIME ZONE NOT NULL)");
        }
    }

    public CompletableFuture<Boolean> insert(UUID target, UUID frozenBy, Instant frozenAt) {
        return CompletableFuture.supplyAsync(() -> insertNow(target, frozenBy, frozenAt), async);
    }

    public CompletableFuture<Boolean> delete(UUID target) {
        return CompletableFuture.supplyAsync(() -> deleteNow(target), async);
    }

    public CompletableFuture<Boolean> contains(UUID target) {
        return CompletableFuture.supplyAsync(() -> containsNow(target), async);
    }

    private synchronized boolean insertNow(UUID target, UUID frozenBy, Instant frozenAt) {
        if (containsNow(target)) {
            return false;
        }
        try (var statement = connection.prepareStatement(INSERT)) {
            statement.setObject(1, target);
            statement.setObject(2, frozenBy);
            statement.setTimestamp(3, Timestamp.from(frozenAt));
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("insert frozen", e);
        }
    }

    private synchronized boolean deleteNow(UUID target) {
        try (var statement = connection.prepareStatement(DELETE)) {
            statement.setObject(1, target);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("delete frozen", e);
        }
    }

    private synchronized boolean containsNow(UUID target) {
        try (var statement = connection.prepareStatement(EXISTS)) {
            statement.setObject(1, target);
            try (var rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("find frozen", e);
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        connection.close();
    }
}
