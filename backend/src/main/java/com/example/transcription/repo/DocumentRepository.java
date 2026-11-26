package com.example.transcription.repo;

import com.example.transcription.domain.Document;
import com.example.transcription.domain.DocumentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface DocumentRepository extends Repository<Document, Long> {
    Document save(Document d);
    Optional<Document> findById(Long id);
    List<Document> findByRequestIdOrderById(Long requestId);
    List<Document> findByStatusOrderById(DocumentStatus status);
}
