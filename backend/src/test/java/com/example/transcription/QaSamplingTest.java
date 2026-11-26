package com.example.transcription;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.hamcrest.Matchers.contains;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/** Sampling rate forced to 100% so the random sampling path is deterministic. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:qa;DB_CLOSE_DELAY=-1",
        "app.processing.enabled=false", "app.storage.root=target/test-uploads", "app.qa.sample-rate=1.0"})
@AutoConfigureMockMvc
class QaSamplingTest extends TestSupport {

    @Test
    void completedRequestsAreSampledIntoSharedQaQueueAndFailuresReturnToSpecialists() throws Exception {
        long req = completedRequest(ALICE, "Sampled", "a.mp3", "b.mp3");
        assertQaOpen(QUINN, req, 2);
        assertInQueue("/api/specialist/queue", TINA, req, false);

        mvc.perform(post("/api/qa/requests/" + req + "/claim").header("X-User-Id", QUINN)).andExpect(status().isOk());
        long review = num(detail(QUINN, req), "$.documents[0].qaReviewId");
        mvc.perform(post("/api/qa/reviews/" + review + "/verdict").header("X-User-Id", QUINN).contentType(JSON)
                .content("{\"verdict\":\"FAIL\",\"comment\":\"wrong names\"}")).andExpect(status().isOk());

        // no specialist task was active, so QA failure re-opens one on the shared queue
        mvc.perform(get("/api/specialist/queue").header("X-User-Id", TINA))
                .andExpect(jsonPath("$[?(@.id==" + req + ")].specialist.source", contains("QA_FAILURE")))
                .andExpect(jsonPath("$[?(@.id==" + req + ")].specialist.reason", contains("QA failed a.mp3: wrong names")));
    }

    @Test
    void eachTranscriptionIsSampledOnlyOnce() throws Exception {
        long req = completedRequest(BOB, "Once", "a.mp3");
        mvc.perform(post("/api/qa/requests/" + req + "/claim").header("X-User-Id", QUINN)).andExpect(status().isOk());
        long review = num(detail(QUINN, req), "$.documents[0].qaReviewId");
        mvc.perform(post("/api/qa/reviews/" + review + "/verdict").header("X-User-Id", QUINN).contentType(JSON)
                .content("{\"verdict\":\"PASS\"}")).andExpect(status().isOk());

        // adding a document re-completes the request; only the new document is sampled
        upload(BOB, req, "c.mp3");
        worker.processPending();
        mvc.perform(get("/api/requests/" + req).header("X-User-Id", QUINN))
                .andExpect(jsonPath("$.reviews.length()").value(2))
                .andExpect(jsonPath("$.summary.qa.open").value(1));
    }
}
