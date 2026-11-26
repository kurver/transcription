package com.example.transcription.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** One per document. Content lives in append-only {@link TranscriptionVersion} rows. */
@Entity
@Table(name = "transcription")
public class Transcription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long documentId;
    public Long requestId;
    public Long ownerId;
    public Long currentVersionId;
    /** Set once the random QA sampling decision has been made for this transcription. */
    public boolean qaSampled;
    public Instant createdAt = Instant.now();
}
