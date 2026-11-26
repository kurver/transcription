package com.example.transcription.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** A request-level work item on the shared specialist (transcriber) queue. */
@Entity
@Table(name = "specialist_task")
public class SpecialistTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long requestId;
    public Long createdBy;
    public Instant createdAt = Instant.now();
    public Long assignedTo;
    @Enumerated(EnumType.STRING)
    public TaskStatus status = TaskStatus.OPEN;
    @Enumerated(EnumType.STRING)
    public TaskSource source = TaskSource.USER_REQUEST;
    public String reason;
    public Instant completedAt;
}
