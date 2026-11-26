package com.example.transcription.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "transcription_request")
public class TranscriptionRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long ownerId;
    public String title;
    public String caseId;
    public String caseOwner;
    public LocalDate dueDate;
    @Enumerated(EnumType.STRING)
    public Priority priority = Priority.NORMAL;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "request_attribute", joinColumns = @JoinColumn(name = "request_id"))
    @MapKeyColumn(name = "attr_key")
    @Column(name = "attr_value")
    public Map<String, String> customFields = new LinkedHashMap<>();
    @Enumerated(EnumType.STRING)
    public RequestStatus status = RequestStatus.DRAFT;
    public Instant createdAt = Instant.now();
    public Instant submittedAt;
}
