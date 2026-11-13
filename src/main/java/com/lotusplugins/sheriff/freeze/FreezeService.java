package com.lotusplugins.sheriff.freeze;

import com.lotusplugins.sheriff.audit.AuditAction;
import com.lotusplugins.sheriff.audit.AuditEntry;
import com.lotusplugins.sheriff.audit.AuditLog;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FreezeService {
    public enum Result {
        DONE,
        UNCHANGED,
        DENIED
    }

    private final FreezeStore store;
    private final AuditLog auditLog;
    private final BiFunction<UUID, UUID, CompletableFuture<Boolean>> canAct;
    private final Logger logger;
    private final Set<UUID> frozen = ConcurrentHashMap.newKeySet();

    public FreezeService(
            FreezeStore store,
            AuditLog auditLog,
            BiFunction<UUID, UUID, CompletableFuture<Boolean>> canAct,
            Logger logger) {
        this.store = store;
        this.auditLog = auditLog;
        this.canAct = canAct;
        this.logger = logger;
    }

    public boolean isFrozen(UUID player) {
        return frozen.contains(player);
    }

    public CompletableFuture<Void> load(UUID player) {
        return store.contains(player).thenAccept(stored -> {
            if (stored) {
                frozen.add(player);
            }
        });
    }

    public void forget(UUID player) {
        frozen.remove(player);
    }

    public CompletableFuture<Result> freeze(UUID actor, UUID target) {
        return change(actor, target, now -> store.insert(target, actor, now), frozen::add, AuditAction.FREEZE);
    }

    public CompletableFuture<Result> unfreeze(UUID actor, UUID target) {
        return change(actor, target, now -> store.delete(target), frozen::remove, AuditAction.UNFREEZE);
    }

    private CompletableFuture<Result> change(
            UUID actor,
            UUID target,
            Function<Instant, CompletableFuture<Boolean>> write,
            Consumer<UUID> remember,
            AuditAction action) {
        return canAct.apply(actor, target)
                .exceptionally(error -> {
                    logger.log(Level.SEVERE, "staff level lookup failed for " + actor + " on " + target, error);
                    return false;
                })
                .thenCompose(allowed -> {
                    if (!allowed) {
                        return CompletableFuture.completedFuture(Result.DENIED);
                    }
                    var now = Instant.now().truncatedTo(ChronoUnit.MICROS);
                    return write.apply(now).thenCompose(changed -> {
                        if (!changed) {
                            return CompletableFuture.completedFuture(Result.UNCHANGED);
                        }
                        remember.accept(target);
                        return audit(now, actor, action, target).thenApply(done -> Result.DONE);
                    });
                });
    }

    private CompletableFuture<Void> audit(Instant at, UUID actor, AuditAction action, UUID target) {
        var entry = new AuditEntry(at, actor, action, Optional.of(target), "");
        return auditLog.record(entry).exceptionally(error -> {
            logger.log(Level.SEVERE, "audit entry not recorded, recreate manually: " + entry, error);
            return null;
        });
    }
}
