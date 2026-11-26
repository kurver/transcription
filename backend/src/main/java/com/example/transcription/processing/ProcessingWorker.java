package com.example.transcription.processing;

import com.example.transcription.domain.*;
import com.example.transcription.repo.*;
import com.example.transcription.service.AuditService;
import com.example.transcription.service.QaService;
import com.example.transcription.service.TranscriptionService;
import com.example.transcription.storage.FileStorage;
import com.example.transcription.transcribe.TranscriptionClient;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Polls for QUEUED documents, calls the transcription API, stores version 1 and rolls up request status. */
@Component
public class ProcessingWorker {
    private static final Logger log = LoggerFactory.getLogger(ProcessingWorker.class);

    private final DocumentRepository documents;
    private final RequestRepository requests;
    private final TranscriptionRepository transcriptions;
    private final TranscriptionService transcriptionService;
    private final TranscriptionClient client;
    private final FileStorage storage;
    private final AuditService audit;
    private final TransactionTemplate tx;
    private final QaService qa;

    public ProcessingWorker(DocumentRepository documents, RequestRepository requests,
                            TranscriptionRepository transcriptions, TranscriptionService transcriptionService,
                            TranscriptionClient client, FileStorage storage, AuditService audit, TransactionTemplate tx,
                            QaService qa) {
        this.documents = documents;
        this.requests = requests;
        this.transcriptions = transcriptions;
        this.transcriptionService = transcriptionService;
        this.client = client;
        this.storage = storage;
        this.audit = audit;
        this.tx = tx;
        this.qa = qa;
    }

    @Scheduled(fixedDelayString = "${app.processing.poll-ms:2000}")
    public void poll() {
        try {
            processPending();
        } catch (RuntimeException e) {
            log.error("Processing poll failed", e);
        }
    }

    /** Public so tests can drive it synchronously. */
    public void processPending() {
        List<Long> ids = tx.execute(s -> {
            List<Document> queued = documents.findByStatusOrderById(DocumentStatus.QUEUED);
            queued.forEach(d -> {
                d.status = DocumentStatus.PROCESSING;
                documents.save(d);
                refreshRequest(d.requestId);
            });
            return queued.stream().map(d -> d.id).toList();
        });
        if (ids == null) return;
        for (Long id : ids) processOne(id);
    }

    private void processOne(Long docId) {
        Document d = tx.execute(s -> documents.findById(docId).orElseThrow());
        String text = null;
        String error = null;
        try {
            byte[] bytes = storage.load(d.storageKey);
            text = client.transcribe(d.originalFilename, d.contentType, bytes);
        } catch (Exception e) {
            log.warn("Transcription failed for document {}: {}", docId, e.toString());
            error = e.getMessage() == null ? e.toString() : e.getMessage();
        }
        final String result = text;
        final String failure = error;
        tx.executeWithoutResult(s -> {
            Document doc = documents.findById(docId).orElseThrow();
            if (result != null) {
                Transcription t = new Transcription();
                t.documentId = doc.id;
                t.requestId = doc.requestId;
                t.ownerId = requests.findById(doc.requestId).orElseThrow().ownerId;
                t = transcriptions.save(t);
                transcriptionService.appendVersion(t, VersionSource.MOCK_API, result, null, "Initial transcription");
                doc.status = DocumentStatus.TRANSCRIBED;
                audit.record(null, "DOCUMENT", doc.id, "TRANSCRIBED", doc.originalFilename);
            } else {
                doc.status = DocumentStatus.FAILED;
                doc.failureReason = failure.length() > 900 ? failure.substring(0, 900) : failure;
                audit.record(null, "DOCUMENT", doc.id, "FAILED", doc.failureReason);
            }
            documents.save(doc);
            refreshRequest(doc.requestId);
        });
    }

    /** Request status is derived from its documents. */
    private void refreshRequest(Long requestId) {
        TranscriptionRequest r = requests.findById(requestId).orElseThrow();
        if (r.status == RequestStatus.DRAFT) return;
        List<Document> docs = documents.findByRequestIdOrderById(requestId);
        RequestStatus next;
        if (docs.stream().anyMatch(d -> d.status == DocumentStatus.PROCESSING)) next = RequestStatus.PROCESSING;
        else if (docs.stream().anyMatch(d -> d.status == DocumentStatus.QUEUED)) next = RequestStatus.QUEUED;
        else if (docs.stream().anyMatch(d -> d.status == DocumentStatus.FAILED)) next = RequestStatus.FAILED;
        else next = RequestStatus.COMPLETED;
        if (next != r.status) {
            r.status = next;
            requests.save(r);
            audit.record(null, "REQUEST", r.id, "STATUS_" + next, null);
            if (next == RequestStatus.COMPLETED) qa.sampleRequest(r.id);   // random QA sampling
        }
    }
}
