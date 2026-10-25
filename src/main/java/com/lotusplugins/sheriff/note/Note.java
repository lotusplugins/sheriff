package com.lotusplugins.sheriff.note;

import java.time.Instant;
import java.util.UUID;

public record Note(long id, UUID target, UUID author, String body, Instant createdAt) {}
