package com.example.transcription.service;

import com.example.transcription.domain.*;
import com.example.transcription.repo.*;
import com.example.transcription.storage.FileStorage;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RequestService {
    /** Metadata supplied by the user when creating or editing a request. */
    public record Metadata(String title, String caseId, String caseOwner, LocalDate dueDate, Priority priority,
                           Map<String, String> customFields) {}

    private static final int MAX_CUSTOM_FIELDS = 20;

    private final RequestRepository requests;
    private final DocumentRepository documents;
    private final FileStorage storage;
    private final AuditService audit;

    public RequestService(RequestRepository requests, DocumentRepository documents, FileStorage storage, AuditService audit) {
        this.requests = requests;
        this.documents = documents;
        this.storage = storage;
        this.audit = audit;
    }

    @Transactional
    public TranscriptionRequest create(CurrentUser u, Metadata m) {
        u.require(Role.USER);
        TranscriptionRequest r = new TranscriptionRequest();
        r.ownerId = u.id();
        apply(r, m);
        r = requests.save(r);
        audit.record(u.id(), "REQUEST", r.id, "CREATED", describe(r));
        return r;
    }

    /** Metadata can be corrected at any time by the owner; the previous values are kept in the audit trail. */
    @Transactional
    public TranscriptionRequest updateMetadata(CurrentUser u, Long id, Metadata m) {
        TranscriptionRequest r = get(u, id);
        String before = describe(r);
        apply(r, m);
        r = requests.save(r);
        audit.record(u.id(), "REQUEST", r.id, "METADATA_UPDATED", "was: " + before);
        return r;
    }

    private void apply(TranscriptionRequest r, Metadata m) {
        if (m.title() == null || m.title().isBlank()) throw new Errors.BadRequest("Title is required");
        r.title = limit(m.title(), 255, "Title");
        r.caseId = blankToNull(limit(m.caseId(), 100, "Case ID"));
        r.caseOwner = blankToNull(limit(m.caseOwner(), 200, "Case owner"));
        r.dueDate = m.dueDate();
        r.priority = m.priority() == null ? Priority.NORMAL : m.priority();
        Map<String, String> custom = new LinkedHashMap<>();
        if (m.customFields() != null) {
            if (m.customFields().size() > MAX_CUSTOM_FIELDS) throw new Errors.BadRequest("At most " + MAX_CUSTOM_FIELDS + " custom fields");
            m.customFields().forEach((k, v) -> {
                if (k == null || k.isBlank()) return;
                custom.put(limit(k.trim(), 100, "Custom field name"), limit(v == null ? "" : v.trim(), 1000, "Custom field value"));
            });
        }
        r.customFields.clear();
        r.customFields.putAll(custom);
    }

    private static String limit(String s, int max, String what) {
        if (s != null && s.trim().length() > max) throw new Errors.BadRequest(what + " is too long (max " + max + ")");
        return s == null ? null : s.trim();
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }

    private static String describe(TranscriptionRequest r) {
        return "title=" + r.title + ", caseId=" + r.caseId + ", caseOwner=" + r.caseOwner + ", due=" + r.dueDate
                + ", priority=" + r.priority + ", custom=" + r.customFields;
    }

    /** Owner-only lookup for mutating operations. */
    @Transactional(readOnly = true)
    public TranscriptionRequest get(CurrentUser u, Long id) {
        u.require(Role.USER);
        return requests.findById(id).filter(r -> r.ownerId.equals(u.id()))
                .orElseThrow(() -> new Errors.NotFound("Request not found"));
    }

    @Transactional
    public Document addDocument(CurrentUser u, Long requestId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new Errors.BadRequest("File is empty");
        try {
            return addDocument(u, requestId, file.getOriginalFilename(), file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read upload", e);
        }
    }

    @Transactional
    public Document addDocument(CurrentUser u, Long requestId, String filename, String contentType, byte[] bytes) {
        TranscriptionRequest r = get(u, requestId);
        if (bytes == null || bytes.length == 0) throw new Errors.BadRequest("File is empty");
        if (!isAudio(filename, contentType)) {
            throw new Errors.BadRequest("Only audio recordings are supported (mp3, wav, m4a, aac, ogg, flac, wma, webm)");
        }
        String key;
        try {
            key = storage.store(new java.io.ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new IllegalStateException("Could not store file", e);
        }
        Document d = new Document();
        d.requestId = r.id;
        d.originalFilename = filename == null ? "unnamed" : filename;
        d.contentType = contentType;
        d.storageKey = key;
        d.sizeBytes = bytes.length;
        boolean submitted = r.status != RequestStatus.DRAFT;
        d.status = submitted ? DocumentStatus.QUEUED : DocumentStatus.PENDING;
        d = documents.save(d);
        if (submitted) {
            // new work on an already submitted request puts the request back in the queue
            r.status = RequestStatus.QUEUED;
            requests.save(r);
        }
        audit.record(u.id(), "DOCUMENT", d.id, "UPLOADED", d.originalFilename);
        return d;
    }

    private static final java.util.Set<String> AUDIO_EXT =
            java.util.Set.of("mp3", "wav", "m4a", "aac", "ogg", "oga", "opus", "flac", "wma", "webm", "amr");

    static boolean isAudio(String filename, String contentType) {
        if (contentType != null && contentType.toLowerCase().startsWith("audio/")) return true;
        if (filename == null) return false;
        int dot = filename.lastIndexOf('.');
        return dot >= 0 && AUDIO_EXT.contains(filename.substring(dot + 1).toLowerCase());
    }

    @Transactional
    public TranscriptionRequest submit(CurrentUser u, Long requestId) {
        TranscriptionRequest r = get(u, requestId);
        if (r.status != RequestStatus.DRAFT) throw new Errors.BadRequest("Request already submitted");
        List<Document> docs = documents.findByRequestIdOrderById(requestId);
        if (docs.isEmpty()) throw new Errors.BadRequest("Add at least one document before submitting");
        docs.forEach(d -> { d.status = DocumentStatus.QUEUED; documents.save(d); });
        r.status = RequestStatus.QUEUED;
        r.submittedAt = Instant.now();
        requests.save(r);
        audit.record(u.id(), "REQUEST", r.id, "SUBMITTED", docs.size() + " document(s)");
        return r;
    }

    /** Re-queue failed documents. */
    @Transactional
    public TranscriptionRequest retry(CurrentUser u, Long requestId) {
        TranscriptionRequest r = get(u, requestId);
        List<Document> failed = documents.findByRequestIdOrderById(requestId).stream()
                .filter(d -> d.status == DocumentStatus.FAILED).toList();
        if (failed.isEmpty()) throw new Errors.BadRequest("No failed documents");
        failed.forEach(d -> { d.status = DocumentStatus.QUEUED; d.failureReason = null; documents.save(d); });
        r.status = RequestStatus.QUEUED;
        requests.save(r);
        audit.record(u.id(), "REQUEST", r.id, "RETRIED", failed.size() + " document(s)");
        return r;
    }
}
