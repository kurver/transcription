package com.example.transcription.web;

import com.example.transcription.domain.AuditEvent;
import com.example.transcription.domain.QaReview;
import com.example.transcription.domain.Role;
import com.example.transcription.domain.SpecialistTask;
import com.example.transcription.repo.AuditRepository;
import com.example.transcription.service.*;
import com.example.transcription.service.RequestViews.RequestSummary;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

/** Shared queues: the specialist (transcriber) queue and the QA queue. */
@RestController
@RequestMapping("/api")
public class QueueController {
    public record VerdictBody(QaService.Verdict verdict, String comment) {}

    private final RequestViewService views;
    private final SpecialistService specialist;
    private final QaService qa;
    private final AuditRepository auditRepo;

    public QueueController(RequestViewService views, SpecialistService specialist, QaService qa, AuditRepository auditRepo) {
        this.views = views;
        this.specialist = specialist;
        this.qa = qa;
        this.auditRepo = auditRepo;
    }

    // ---- specialist queue (TRANSCRIBER) ---------------------------------------------------

    @GetMapping("/specialist/queue")
    public List<RequestSummary> specialistQueue(CurrentUser u) { return views.specialistQueue(u); }

    @PostMapping("/specialist/tasks/{taskId}/claim")
    public SpecialistTask claim(CurrentUser u, @PathVariable Long taskId) { return specialist.claim(u, taskId); }

    @PostMapping("/specialist/tasks/{taskId}/complete")
    public SpecialistTask complete(CurrentUser u, @PathVariable Long taskId) { return specialist.complete(u, taskId); }

    // ---- QA queue (QA) --------------------------------------------------------------------

    @GetMapping("/qa/queue")
    public List<RequestSummary> qaQueue(CurrentUser u) { return views.qaQueue(u); }

    @PostMapping("/qa/requests/{requestId}/claim")
    public List<QaReview> claimQa(CurrentUser u, @PathVariable Long requestId) { return qa.claimRequest(u, requestId); }

    @PostMapping("/qa/reviews/{reviewId}/verdict")
    public QaReview verdict(CurrentUser u, @PathVariable Long reviewId, @RequestBody VerdictBody body) {
        return qa.verdict(u, reviewId, body.verdict(), body.comment());
    }

    /** Read-only list of every request, for QA oversight. */
    @GetMapping("/qa/requests")
    public List<RequestSummary> qaAll(CurrentUser u) { return views.qaAll(u); }

    @GetMapping("/qa/audit")
    public List<AuditEvent> audit(CurrentUser u, @RequestParam(defaultValue = "200") int limit) {
        u.require(Role.QA);
        return auditRepo.findAllByOrderByIdDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 1000)));
    }
}
