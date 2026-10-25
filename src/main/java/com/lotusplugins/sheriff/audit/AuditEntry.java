package com.lotusplugins.sheriff.audit;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record AuditEntry(Instant at, UUID actor, AuditAction action, Optional<UUID> target, String detail) {}
