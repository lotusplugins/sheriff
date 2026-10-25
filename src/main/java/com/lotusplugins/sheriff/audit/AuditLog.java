package com.lotusplugins.sheriff.audit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.h2.Driver;

public final class AuditLog implements AutoCloseable {
    private static final String INSERT =
            "INSERT INTO audit_log (at, actor, action, target, detail) VALUES (?, ?, ?, ?, ?)";

    private final Connection connection;
    private final Executor async;

    public AuditLog(String jdbcUrl, Executor async) throws SQLException {
        Driver.load();
        this.connection = DriverManager.getConnection(jdbcUrl);
        this.async = async;
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS audit_log ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, at TIMESTAMP WITH TIME ZONE NOT NULL, "
                    + "actor UUID NOT NULL, action VARCHAR(32) NOT NULL, target UUID, detail VARCHAR(512) NOT NULL)");
            statement.execute("CREATE INDEX IF NOT EXISTS audit_log_actor_at ON audit_log (actor, at)");
        }
    }

    public CompletableFuture<Void> record(AuditEntry entry) {
        return CompletableFuture.runAsync(() -> insert(entry), async);
    }

    private synchronized void insert(AuditEntry entry) {
        try (var statement = connection.prepareStatement(INSERT)) {
            statement.setTimestamp(1, Timestamp.from(entry.at()));
            statement.setObject(2, entry.actor());
            statement.setString(3, entry.action().name());
            statement.setObject(4, entry.target().orElse(null), Types.OTHER);
            statement.setString(5, entry.detail());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("record audit entry", e);
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        connection.close();
    }
}
