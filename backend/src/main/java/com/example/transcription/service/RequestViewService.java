package com.example.transcription.service;

import com.example.transcription.domain.*;
import com.example.transcription.repo.*;
import com.example.transcription.service.RequestViews.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds request-centric read models and the role-specific queues. */
@Service
@Transactional(readOnly = true)
public class RequestViewService {
    private static final Set<TaskStatus> OPEN_OR_CLAIMED = EnumSet.of(TaskStatus.OPEN, TaskStatus.CLAIMED);
    private static final Set<QaStatus> QA_ACTIVE = EnumSet.of(QaStatus.OPEN, QaStatus.CLAIMED);

    /** Priority (urgent first), then earliest due date (none last), then oldest first. */
    static final Comparator<RequestSummary> WORK_ORDER = Comparator
            .comparing((RequestSummary s) -> s.priority().ordinal(), Comparator.reverseOrder())
            .thenComparing(RequestSummary::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(RequestSummary::createdAt);

    private final AccessService access;
    private final RequestRepository requests;
    private final DocumentRepository documents;
    private final TranscriptionRepository transcriptions;
    private final VersionRepository versions;
    private final SpecialistTaskRepository tasks;
    private final QaReviewRepository reviews;

    public RequestViewService(AccessService access, RequestRepository requests, DocumentRepository documents,
                              TranscriptionRepository transcriptions, VersionRepository versions,
                              SpecialistTaskRepository tasks, QaReviewRepository reviews) {
        this.access = access;
        this.requests = requests;
        this.documents = documents;
        this.transcriptions = transcriptions;
        this.versions = versions;
        this.tasks = tasks;
        this.reviews = reviews;
    }

    // ---- queues ---------------------------------------------------------------------------

    /** USER: all my requests, newest first. */
    public List<RequestSummary> mine(CurrentUser u) {
        u.require(Role.USER);
        return requests.findByOwnerIdOrderByCreatedAtDesc(u.id()).stream().map(this::summarise).toList();
    }

    /** USER: my requests whose documents are all transcribed. Each item is a request, not a document. */
    public List<RequestSummary> completedQueue(CurrentUser u) {
        return mine(u).stream().filter(s -> s.status() == RequestStatus.COMPLETED).sorted(WORK_ORDER).toList();
    }

    /** TRANSCRIBER: shared queue — all open tasks plus the ones I have claimed. */
    public List<RequestSummary> specialistQueue(CurrentUser u) {
        u.require(Role.TRANSCRIBER);
        Set<Long> ids = tasks.findByStatusInOrderById(OPEN_OR_CLAIMED).stream()
                .filter(t -> t.status == TaskStatus.OPEN || u.id().equals(t.assignedTo))
                .map(t -> t.requestId).collect(Collectors.toCollection(LinkedHashSet::new));
        return summaries(ids);
    }

    /** QA: shared queue — requests with open reviews plus requests where I hold a claimed review. */
    public List<RequestSummary> qaQueue(CurrentUser u) {
        u.require(Role.QA);
        Set<Long> ids = reviews.findByStatusInOrderById(QA_ACTIVE).stream()
                .filter(r -> r.status == QaStatus.OPEN || u.id().equals(r.assignedTo))
                .map(r -> r.requestId).collect(Collectors.toCollection(LinkedHashSet::new));
        return summaries(ids);
    }

    /** QA: read-only view of every request. */
    public List<RequestSummary> qaAll(CurrentUser u) {
        u.require(Role.QA);
        return requests.findAllByOrderByIdDesc().stream().map(this::summarise).toList();
    }

    private List<RequestSummary> summaries(Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return requests.findByIdIn(ids).stream().map(this::summarise).sorted(WORK_ORDER).toList();
    }

    // ---- detail ---------------------------------------------------------------------------

    public RequestDetail detail(CurrentUser u, Long id) {
        TranscriptionRequest r = access.request(u, id);
        List<QaReview> allReviews = reviews.findByRequestIdOrderById(r.id);
        Map<Long, QaReview> latest = latestPerTranscription(allReviews);
        List<DocView> docs = documents.findByRequestIdOrderById(r.id).stream().map(d -> {
            Transcription t = transcriptions.findByDocumentId(d.id).orElse(null);
            QaReview qa = t == null ? null : latest.get(t.id);
            int vNo = t == null || t.currentVersionId == null ? 0
                    : versions.findById(t.currentVersionId).map(v -> v.versionNo).orElse(0);
            return new DocView(d.id, d.originalFilename, d.sizeBytes, d.status, d.failureReason,
                    t == null ? null : t.id, vNo, t == null ? 0 : versions.countByTranscriptionId(t.id),
                    qa == null ? QaStatus.NONE : qa.status, qa == null ? null : qa.id,
                    qa == null ? null : qa.assignedTo, qa == null ? null : qa.comment);
        }).toList();
        return new RequestDetail(summarise(r), new LinkedHashMap<>(r.customFields), docs,
                tasks.findByRequestIdOrderById(r.id), allReviews);
    }

    // ---- shared mapping -------------------------------------------------------------------

    public RequestSummary summarise(TranscriptionRequest r) {
        List<Document> docs = documents.findByRequestIdOrderById(r.id);
        int transcribed = (int) docs.stream().filter(d -> d.status == DocumentStatus.TRANSCRIBED).count();
        SpecialistInfo spec = tasks.findByRequestIdOrderById(r.id).stream().reduce((a, b) -> b)
                .map(t -> new SpecialistInfo(t.id, t.status, t.assignedTo, t.source, t.reason)).orElse(null);
        Map<QaStatus, Long> counts = latestPerTranscription(reviews.findByRequestIdOrderById(r.id)).values().stream()
                .collect(Collectors.groupingBy(q -> q.status, Collectors.counting()));
        QaCounts qa = new QaCounts(counts.getOrDefault(QaStatus.OPEN, 0L).intValue(),
                counts.getOrDefault(QaStatus.CLAIMED, 0L).intValue(),
                counts.getOrDefault(QaStatus.PASSED, 0L).intValue(),
                counts.getOrDefault(QaStatus.FAILED, 0L).intValue());
        return new RequestSummary(r.id, r.title, r.status, r.ownerId, r.caseId, r.caseOwner, r.dueDate, r.priority,
                r.createdAt, r.submittedAt, docs.size(), transcribed, spec, qa);
    }

    /** Latest review per transcription (reviews arrive ordered by id). */
    static Map<Long, QaReview> latestPerTranscription(List<QaReview> all) {
        return all.stream().collect(Collectors.toMap(q -> q.transcriptionId, Function.identity(), (a, b) -> b, LinkedHashMap::new));
    }
}
