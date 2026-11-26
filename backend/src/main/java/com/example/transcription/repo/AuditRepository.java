package com.example.transcription.repo;

import com.example.transcription.domain.AuditEvent;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface AuditRepository extends Repository<AuditEvent, Long> {
    AuditEvent save(AuditEvent e);
    List<AuditEvent> findAllByOrderByIdDesc(Pageable page);
    List<AuditEvent> findByEntityTypeAndEntityIdOrderById(String entityType, Long entityId);
}
