package com.example.transcription.repo;

import com.example.transcription.domain.QaReview;
import com.example.transcription.domain.QaStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface QaReviewRepository extends Repository<QaReview, Long> {
    QaReview save(QaReview r);
    Optional<QaReview> findById(Long id);
    List<QaReview> findByRequestIdOrderById(Long requestId);
    List<QaReview> findByStatusInOrderById(Collection<QaStatus> statuses);
    boolean existsByTranscriptionIdAndStatusIn(Long transcriptionId, Collection<QaStatus> statuses);
}
