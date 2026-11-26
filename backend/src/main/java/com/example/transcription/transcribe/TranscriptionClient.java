package com.example.transcription.transcribe;

/** Client for the external transcription API (Azure AI Foundry in production, mock for the POC). */
public interface TranscriptionClient {
    String transcribe(String filename, String contentType, byte[] content);
}
