package com.example.transcription;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import com.example.transcription.processing.ProcessingWorker;
import com.example.transcription.transcribe.TranscriptionClient;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

abstract class TestSupport {
    static final long ALICE = 1, BOB = 2, TINA = 3, TERRY = 4, QUINN = 5;
    static final MediaType JSON = MediaType.APPLICATION_JSON;

    @TestConfiguration
    static class Stub {
        @Bean
        @Primary
        TranscriptionClient client() {
            return (name, type, bytes) -> {
                if (name.startsWith("bad")) throw new IllegalStateException("boom");
                return "transcribed " + name;
            };
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ProcessingWorker worker;

    String json(ResultActions a) throws Exception { return a.andReturn().getResponse().getContentAsString(); }

    long num(String body, String path) { return ((Number) JsonPath.read(body, path)).longValue(); }

    long createRequest(long user, String title) throws Exception {
        return num(json(mvc.perform(post("/api/requests").header("X-User-Id", user).contentType(JSON)
                .content("{\"title\":\"" + title + "\"}")).andExpect(status().isOk())), "$.id");
    }

    void upload(long user, long req, String filename) throws Exception {
        mvc.perform(multipart("/api/requests/" + req + "/documents")
                .file(new MockMultipartFile("file", filename, "audio/mpeg", "data".getBytes()))
                .header("X-User-Id", user)).andExpect(status().isOk());
    }

    /** Creates a request with the given documents, submits it and runs the worker so it completes. */
    long completedRequest(long user, String title, String... files) throws Exception {
        long req = createRequest(user, title);
        for (String f : files) upload(user, req, f);
        mvc.perform(post("/api/requests/" + req + "/submit").header("X-User-Id", user)).andExpect(status().isOk());
        worker.processPending();
        return req;
    }

    String detail(long user, long req) throws Exception {
        return json(mvc.perform(get("/api/requests/" + req).header("X-User-Id", user)).andExpect(status().isOk()));
    }

    long transcriptionOf(long user, long req, int docIndex) throws Exception {
        return num(detail(user, req), "$.documents[" + docIndex + "].transcriptionId");
    }

    /** Queues are shared across tests in one context, so assertions are scoped to a single request id. */
    void assertInQueue(String path, long user, long req, boolean expected) throws Exception {
        mvc.perform(get(path).header("X-User-Id", user)).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + req + ")]", hasSize(expected ? 1 : 0)));
    }

    void assertQaOpen(long user, long req, int open) throws Exception {
        mvc.perform(get("/api/qa/queue").header("X-User-Id", user))
                .andExpect(jsonPath("$[?(@.id==" + req + ")].qa.open", contains(open)));
    }
}
