package com.example.transcription.repo;

import com.example.transcription.domain.TranscriptionVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Insert/read only. Versions are immutable history. */
public interface VersionRepository extends Repository<TranscriptionVersion, Long> {
    TranscriptionVersion save(TranscriptionVersion v);
    Optional<TranscriptionVersion> findById(Long id);
    List<TranscriptionVersion> findByTranscriptionIdOrderByVersionNo(Long transcriptionId);
    long countByTranscriptionId(Long transcriptionId);
}
