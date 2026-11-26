package com.example.transcription.web;

import com.example.transcription.domain.Priority;
import com.example.transcription.domain.Role;
import com.example.transcription.service.CurrentUser;
import com.example.transcription.service.RequestService;
import com.example.transcription.service.RequestViewService;
import com.example.transcription.service.RequestViews.RequestSummary;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Demo helper: creates realistic audio-transcription requests with silent placeholder recordings so features can be shown without manual uploads. */
@RestController
@RequestMapping("/api/demo")
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoController {
    private record Scenario(String title, String dept, List<String> docs) {}

    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario("Witness interviews - Harbour St incident", "Legal", List.of("Interview - J Morgan.wav", "Interview - P Singh.wav", "Officer debrief.wav")),
            new Scenario("Quarterly board meeting recording", "Governance", List.of("Board meeting part 1.wav", "Board meeting part 2.wav", "Board meeting part 3.wav")),
            new Scenario("Insurance claim 4471 - claimant calls", "Claims", List.of("Claim call 2026-11-03.wav", "Loss adjuster voicemail.wav", "Repairer callback.wav")),
            new Scenario("Patient consultations - Ward 7 dictation", "Health", List.of("Dr Patel dictation 1.wav", "Dr Patel dictation 2.wav", "Ward round notes.wav")),
            new Scenario("Supplier negotiation call", "Procurement", List.of("Negotiation call - round 1.wav", "Negotiation call - round 2.wav")),
            new Scenario("Tenancy tribunal hearing audio", "Legal", List.of("Hearing - morning session.wav", "Hearing - afternoon session.wav", "Adjudicator summary.wav", "Tenant statement.wav")),
            new Scenario("Customer complaint calls 2291", "Customer Care", List.of("Complaint call 1.wav", "Complaint call 2.wav")),
            new Scenario("Site safety toolbox talks - Warehouse B", "Operations", List.of("Toolbox talk - Monday.wav", "Toolbox talk - Wednesday.wav", "Incident recount - forklift.wav")));

    private static final List<String> OWNERS = List.of("Dana Whitaker", "Priya Raman", "Tom Becker", "Aroha Ngata", "Sam Okafor", "Elena Novak");
    private static final List<String> JURISDICTIONS = List.of("Auckland", "Wellington", "Canterbury", "Otago");

    private final RequestService requests;
    private final RequestViewService views;
    private int counter;

    public DemoController(RequestService requests, RequestViewService views) {
        this.requests = requests;
        this.views = views;
    }

    /** Creates {@code count} requests for the current user; {@code submit} queues them straight away. */
    @PostMapping("/requests")
    public List<RequestSummary> generate(CurrentUser u, @RequestParam(defaultValue = "3") int count,
                                         @RequestParam(defaultValue = "true") boolean submit) {
        u.require(Role.USER);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<RequestSummary> out = new ArrayList<>();
        for (int i = 0; i < Math.max(1, Math.min(count, 10)); i++) {
            Scenario s = SCENARIOS.get(rnd.nextInt(SCENARIOS.size()));
            int n = nextNumber();
            Map<String, String> custom = new LinkedHashMap<>();
            custom.put("Department", s.dept());
            custom.put("Jurisdiction", JURISDICTIONS.get(rnd.nextInt(JURISDICTIONS.size())));
            var r = requests.create(u, new RequestService.Metadata(
                    s.title(), "CASE-%d-%04d".formatted(LocalDate.now().getYear(), n),
                    OWNERS.get(rnd.nextInt(OWNERS.size())), LocalDate.now().plusDays(rnd.nextInt(1, 15)),
                    pickPriority(rnd.nextDouble()), custom));
            for (String name : s.docs()) {
                requests.addDocument(u, r.id, name, "audio/wav", silentWav(rnd.nextInt(8, 25)));
            }
            out.add(views.summarise(submit ? requests.submit(u, r.id) : requests.get(u, r.id)));
        }
        return out;
    }

    private synchronized int nextNumber() { return 1000 + ++counter + ThreadLocalRandom.current().nextInt(0, 50) * 100; }

    private static Priority pickPriority(double d) {
        return d < 0.15 ? Priority.URGENT : d < 0.45 ? Priority.HIGH : Priority.NORMAL;
    }

    /** A valid, silent 8 kHz mono 8-bit PCM WAV so the audio player in the UI works in demos. */
    static byte[] silentWav(int seconds) {
        int rate = 8000, n = rate * seconds;
        ByteBuffer b = ByteBuffer.allocate(44 + n).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + n).put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII))
                .putInt(16).putShort((short) 1).putShort((short) 1).putInt(rate).putInt(rate).putShort((short) 1).putShort((short) 8)
                .put("data".getBytes(StandardCharsets.US_ASCII)).putInt(n);
        for (int i = 0; i < n; i++) b.put((byte) 128);
        return b.array();
    }
}
