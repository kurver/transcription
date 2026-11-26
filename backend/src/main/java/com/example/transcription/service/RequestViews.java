package com.example.transcription.service;

import com.example.transcription.domain.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Read-model records returned by the API. */
public final class RequestViews {
    private RequestViews() {}

    public record SpecialistInfo(Long taskId, TaskStatus status, Long assignedTo, TaskSource source, String reason) {}

    public record QaCounts(int open, int claimed, int passed, int failed) {}

    public record RequestSummary(Long id, String title, RequestStatus status, Long ownerId, String caseId, String caseOwner,
                                 LocalDate dueDate, Priority priority, Instant createdAt, Instant submittedAt,
                                 int documentCount, int transcribedCount, SpecialistInfo specialist, QaCounts qa) {}

    public record DocView(Long documentId, String filename, long sizeBytes, DocumentStatus status, String failureReason,
                          Long transcriptionId, int versionNo, long versionCount,
                          QaStatus qaStatus, Long qaReviewId, Long qaAssignedTo, String qaComment) {}

    public record RequestDetail(RequestSummary summary, Map<String, String> customFields, List<DocView> documents,
                                List<SpecialistTask> tasks, List<QaReview> reviews) {}
}
