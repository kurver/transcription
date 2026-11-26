package com.example.transcription.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** A document-level QA review. Reviews appear in the shared QA queue grouped by request. */
@Entity
@Table(name = "qa_review")
public class QaReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long transcriptionId;
    public Long requestId;
    @Enumerated(EnumType.STRING)
    public QaSource source;
    public Long requestedBy;          // null for random sampling
    public Instant createdAt = Instant.now();
    public Long assignedTo;
    @Enumerated(EnumType.STRING)
    public QaStatus status = QaStatus.OPEN;
    public Integer reviewedVersionNo;
    public String comment;
    public Instant completedAt;
}
