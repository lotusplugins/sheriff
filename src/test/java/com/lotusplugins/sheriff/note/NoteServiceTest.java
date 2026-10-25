package com.lotusplugins.sheriff.note;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lotusplugins.sheriff.audit.AuditLog;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NoteServiceTest {
    private final UUID actor = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final List<LogRecord> logged = new ArrayList<>();

    private String url;
    private AuditLog auditLog;
    private NoteStore store;
    private NoteService service;

    @BeforeEach
    void open() throws Exception {
        url = "jdbc:h2:mem:" + UUID.randomUUID();
        auditLog = new AuditLog(url, Runnable::run);
        store = new NoteStore(url, Runnable::run);
        var logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });
        service = new NoteService(store, auditLog, logger);
    }

    @AfterEach
    void close() throws Exception {
        store.close();
        auditLog.close();
    }

    @Test
    void addThenListReturnsNote() {
        var added = service.add(actor, target, "x-ray pack").join();

        var notes = service.list(target, 1).join();

        assertEquals(List.of(added), notes);
        assertEquals(target, added.target());
        assertEquals(actor, added.author());
        assertEquals("x-ray pack", added.body());
    }

    @Test
    void listIsNewestFirstAndPaged() {
        for (var i = 1; i <= NoteService.PAGE_SIZE + 2; i++) {
            service.add(actor, target, "note " + i).join();
        }

        var first = service.list(target, 1).join();
        var second = service.list(target, 2).join();

        assertEquals(NoteService.PAGE_SIZE, first.size());
        assertEquals("note 12", first.getFirst().body());
        assertEquals(List.of("note 2", "note 1"), second.stream().map(Note::body).toList());
        assertTrue(service.list(target, 3).join().isEmpty());
    }

    @Test
    void listOnlyReturnsRequestedTarget() {
        service.add(actor, target, "mine").join();
        service.add(actor, UUID.randomUUID(), "other").join();

        assertEquals(List.of("mine"), service.list(target, 1).join().stream().map(Note::body).toList());
    }

    @Test
    void addWritesOneAuditEntry() throws Exception {
        var note = service.add(actor, target, "body").join();

        try (var reader = DriverManager.getConnection(url);
                var rows = reader.createStatement().executeQuery("SELECT actor, action, target, detail FROM audit_log")) {
            assertTrue(rows.next());
            assertEquals(actor, rows.getObject("actor", UUID.class));
            assertEquals("NOTE_ADD", rows.getString("action"));
            assertEquals(target, rows.getObject("target", UUID.class));
            assertEquals("id=" + note.id(), rows.getString("detail"));
            assertFalse(rows.next());
        }
    }

    @Test
    void failedAuditKeepsNoteAndLogsEntry() throws Exception {
        auditLog.close();

        var note = service.add(actor, target, "body").join();

        assertEquals(List.of(note), service.list(target, 1).join());
        assertEquals(1, logged.size());
        assertEquals(Level.SEVERE, logged.getFirst().getLevel());
        var message = logged.getFirst().getMessage();
        assertTrue(message.contains(actor.toString()));
        assertTrue(message.contains(target.toString()));
        assertTrue(message.contains("NOTE_ADD"));
        assertTrue(message.contains("id=" + note.id()));
    }
}
