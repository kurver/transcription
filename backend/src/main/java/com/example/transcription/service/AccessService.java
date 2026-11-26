package com.example.transcription.service;

import com.example.transcription.domain.Role;
import com.example.transcription.domain.Transcription;
import com.example.transcription.domain.TranscriptionRequest;
import com.example.transcription.repo.RequestRepository;
import com.example.transcription.repo.SpecialistTaskRepository;
import com.example.transcription.repo.TranscriptionRepository;
import org.springframework.stereotype.Service;

/**
 * Single place for read visibility. Users see only their own requests (others look nonexistent),
 * transcribers see requests that have a specialist task, QA sees everything (read-only).
 */
@Service
public class AccessService {
    private final RequestRepository requests;
    private final TranscriptionRepository transcriptions;
    private final SpecialistTaskRepository tasks;

    public AccessService(RequestRepository requests, TranscriptionRepository transcriptions, SpecialistTaskRepository tasks) {
        this.requests = requests;
        this.transcriptions = transcriptions;
        this.tasks = tasks;
    }

    public TranscriptionRequest request(CurrentUser u, Long id) {
        TranscriptionRequest r = requests.findById(id).orElseThrow(() -> new Errors.NotFound("Request not found"));
        boolean ok = switch (u.role()) {
            case USER -> r.ownerId.equals(u.id());
            case TRANSCRIBER -> !tasks.findByRequestIdOrderById(r.id).isEmpty();
            case QA -> true;
        };
        if (!ok) throw new Errors.NotFound("Request not found");
        return r;
    }

    public Transcription transcription(CurrentUser u, Long id) {
        Transcription t = transcriptions.findById(id).orElseThrow(() -> new Errors.NotFound("Transcription not found"));
        try {
            request(u, t.requestId);
        } catch (Errors.NotFound e) {
            throw new Errors.NotFound("Transcription not found");
        }
        return t;
    }

    public boolean isQa(CurrentUser u) { return u.role() == Role.QA; }
}
