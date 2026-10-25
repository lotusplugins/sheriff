package com.lotusplugins.sheriff.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.DriverManager;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditLogTest {
    private static final String URL = "jdbc:h2:mem:audit_test";

    @Test
    void recordsEntry() throws Exception {
        var actor = UUID.randomUUID();
        var target = UUID.randomUUID();
        try (var log = new AuditLog(URL, Runnable::run);
                var reader = DriverManager.getConnection(URL)) {
            log.record(new AuditEntry(Instant.now(), actor, AuditAction.FREEZE, Optional.of(target), "reason"))
                    .join();
            log.record(new AuditEntry(Instant.now(), actor, AuditAction.VANISH, Optional.empty(), "")).join();
            try (var rows = reader.createStatement()
                    .executeQuery("SELECT actor, action, target, detail FROM audit_log ORDER BY id")) {
                rows.next();
                assertEquals(actor, rows.getObject("actor", UUID.class));
                assertEquals("FREEZE", rows.getString("action"));
                assertEquals(target, rows.getObject("target", UUID.class));
                assertEquals("reason", rows.getString("detail"));
                rows.next();
                assertEquals(null, rows.getObject("target"));
                assertFalse(rows.next());
            }
        }
    }
}
