package com.lotusplugins.sheriff.note;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.h2.Driver;

public final class NoteStore implements AutoCloseable {
    private static final String INSERT =
            "INSERT INTO notes (target, author, body, created_at) VALUES (?, ?, ?, ?)";
    private static final String FIND =
            "SELECT id, target, author, body, created_at FROM notes "
                    + "WHERE target = ? AND deleted_at IS NULL ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";

    private static final String FIND_LIVE =
            "SELECT id, target, author, body, created_at FROM notes WHERE id = ? AND deleted_at IS NULL";
    private static final String MARK_DELETED = "UPDATE notes SET deleted_at = ? WHERE id = ?";

    private final Connection connection;
    private final Executor async;

    public NoteStore(String jdbcUrl, Executor async) throws SQLException {
        Driver.load();
        this.connection = DriverManager.getConnection(jdbcUrl);
        this.async = async;
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS notes ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, target UUID NOT NULL, author UUID NOT NULL, "
                    + "body VARCHAR(256) NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, "
                    + "deleted_at TIMESTAMP WITH TIME ZONE)");
            statement.execute("CREATE INDEX IF NOT EXISTS notes_target_created ON notes (target, created_at)");
        }
    }

    public CompletableFuture<Note> insert(UUID target, UUID author, String body, Instant createdAt) {
        return CompletableFuture.supplyAsync(() -> insertNow(target, author, body, createdAt), async);
    }

    public CompletableFuture<List<Note>> find(UUID target, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> findNow(target, limit, offset), async);
    }

    public CompletableFuture<Optional<Note>> softDelete(long id, Instant deletedAt) {
        return CompletableFuture.supplyAsync(() -> softDeleteNow(id, deletedAt), async);
    }

    private synchronized Optional<Note> softDeleteNow(long id, Instant deletedAt) {
        try (var find = connection.prepareStatement(FIND_LIVE);
                var mark = connection.prepareStatement(MARK_DELETED)) {
            find.setLong(1, id);
            try (var rows = find.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }
                var note = read(rows);
                mark.setTimestamp(1, Timestamp.from(deletedAt));
                mark.setLong(2, id);
                mark.executeUpdate();
                return Optional.of(note);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("delete note", e);
        }
    }

    private synchronized Note insertNow(UUID target, UUID author, String body, Instant createdAt) {
        try (var statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setObject(1, target);
            statement.setObject(2, author);
            statement.setString(3, body);
            statement.setTimestamp(4, Timestamp.from(createdAt));
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                keys.next();
                return new Note(keys.getLong(1), target, author, body, createdAt);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("insert note", e);
        }
    }

    private synchronized List<Note> findNow(UUID target, int limit, int offset) {
        try (var statement = connection.prepareStatement(FIND)) {
            statement.setObject(1, target);
            statement.setInt(2, limit);
            statement.setInt(3, offset);
            try (var rows = statement.executeQuery()) {
                var notes = new ArrayList<Note>();
                while (rows.next()) {
                    notes.add(read(rows));
                }
                return List.copyOf(notes);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("find notes", e);
        }
    }

    private static Note read(ResultSet rows) throws SQLException {
        return new Note(
                rows.getLong("id"),
                rows.getObject("target", UUID.class),
                rows.getObject("author", UUID.class),
                rows.getString("body"),
                rows.getTimestamp("created_at").toInstant());
    }

    @Override
    public synchronized void close() throws SQLException {
        connection.close();
    }
}
