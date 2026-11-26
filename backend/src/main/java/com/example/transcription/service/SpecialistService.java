package com.example.transcription.service;

import com.example.transcription.domain.*;
import com.example.transcription.repo.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The shared specialist queue: one task per request, claimed by any transcriber. */
@Service
public class SpecialistService {
    static final Set<TaskStatus> ACTIVE = EnumSet.of(TaskStatus.OPEN, TaskStatus.CLAIMED);

    private final SpecialistTaskRepository tasks;
    private final AccessService access;
    private final TranscriptionRepository transcriptions;
    private final QaReviewRepository reviews;
    private final AuditService audit;

    public SpecialistService(SpecialistTaskRepository tasks, AccessService access, TranscriptionRepository transcriptions,
                             QaReviewRepository reviews, AuditService audit) {
        this.tasks = tasks;
        this.access = access;
        this.transcriptions = transcriptions;
        this.reviews = reviews;
        this.audit = audit;
    }

    /** Owner hands a completed request to the specialist team. */
    @Transactional
    public SpecialistTask sendToSpecialists(CurrentUser u, Long requestId, String reason) {
        u.require(Role.USER);
        TranscriptionRequest r = access.request(u, requestId);
        if (r.status != RequestStatus.COMPLETED) throw new Errors.BadRequest("Only completed requests can be sent to the specialist team");
        if (tasks.existsByRequestIdAndStatusIn(r.id, ACTIVE)) throw new Errors.BadRequest("Already with the specialist team");
        return create(r.id, u.id(), TaskSource.USER_REQUEST, blankToNull(reason));
    }

    /** Called when QA fails a document: puts the request back on the specialist queue unless it is already there. */
    @Transactional
    public void reopenForQaFailure(Long requestId, Long qaUserId, String reason) {
        if (!tasks.existsByRequestIdAndStatusIn(requestId, ACTIVE)) create(requestId, qaUserId, TaskSource.QA_FAILURE, reason);
    }

    private SpecialistTask create(Long requestId, Long actorId, TaskSource source, String reason) {
        SpecialistTask t = new SpecialistTask();
        t.requestId = requestId;
        t.createdBy = actorId;
        t.source = source;
        t.reason = reason;
        t = tasks.save(t);
        audit.record(actorId, "REQUEST", requestId, "SENT_TO_SPECIALISTS", source + (reason == null ? "" : ": " + reason));
        return t;
    }

    @Transactional
    public SpecialistTask claim(CurrentUser u, Long taskId) {
        u.require(Role.TRANSCRIBER);
        SpecialistTask t = task(taskId);
        if (t.status != TaskStatus.OPEN) throw new Errors.BadRequest("Task is not open");
        t.status = TaskStatus.CLAIMED;
        t.assignedTo = u.id();
        tasks.save(t);
        audit.record(u.id(), "REQUEST", t.requestId, "SPECIALIST_CLAIMED", "task " + t.id);
        return t;
    }

    /** Finishing a task re-queues any document QA had failed for a recheck. */
    @Transactional
    public SpecialistTask complete(CurrentUser u, Long taskId) {
        u.require(Role.TRANSCRIBER);
        SpecialistTask t = task(taskId);
        if (t.status != TaskStatus.CLAIMED || !u.id().equals(t.assignedTo))
            throw new Errors.Forbidden("Task is not claimed by you");
        t.status = TaskStatus.DONE;
        t.completedAt = Instant.now();
        tasks.save(t);
        var latest = RequestViewService.latestPerTranscription(reviews.findByRequestIdOrderById(t.requestId));
        for (Transcription tr : transcriptions.findByRequestIdOrderById(t.requestId)) {
            QaReview last = latest.get(tr.id);
            if (last != null && last.status == QaStatus.FAILED) {
                QaReview re = new QaReview();
                re.transcriptionId = tr.id;
                re.requestId = t.requestId;
                re.source = QaSource.RECHECK;
                re.requestedBy = u.id();
                re = reviews.save(re);
                audit.record(u.id(), "QA_REVIEW", re.id, "RECHECK_QUEUED", "transcription " + tr.id);
            }
        }
        audit.record(u.id(), "REQUEST", t.requestId, "SPECIALIST_COMPLETED", "task " + t.id);
        return t;
    }

    private SpecialistTask task(Long id) {
        return tasks.findById(id).orElseThrow(() -> new Errors.NotFound("Task not found"));
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
