package com.example.transcription.transcribe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class HttpTranscriptionClient implements TranscriptionClient {
    public record Response(String text) {}

    private final RestClient client;

    public HttpTranscriptionClient(
            @Value("${app.transcription.base-url:http://localhost:${server.port:8080}/mock-api}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public String transcribe(String filename, String contentType, byte[] content) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() { return filename; }
        });
        Response r = client.post().uri("/transcribe")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(Response.class);
        if (r == null || r.text() == null) throw new IllegalStateException("Empty response from transcription API");
        return r.text();
    }
}
