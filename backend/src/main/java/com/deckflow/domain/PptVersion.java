package com.deckflow.domain;

import java.time.Instant;

public record PptVersion(int version, String fileName, String filePath, Instant createdAt) {
}

