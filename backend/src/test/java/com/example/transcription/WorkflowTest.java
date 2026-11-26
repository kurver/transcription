package com.example.transcription;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.mock.web.MockMultipartFile;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:wf;DB_CLOSE_DELAY=-1",
        "app.processing.enabled=false", "app.storage.root=target/test-uploads", "app.qa.sample-rate=0"})
@AutoConfigureMockMvc
class WorkflowTest extends TestSupport {

    @Test
    void metadataIsStoredAuditedAndPrivate() throws Exception {
        String body = """
                {"title":"Smith v Jones","caseId":"C-1042","caseOwner":"Dana Lee","dueDate":"2026-12-24",
                 "priority":"HIGH","customFields":{"Court":"District","Jurisdiction":"NZ"}}""";
        long req = num(json(mvc.perform(post("/api/requests").header("X-User-Id", ALICE).contentType(JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.caseId").value("C-1042"))
                .andExpect(jsonPath("$.priority").value("HIGH"))), "$.id");
        mvc.perform(get("/api/requests/" + req).header("X-User-Id", ALICE))
                .andExpect(jsonPath("$.summary.caseOwner").value("Dana Lee"))
                .andExpect(jsonPath("$.summary.dueDate").value("2026-12-24"))
                .andExpect(jsonPath("$.customFields.Court").value("District"));

        mvc.perform(put("/api/requests/" + req + "/metadata").header("X-User-Id", ALICE).contentType(JSON)
                .content("{\"title\":\"Smith v Jones\",\"caseId\":\"C-1043\",\"priority\":\"URGENT\",\"customFields\":{\"Court\":\"High\"}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.caseId").value("C-1043"));
        mvc.perform(get("/api/qa/audit").header("X-User-Id", QUINN))
                .andExpect(jsonPath("$[?(@.action=='METADATA_UPDATED')].detail", hasItem(containsString("caseId=C-1042"))));

        mvc.perform(get("/api/requests/" + req).header("X-User-Id", BOB)).andExpect(status().isNotFound());
        mvc.perform(put("/api/requests/" + req + "/metadata").header("X-User-Id", BOB).contentType(JSON)
                .content("{\"title\":\"x\"}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/requests").header("X-User-Id", ALICE).contentType(JSON).content("{\"title\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sharedSpecialistQueueEditingAndQaFlow() throws Exception {
        long req = completedRequest(ALICE, "Contracts", "a.mp3", "b.wav");
        mvc.perform(get("/api/requests/" + req).header("X-User-Id", ALICE)).andExpect(jsonPath("$.summary.status").value("COMPLETED"));

        // the user's completed queue is request-level
        mvc.perform(get("/api/queue/completed").header("X-User-Id", ALICE))
                .andExpect(jsonPath("$[?(@.id==" + req + ")].documentCount", contains(2)));
        assertInQueue("/api/queue/completed", BOB, req, false);

        // transcribers see nothing until the owner sends it to the specialist team
        assertInQueue("/api/specialist/queue", TINA, req, false);
        mvc.perform(get("/api/requests/" + req).header("X-User-Id", TINA)).andExpect(status().isNotFound());
        mvc.perform(post("/api/requests/" + req + "/send-to-specialists").header("X-User-Id", BOB)).andExpect(status().isNotFound());
        long task = num(json(mvc.perform(post("/api/requests/" + req + "/send-to-specialists").header("X-User-Id", ALICE)
                .contentType(JSON).content("{\"reason\":\"needs legal review\"}")).andExpect(status().isOk())), "$.id");
        mvc.perform(post("/api/requests/" + req + "/send-to-specialists").header("X-User-Id", ALICE)).andExpect(status().isBadRequest());

        // shared queue: every transcriber sees it; first to claim takes it
        assertInQueue("/api/specialist/queue", TINA, req, true);
        assertInQueue("/api/specialist/queue", TERRY, req, true);
        mvc.perform(post("/api/specialist/tasks/" + task + "/claim").header("X-User-Id", TINA)).andExpect(status().isOk());
        assertInQueue("/api/specialist/queue", TERRY, req, false);
        mvc.perform(post("/api/specialist/tasks/" + task + "/claim").header("X-User-Id", TERRY)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/specialist/tasks/" + task + "/claim").header("X-User-Id", ALICE)).andExpect(status().isForbidden());

        // only the claiming transcriber can edit; comment required; history append-only
        long doc1 = transcriptionOf(TINA, req, 0), doc2 = transcriptionOf(TINA, req, 1);
        String edit = "{\"mode\":\"EDIT\",\"content\":\"fixed\",\"comment\":\"typo\"}";
        mvc.perform(post("/api/transcriptions/" + doc1 + "/versions").header("X-User-Id", TERRY).contentType(JSON).content(edit))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/transcriptions/" + doc1 + "/versions").header("X-User-Id", TINA).contentType(JSON)
                .content("{\"mode\":\"EDIT\",\"content\":\"x\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/transcriptions/" + doc1 + "/versions").header("X-User-Id", TINA).contentType(JSON).content(edit))
                .andExpect(status().isOk());
        mvc.perform(post("/api/transcriptions/" + doc1 + "/versions").header("X-User-Id", TINA).contentType(JSON)
                .content("{\"mode\":\"REPLACEMENT\",\"content\":\"all new\",\"comment\":\"rewrite\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/transcriptions/" + doc1 + "/versions").header("X-User-Id", QUINN))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].source").value("MOCK_API"))
                .andExpect(jsonPath("$[0].content").value(startsWith("transcribed")))
                .andExpect(jsonPath("$[2].source").value("REPLACEMENT"));

        // QA on a doc-by-doc basis: owner or the working transcriber, never twice, never QA itself
        mvc.perform(post("/api/transcriptions/" + doc1 + "/qa-request").header("X-User-Id", ALICE)).andExpect(status().isOk());
        mvc.perform(post("/api/transcriptions/" + doc1 + "/qa-request").header("X-User-Id", ALICE)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/transcriptions/" + doc2 + "/qa-request").header("X-User-Id", TERRY)).andExpect(status().isForbidden());
        mvc.perform(post("/api/transcriptions/" + doc2 + "/qa-request").header("X-User-Id", TINA)).andExpect(status().isOk());
        mvc.perform(post("/api/transcriptions/" + doc2 + "/qa-request").header("X-User-Id", QUINN)).andExpect(status().isForbidden());

        // shared QA queue is request-level; QA claims the request, then records verdicts per document
        mvc.perform(get("/api/qa/queue").header("X-User-Id", ALICE)).andExpect(status().isForbidden());
        assertQaOpen(QUINN, req, 2);
        mvc.perform(post("/api/qa/requests/" + req + "/claim").header("X-User-Id", QUINN)).andExpect(status().isOk());
        mvc.perform(post("/api/qa/requests/" + req + "/claim").header("X-User-Id", QUINN)).andExpect(status().isBadRequest());
        String d = detail(QUINN, req);
        long review1 = num(d, "$.documents[0].qaReviewId"), review2 = num(d, "$.documents[1].qaReviewId");
        mvc.perform(post("/api/qa/reviews/" + review1 + "/verdict").header("X-User-Id", ALICE).contentType(JSON)
                .content("{\"verdict\":\"PASS\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/qa/reviews/" + review2 + "/verdict").header("X-User-Id", QUINN).contentType(JSON)
                .content("{\"verdict\":\"FAIL\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/qa/reviews/" + review1 + "/verdict").header("X-User-Id", QUINN).contentType(JSON)
                .content("{\"verdict\":\"PASS\",\"comment\":\"good\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewedVersionNo").value(3));
        mvc.perform(post("/api/qa/reviews/" + review2 + "/verdict").header("X-User-Id", QUINN).contentType(JSON)
                .content("{\"verdict\":\"FAIL\",\"comment\":\"missing section\"}")).andExpect(status().isOk());
        assertInQueue("/api/qa/queue", QUINN, req, false);

        // finishing the task re-queues the failed document for QA recheck
        mvc.perform(post("/api/specialist/tasks/" + task + "/complete").header("X-User-Id", TERRY)).andExpect(status().isForbidden());
        mvc.perform(post("/api/specialist/tasks/" + task + "/complete").header("X-User-Id", TINA)).andExpect(status().isOk());
        assertQaOpen(QUINN, req, 1);
        mvc.perform(get("/api/requests/" + req).header("X-User-Id", QUINN))
                .andExpect(jsonPath("$.reviews.length()").value(3))
                .andExpect(jsonPath("$.documents[0].qaStatus").value("PASSED"));
    }

    @Test
    void failedDocumentCanBeRetried() throws Exception {
        long req = completedRequest(BOB, "Failing", "bad.mp3", "good.mp3");
        mvc.perform(get("/api/requests/" + req).header("X-User-Id", BOB)).andExpect(jsonPath("$.summary.status").value("FAILED"));
        assertInQueue("/api/queue/completed", BOB, req, false);
    }

    @Test
    void cannotSubmitEmptyRequestAndRequiresIdentity() throws Exception {
        long req = createRequest(ALICE, "Empty");
        mvc.perform(post("/api/requests/" + req + "/submit").header("X-User-Id", ALICE)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/requests")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/requests").header("X-User-Id", QUINN)).andExpect(status().isForbidden());
    }

    @Test
    void onlyAudioIsAccepted_andRecordingIsPlayableByOwnerOnly() throws Exception {
        long req = createRequest(ALICE, "Audio only");
        mvc.perform(multipart("/api/requests/" + req + "/documents")
                .file(new MockMultipartFile("file", "contract.pdf", "application/pdf", "data".getBytes()))
                .header("X-User-Id", ALICE)).andExpect(status().isBadRequest());
        long done = completedRequest(ALICE, "Playback", "call.mp3");
        String d = json(mvc.perform(get("/api/requests/" + done).header("X-User-Id", ALICE)));
        long tid = num(d, "$.documents[0].transcriptionId");
        mvc.perform(get("/api/transcriptions/" + tid + "/audio").header("X-User-Id", ALICE))
                .andExpect(status().isOk()).andExpect(content().contentType("audio/mpeg"));
        mvc.perform(get("/api/transcriptions/" + tid + "/audio").header("X-User-Id", BOB)).andExpect(status().isNotFound());
    }

    @Test
    void queuesAreOrderedByPriorityThenDueDate() throws Exception {
        long low = num(json(mvc.perform(post("/api/requests").header("X-User-Id", BOB).contentType(JSON)
                .content("{\"title\":\"low\",\"priority\":\"LOW\"}"))), "$.id");
        long urgent = num(json(mvc.perform(post("/api/requests").header("X-User-Id", BOB).contentType(JSON)
                .content("{\"title\":\"urgent\",\"priority\":\"URGENT\",\"dueDate\":\"2026-12-01\"}"))), "$.id");
        long soon = num(json(mvc.perform(post("/api/requests").header("X-User-Id", BOB).contentType(JSON)
                .content("{\"title\":\"normal-soon\",\"dueDate\":\"2026-12-02\"}"))), "$.id");
        for (long r : new long[] {low, urgent, soon}) {
            upload(BOB, r, "f.mp3");
            mvc.perform(post("/api/requests/" + r + "/submit").header("X-User-Id", BOB));
        }
        worker.processPending();
        for (long r : new long[] {low, urgent, soon})
            mvc.perform(post("/api/requests/" + r + "/send-to-specialists").header("X-User-Id", BOB)).andExpect(status().isOk());
        mvc.perform(get("/api/specialist/queue").header("X-User-Id", TINA))
                .andExpect(jsonPath("$[?(@.title in ['urgent','normal-soon','low'])].title", contains("urgent", "normal-soon", "low")));
    }
}
