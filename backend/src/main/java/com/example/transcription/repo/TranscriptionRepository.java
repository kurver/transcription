package com.example.transcription.repo;

import com.example.transcription.domain.Transcription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface TranscriptionRepository extends Repository<Transcription, Long> {
    Transcription save(Transcription t);
    Optional<Transcription> findById(Long id);
    Optional<Transcription> findByDocumentId(Long documentId);
    List<Transcription> findByRequestIdOrderById(Long requestId);
}
