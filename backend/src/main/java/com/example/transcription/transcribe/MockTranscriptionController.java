package com.example.transcription.transcribe;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Stand-in for the real transcription API. Returns a canned speaker-labelled transcript after a delay; can fail randomly. */
@RestController
@RequestMapping("/mock-api")
public class MockTranscriptionController {
    private final long minDelayMs;
    private final long maxDelayMs;
    private final double slowChance;
    private final double failureRate;

    /**
     * Delay is random so queue processing is visible: most calls take between min and max, and a
     * {@code slow-chance} share take up to 3x max. Set min=max for a fixed delay (tests do this).
     */
    public MockTranscriptionController(@Value("${app.mock.delay-min-ms:1000}") long minDelayMs,
                                       @Value("${app.mock.delay-max-ms:8000}") long maxDelayMs,
                                       @Value("${app.mock.slow-chance:0.15}") double slowChance,
                                       @Value("${app.mock.failure-rate:0.0}") double failureRate) {
        this.minDelayMs = minDelayMs;
        this.maxDelayMs = Math.max(minDelayMs, maxDelayMs);
        this.slowChance = slowChance;
        this.failureRate = failureRate;
    }

    private long randomDelay() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long max = rnd.nextDouble() < slowChance ? maxDelayMs * 3 : maxDelayMs;
        return minDelayMs >= max ? minDelayMs : rnd.nextLong(minDelayMs, max + 1);
    }

    @PostMapping("/transcribe")
    public HttpTranscriptionClient.Response transcribe(@RequestParam("file") MultipartFile file) throws InterruptedException {
        long delay = randomDelay();
        if (delay > 0) Thread.sleep(delay);
        if (failureRate > 0 && ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Mock transcription failure");
        }
        long seconds = Math.max(1, file.getSize() / 16_000);
        String text = """
                [MOCK TRANSCRIPT] %s (%s, ~%ds)

                [00:00] Speaker 1: Good morning, thanks for joining. This call is being recorded for transcription purposes.
                [00:07] Speaker 2: No problem. I have the case notes open, so we can go through the details whenever you are ready.
                [00:15] Speaker 1: Great. Can you start by confirming the sequence of events as you remember them?
                [00:24] Speaker 2: Yes. It was just after nine when I first noticed the issue, and I reported it to my supervisor straight away.
                [00:36] Speaker 1: And did anyone else witness that? [inaudible]
                [00:42] Speaker 2: Two colleagues were nearby, I can provide their names after the call.
                [00:50] Speaker 1: That would be helpful. Let's move on to the follow-up actions.
                """.formatted(file.getOriginalFilename(), file.getContentType(), seconds);
        return new HttpTranscriptionClient.Response(text);
    }
}
