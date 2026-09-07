package com.multicloud.costmonitor.controller;

import com.multicloud.costmonitor.repository.CostRecordRepository;
import com.multicloud.costmonitor.service.SseBroadcasterService;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/dashboard")
@EnableScheduling
public class RealtimeCostController {

    private final CostRecordRepository costRecordRepository;
    private final SseBroadcasterService sseBroadcasterService;
    private final Random random = new Random();
    private double accumulatedIncrement = 0.0;

    public RealtimeCostController(CostRecordRepository costRecordRepository,
                                  SseBroadcasterService sseBroadcasterService) {
        this.costRecordRepository = costRecordRepository;
        this.sseBroadcasterService = sseBroadcasterService;
    }

    @GetMapping(value = "/realtime-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRealtimeCosts() {
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes timeout
        sseBroadcasterService.addEmitter(emitter);
        
        try {
            emitter.send(SseEmitter.event().name("connection").data("Connected to Maheshwari Cloud Live Stream"));
        } catch (IOException e) {
            // silent fail on initial write failure
        }
        return emitter;
    }

    @Scheduled(fixedRate = 3000)
    public void broadcastLiveBilling() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        
        Double initialSpentVal = costRecordRepository.getTotalCost(startOfMonth, today);
        double baseSpent = (initialSpentVal != null) ? initialSpentVal : 3950.40;

        double increment = 0.02 + (0.07 * random.nextDouble());
        accumulatedIncrement += increment;
        
        double liveMonthlySpent = Math.round((baseSpent + accumulatedIncrement) * 100.0) / 100.0;
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("liveMonthlySpent", liveMonthlySpent);
        payload.put("increment", Math.round(increment * 100.0) / 100.0);
        payload.put("timestamp", System.currentTimeMillis());
        
        payload.put("awsLoad", 45 + random.nextInt(15));
        payload.put("azureLoad", 60 + random.nextInt(20));
        payload.put("ociLoad", 30 + random.nextInt(10));

        sseBroadcasterService.broadcast("cost-update", payload);
    }
}
