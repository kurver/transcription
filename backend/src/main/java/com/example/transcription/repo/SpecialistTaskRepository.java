package com.example.transcription.repo;

import com.example.transcription.domain.SpecialistTask;
import com.example.transcription.domain.TaskStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface SpecialistTaskRepository extends Repository<SpecialistTask, Long> {
    SpecialistTask save(SpecialistTask t);
    Optional<SpecialistTask> findById(Long id);
    List<SpecialistTask> findByRequestIdOrderById(Long requestId);
    List<SpecialistTask> findByStatusInOrderById(Collection<TaskStatus> statuses);
    boolean existsByRequestIdAndStatusIn(Long requestId, Collection<TaskStatus> statuses);
    Optional<SpecialistTask> findFirstByRequestIdAndStatusAndAssignedTo(Long requestId, TaskStatus status, Long assignedTo);
}
