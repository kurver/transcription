package com.example.transcription.service;

import com.example.transcription.domain.*;
import com.example.transcription.repo.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Document-level view of a transcription plus append-only versioning. Always reached through its request. */
@Service
public class TranscriptionService {
    public record TranscriptionView(Long id, Long requestId, String requestTitle, String caseId, Long documentId,
                                    String documentName, int currentVersionNo, long versionCount,
                                    QaStatus qaStatus, Long qaReviewId, Long qaAssignedTo, String qaComment,
                                    Long taskId, TaskStatus taskStatus, Long taskAssignedTo) {}

    public record Detail(TranscriptionView summary, String content, TranscriptionVersion currentVersion) {}

    private final AccessService access;
    private final TranscriptionRepository transcriptions;
    private final VersionRepository versions;
    private final RequestRepository requests;
    private final DocumentRepository documents;
    private final SpecialistTaskRepository tasks;
    private final QaReviewRepository reviews;
    private final AuditService audit;
    private final com.example.transcription.storage.FileStorage storage;

    public record Audio(String filename, String contentType, byte[] bytes) {}

    public TranscriptionService(AccessService access, TranscriptionRepository transcriptions, VersionRepository versions,
                                RequestRepository requests, DocumentRepository documents, SpecialistTaskRepository tasks,
                                QaReviewRepository reviews, AuditService audit,
                                com.example.transcription.storage.FileStorage storage) {
        this.access = access;
        this.transcriptions = transcriptions;
        this.versions = versions;
        this.requests = requests;
        this.documents = documents;
        this.tasks = tasks;
        this.reviews = reviews;
        this.audit = audit;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public Detail detail(CurrentUser u, Long id) {
        Transcription t = access.transcription(u, id);
        TranscriptionVersion v = versions.findById(t.currentVersionId).orElseThrow();
        return new Detail(view(t, v.versionNo), v.content, v);
    }

    @Transactional(readOnly = true)
    public List<TranscriptionVersion> history(CurrentUser u, Long id) {
        Transcription t = access.transcription(u, id);
        return versions.findByTranscriptionIdOrderByVersionNo(t.id);
    }

    /** Edit or replace: always appends a new version; earlier versions are never touched. */
    @Transactional
    public TranscriptionVersion addVersion(CurrentUser u, Long transcriptionId, VersionSource source,
                                           String content, String comment) {
        u.require(Role.TRANSCRIBER);
        if (source != VersionSource.EDIT && source != VersionSource.REPLACEMENT)
            throw new Errors.BadRequest("mode must be EDIT or REPLACEMENT");
        if (content == null || content.isBlank()) throw new Errors.BadRequest("Content is required");
        if (comment == null || comment.isBlank()) throw new Errors.BadRequest("A comment is required");
        Transcription t = access.transcription(u, transcriptionId);
        tasks.findFirstByRequestIdAndStatusAndAssignedTo(t.requestId, TaskStatus.CLAIMED, u.id())
                .orElseThrow(() -> new Errors.Forbidden("Claim the request before changing its transcriptions"));
        return appendVersion(t, source, content, u.id(), comment.trim());
    }

    /** Used by the worker for version 1 and by addVersion. */
    @Transactional
    public TranscriptionVersion appendVersion(Transcription t, VersionSource source, String content,
                                              Long authorId, String comment) {
        TranscriptionVersion v = new TranscriptionVersion();
        v.transcriptionId = t.id;
        v.versionNo = (int) versions.countByTranscriptionId(t.id) + 1;
        v.content = content;
        v.source = source;
        v.authorId = authorId;
        v.comment = comment;
        v = versions.save(v);
        t.currentVersionId = v.id;
        transcriptions.save(t);
        audit.record(authorId, "TRANSCRIPTION", t.id, "VERSION_" + source, "v" + v.versionNo
                + (comment == null ? "" : ": " + comment));
        return v;
    }

    /** The original recording, for playback while reviewing or editing. Same visibility as the transcription. */
    @Transactional(readOnly = true)
    public Audio audio(CurrentUser u, Long id) {
        Transcription t = access.transcription(u, id);
        Document d = documents.findById(t.documentId).orElseThrow();
        try {
            return new Audio(d.originalFilename, d.contentType, storage.load(d.storageKey));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not read recording", e);
        }
    }

    private TranscriptionView view(Transcription t, int versionNo) {
        TranscriptionRequest r = requests.findById(t.requestId).orElseThrow();
        Document d = documents.findById(t.documentId).orElseThrow();
        QaReview qa = reviews.findByRequestIdOrderById(r.id).stream()
                .filter(q -> q.transcriptionId.equals(t.id)).reduce((a, b) -> b).orElse(null);
        SpecialistTask task = tasks.findByRequestIdOrderById(r.id).stream().reduce((a, b) -> b).orElse(null);
        return new TranscriptionView(t.id, r.id, r.title, r.caseId, d.id, d.originalFilename, versionNo,
                versions.countByTranscriptionId(t.id),
                qa == null ? QaStatus.NONE : qa.status, qa == null ? null : qa.id, qa == null ? null : qa.assignedTo,
                qa == null ? null : qa.comment,
                task == null ? null : task.id, task == null ? null : task.status, task == null ? null : task.assignedTo);
    }
}
