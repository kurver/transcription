package com.example.transcription.repo;

import com.example.transcription.domain.TranscriptionRequest;
import java.util.List;
import org.springframework.data.repository.Repository;

/** No delete methods on purpose: nothing is ever deleted. */
public interface RequestRepository extends Repository<TranscriptionRequest, Long> {
    TranscriptionRequest save(TranscriptionRequest r);
    java.util.Optional<TranscriptionRequest> findById(Long id);
    List<TranscriptionRequest> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<TranscriptionRequest> findByIdIn(java.util.Collection<Long> ids);
    List<TranscriptionRequest> findAllByOrderByIdDesc();
}
