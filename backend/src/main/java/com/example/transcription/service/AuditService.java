package com.example.transcription.service;

import com.example.transcription.domain.AuditEvent;
import com.example.transcription.repo.AuditRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditRepository repo;

    public AuditService(AuditRepository repo) { this.repo = repo; }

    public void record(Long actorId, String entityType, Long entityId, String action, String detail) {
        AuditEvent e = new AuditEvent();
        e.actorId = actorId;
        e.entityType = entityType;
        e.entityId = entityId;
        e.action = action;
        e.detail = detail;
        repo.save(e);
    }
}
