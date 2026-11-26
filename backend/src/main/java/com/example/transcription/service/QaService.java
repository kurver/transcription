package com.example.transcription.service;

import com.example.transcription.domain.*;
import com.example.transcription.repo.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The shared QA queue. Reviews are document-level but are claimed per request.
 * Sources: random sampling when a request completes, or an explicit request from the owner / working transcriber.
 * QA only records verdicts — it never changes transcription content.
 */
@Service
public class QaService {
    public enum Verdict { PASS, FAIL }

    private static final Set<QaStatus> ACTIVE = EnumSet.of(QaStatus.OPEN, QaStatus.CLAIMED);

    private final QaReviewRepository reviews;
    private final TranscriptionRepository transcriptions;
    private final VersionRepository versions;
    private final DocumentRepository documents;
    private final SpecialistTaskRepository tasks;
    private final SpecialistService specialist;
    private final AccessService access;
    private final AuditService audit;
    private final double sampleRate;

    public QaService(QaReviewRepository reviews, TranscriptionRepository transcriptions, VersionRepository versions,
                     DocumentRepository documents, SpecialistTaskRepository tasks, SpecialistService specialist,
                     AccessService access, AuditService audit, @Value("${app.qa.sample-rate:0.25}") double sampleRate) {
        this.reviews = reviews;
        this.transcriptions = transcriptions;
        this.versions = versions;
        this.documents = documents;
        this.tasks = tasks;
        this.specialist = specialist;
        this.access = access;
        this.audit = audit;
        this.sampleRate = sampleRate;
    }

    /** Called once a request completes: each not-yet-sampled transcription is sampled with probability {@code app.qa.sample-rate}. */
    @Transactional
    public void sampleRequest(Long requestId) {
        for (Transcription t : transcriptions.findByRequestIdOrderById(requestId)) {
            if (t.qaSampled) continue;
            t.qaSampled = true;
            transcriptions.save(t);
            if (ThreadLocalRandom.current().nextDouble() < sampleRate) {
                QaReview r = new QaReview();
                r.transcriptionId = t.id;
                r.requestId = requestId;
                r.source = QaSource.SAMPLED;
                r = reviews.save(r);
                audit.record(null, "QA_REVIEW", r.id, "SAMPLED", "transcription " + t.id);
            }
        }
    }

    /** Owner, or the transcriber working the request, asks for QA on one document. */
    @Transactional
    public QaReview requestReview(CurrentUser u, Long transcriptionId) {
        if (u.role() == Role.QA) throw new Errors.Forbidden("QA reviewers cannot request QA");
        Transcription t = access.transcription(u, transcriptionId);
        if (u.role() == Role.TRANSCRIBER
                && tasks.findFirstByRequestIdAndStatusAndAssignedTo(t.requestId, TaskStatus.CLAIMED, u.id()).isEmpty())
            throw new Errors.Forbidden("Claim the request before asking for QA");
        if (reviews.existsByTranscriptionIdAndStatusIn(t.id, ACTIVE)) throw new Errors.BadRequest("QA is already pending for this document");
        QaReview r = new QaReview();
        r.transcriptionId = t.id;
        r.requestId = t.requestId;
        r.source = u.role() == Role.USER ? QaSource.USER_REQUEST : QaSource.TRANSCRIBER_REQUEST;
        r.requestedBy = u.id();
        r = reviews.save(r);
        audit.record(u.id(), "QA_REVIEW", r.id, "REQUESTED", "transcription " + t.id);
        return r;
    }

    /** Takes every open review on a request from the shared queue. */
    @Transactional
    public List<QaReview> claimRequest(CurrentUser u, Long requestId) {
        u.require(Role.QA);
        List<QaReview> open = reviews.findByRequestIdOrderById(requestId).stream().filter(r -> r.status == QaStatus.OPEN).toList();
        if (open.isEmpty()) throw new Errors.BadRequest("Nothing to claim on this request");
        for (QaReview r : open) {
            r.status = QaStatus.CLAIMED;
            r.assignedTo = u.id();
            reviews.save(r);
            audit.record(u.id(), "QA_REVIEW", r.id, "CLAIMED", "request " + requestId);
        }
        return open;
    }

    @Transactional
    public QaReview verdict(CurrentUser u, Long reviewId, Verdict verdict, String comment) {
        u.require(Role.QA);
        QaReview r = reviews.findById(reviewId).orElseThrow(() -> new Errors.NotFound("Review not found"));
        if (r.status != QaStatus.CLAIMED || !u.id().equals(r.assignedTo)) throw new Errors.Forbidden("Review is not claimed by you");
        if (verdict == null) throw new Errors.BadRequest("verdict must be PASS or FAIL");
        String note = comment == null || comment.isBlank() ? null : comment.trim();
        if (verdict == Verdict.FAIL && note == null) throw new Errors.BadRequest("A comment is required when failing a document");
        Transcription t = transcriptions.findById(r.transcriptionId).orElseThrow();
        r.reviewedVersionNo = versions.findById(t.currentVersionId).map(v -> v.versionNo).orElse(null);
        r.status = verdict == Verdict.PASS ? QaStatus.PASSED : QaStatus.FAILED;
        r.comment = note;
        r.completedAt = Instant.now();
        reviews.save(r);
        audit.record(u.id(), "QA_REVIEW", r.id, "VERDICT_" + verdict, note);
        if (verdict == Verdict.FAIL) {
            String doc = documents.findById(t.documentId).map(d -> d.originalFilename).orElse("document");
            specialist.reopenForQaFailure(r.requestId, u.id(), "QA failed " + doc + ": " + note);
        }
        return r;
    }
}
