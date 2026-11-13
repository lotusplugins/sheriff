package com.lotusplugins.sheriff.freeze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lotusplugins.sheriff.audit.AuditLog;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FreezeServiceTest {
    private final UUID actor = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final Logger logger = Logger.getAnonymousLogger();

    private String url;
    private AuditLog auditLog;
    private FreezeStore store;
    private FreezeService service;

    @BeforeEach
    void open() throws Exception {
        url = "jdbc:h2:mem:" + UUID.randomUUID();
        auditLog = new AuditLog(url, Runnable::run);
        store = new FreezeStore(url, Runnable::run);
        logger.setUseParentHandlers(false);
        service = serviceAllowing((who, whom) -> CompletableFuture.completedFuture(true));
    }

    @AfterEach
    void close() throws Exception {
        store.close();
        auditLog.close();
    }

    private FreezeService serviceAllowing(BiFunction<UUID, UUID, CompletableFuture<Boolean>> canAct) {
        return new FreezeService(store, auditLog, canAct, logger);
    }

    @Test
    void freezePersistsRemembersAndAudits() throws Exception {
        assertEquals(FreezeService.Result.DONE, service.freeze(actor, target).join());

        assertTrue(service.isFrozen(target));
        assertTrue(store.contains(target).join());
        assertEquals(1, auditCount("FREEZE"));
        try (var reader = DriverManager.getConnection(url);
                var rows = reader.createStatement().executeQuery("SELECT actor, target FROM audit_log")) {
            assertTrue(rows.next());
            assertEquals(actor, rows.getObject("actor", UUID.class));
            assertEquals(target, rows.getObject("target", UUID.class));
        }
    }

    @Test
    void freezingTwiceIsUnchangedAndNotAudited() throws Exception {
        service.freeze(actor, target).join();

        assertEquals(FreezeService.Result.UNCHANGED, service.freeze(actor, target).join());
        assertEquals(1, auditCount("FREEZE"));
    }

    @Test
    void unfreezeRemovesAndAudits() throws Exception {
        service.freeze(actor, target).join();

        assertEquals(FreezeService.Result.DONE, service.unfreeze(actor, target).join());

        assertFalse(service.isFrozen(target));
        assertFalse(store.contains(target).join());
        assertEquals(1, auditCount("UNFREEZE"));
    }

    @Test
    void unfreezingUnfrozenIsUnchangedAndNotAudited() throws Exception {
        assertEquals(FreezeService.Result.UNCHANGED, service.unfreeze(actor, target).join());
        assertEquals(0, auditCount("UNFREEZE"));
    }

    @Test
    void freezeSurvivesRestart() {
        service.freeze(actor, target).join();
        var restarted = serviceAllowing((who, whom) -> CompletableFuture.completedFuture(true));
        assertFalse(restarted.isFrozen(target));

        restarted.load(target).join();

        assertTrue(restarted.isFrozen(target));
    }

    @Test
    void deniedOrFailedLevelCheckChangesNothing() throws Exception {
        var denied = serviceAllowing((who, whom) -> CompletableFuture.completedFuture(false));
        var failed = serviceAllowing((who, whom) -> CompletableFuture.failedFuture(new IllegalStateException()));

        assertEquals(FreezeService.Result.DENIED, denied.freeze(actor, target).join());
        assertEquals(FreezeService.Result.DENIED, failed.freeze(actor, target).join());

        assertFalse(store.contains(target).join());
        assertEquals(0, auditCount("FREEZE"));
    }

    @Test
    void failedAuditKeepsFreeze() throws Exception {
        auditLog.close();

        assertEquals(FreezeService.Result.DONE, service.freeze(actor, target).join());

        assertTrue(service.isFrozen(target));
        assertTrue(store.contains(target).join());
    }

    @Test
    void failedStoreWriteChangesNothing() throws Exception {
        store.close();

        assertThrows(CompletionException.class, () -> service.freeze(actor, target).join());

        assertFalse(service.isFrozen(target));
        assertEquals(0, auditCount("FREEZE"));
    }

    private int auditCount(String action) throws Exception {
        try (var reader = DriverManager.getConnection(url);
                var rows = reader.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM audit_log WHERE action = '" + action + "'")) {
            rows.next();
            return rows.getInt(1);
        }
    }
}
