package com.example.transcription.web;

import com.example.transcription.domain.*;
import com.example.transcription.repo.*;
import com.example.transcription.service.*;
import com.example.transcription.service.RequestViews.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ApiController {
    public record RequestPayload(String title, String caseId, String caseOwner, LocalDate dueDate, Priority priority,
                              Map<String, String> customFields) {
        RequestService.Metadata toMetadata() { return new RequestService.Metadata(title, caseId, caseOwner, dueDate, priority, customFields); }
    }
    public record VersionRequest(VersionSource mode, String content, String comment) {}
    public record ReasonBody(String reason) {}

    private final AppUserRepository users;
    private final RequestService requestService;
    private final RequestViewService views;
    private final TranscriptionService transcriptionService;
    private final SpecialistService specialist;
    private final QaService qa;

    public ApiController(AppUserRepository users, RequestService requestService, RequestViewService views,
                         TranscriptionService transcriptionService, SpecialistService specialist, QaService qa) {
        this.users = users;
        this.requestService = requestService;
        this.views = views;
        this.transcriptionService = transcriptionService;
        this.specialist = specialist;
        this.qa = qa;
    }

    /** Feeds the dev role switcher; no identity needed. */
    @GetMapping("/users")
    public List<AppUser> users() { return users.findAll(); }

    @GetMapping("/me")
    public CurrentUser me(CurrentUser u) { return u; }

    // ---- requests -------------------------------------------------------------------------

    @GetMapping("/requests")
    public List<RequestSummary> requests(CurrentUser u) { return views.mine(u); }

    @PostMapping("/requests")
    public RequestSummary create(CurrentUser u, @RequestBody RequestPayload body) {
        return views.summarise(requestService.create(u, body.toMetadata()));
    }

    /** Visible to the owner, to transcribers once the request has a specialist task, and to QA. */
    @GetMapping("/requests/{id}")
    public RequestDetail request(CurrentUser u, @PathVariable Long id) { return views.detail(u, id); }

    @PutMapping("/requests/{id}/metadata")
    public RequestSummary updateMetadata(CurrentUser u, @PathVariable Long id, @RequestBody RequestPayload body) {
        return views.summarise(requestService.updateMetadata(u, id, body.toMetadata()));
    }

    @PostMapping(value = "/requests/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Document upload(CurrentUser u, @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return requestService.addDocument(u, id, file);
    }

    @PostMapping("/requests/{id}/submit")
    public RequestSummary submit(CurrentUser u, @PathVariable Long id) { return views.summarise(requestService.submit(u, id)); }

    @PostMapping("/requests/{id}/retry")
    public RequestSummary retry(CurrentUser u, @PathVariable Long id) { return views.summarise(requestService.retry(u, id)); }

    @PostMapping("/requests/{id}/send-to-specialists")
    public SpecialistTask sendToSpecialists(CurrentUser u, @PathVariable Long id, @RequestBody(required = false) ReasonBody body) {
        return specialist.sendToSpecialists(u, id, body == null ? null : body.reason());
    }

    /** USER: my completed requests (request-level, never individual documents). */
    @GetMapping("/queue/completed")
    public List<RequestSummary> completed(CurrentUser u) { return views.completedQueue(u); }

    // ---- documents' transcriptions (always reached from a request) ------------------------

    @GetMapping("/transcriptions/{id}")
    public TranscriptionService.Detail transcription(CurrentUser u, @PathVariable Long id) { return transcriptionService.detail(u, id); }

    @GetMapping("/transcriptions/{id}/audio")
    public org.springframework.http.ResponseEntity<byte[]> audio(CurrentUser u, @PathVariable Long id) {
        var a = transcriptionService.audio(u, id);
        String type = a.contentType() == null || !a.contentType().startsWith("audio/") ? "audio/mpeg" : a.contentType();
        return org.springframework.http.ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).body(a.bytes());
    }

    @GetMapping("/transcriptions/{id}/versions")
    public List<TranscriptionVersion> versions(CurrentUser u, @PathVariable Long id) { return transcriptionService.history(u, id); }

    @PostMapping("/transcriptions/{id}/versions")
    public TranscriptionVersion addVersion(CurrentUser u, @PathVariable Long id, @RequestBody VersionRequest body) {
        return transcriptionService.addVersion(u, id, body.mode(), body.content(), body.comment());
    }

    /** USER (owner) or the transcriber working the request: ask for QA on one document. */
    @PostMapping("/transcriptions/{id}/qa-request")
    public QaReview requestQa(CurrentUser u, @PathVariable Long id) { return qa.requestReview(u, id); }
}
