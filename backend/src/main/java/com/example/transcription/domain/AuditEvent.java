package com.example.transcription.domain;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "audit_event")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long actorId;
    public String entityType;
    public Long entityId;
    public String action;
    public String detail;
    public Instant createdAt = Instant.now();
}
