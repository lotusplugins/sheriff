package com.lotusplugins.sheriff.note;

import com.lotusplugins.sheriff.audit.AuditAction;
import com.lotusplugins.sheriff.audit.AuditEntry;
import com.lotusplugins.sheriff.audit.AuditLog;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NoteService {
    public static final int PAGE_SIZE = 10;

    private final NoteStore store;
    private final AuditLog auditLog;
    private final Logger logger;

    public NoteService(NoteStore store, AuditLog auditLog, Logger logger) {
        this.store = store;
        this.auditLog = auditLog;
        this.logger = logger;
    }

    public CompletableFuture<Note> add(UUID actor, UUID target, String body) {
        var now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return store.insert(target, actor, body, now)
                .thenCompose(note -> audit(now, actor, AuditAction.NOTE_ADD, note).thenApply(done -> note));
    }

    public CompletableFuture<List<Note>> list(UUID target, int page) {
        return store.find(target, PAGE_SIZE, (page - 1) * PAGE_SIZE);
    }

    public CompletableFuture<Optional<Note>> delete(UUID actor, long id) {
        var now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return store.softDelete(id, now).thenCompose(deleted -> deleted
                .map(note -> audit(now, actor, AuditAction.NOTE_DELETE, note).thenApply(done -> deleted))
                .orElseGet(() -> CompletableFuture.completedFuture(deleted)));
    }

    private CompletableFuture<Void> audit(Instant at, UUID actor, AuditAction action, Note note) {
        var entry = new AuditEntry(at, actor, action, Optional.of(note.target()), "id=" + note.id());
        return auditLog.record(entry).exceptionally(error -> {
            logger.log(Level.SEVERE, "audit entry not recorded, recreate manually: " + entry, error);
            return null;
        });
    }
}
