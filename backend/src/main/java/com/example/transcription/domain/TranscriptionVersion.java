package com.example.transcription.domain;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/** Append-only: never updated or deleted. Edits and replacements create new rows. */
@Entity
@Immutable
@Table(name = "transcription_version")
public class TranscriptionVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long transcriptionId;
    public int versionNo;
    public String content;
    @Enumerated(EnumType.STRING)
    public VersionSource source;
    public Long authorId;
    public String comment;
    public Instant createdAt = Instant.now();
}
