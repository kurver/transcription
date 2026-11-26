package com.example.transcription.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "document")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long requestId;
    public String originalFilename;
    public String contentType;
    public String storageKey;
    public long sizeBytes;
    @Enumerated(EnumType.STRING)
    public DocumentStatus status = DocumentStatus.PENDING;
    public String failureReason;
    public Instant uploadedAt = Instant.now();
}
