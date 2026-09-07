package com.multicloud.costmonitor.service;

import com.multicloud.costmonitor.repository.CostRecordRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
public class AnomalyDetectionService {

    private final CostRecordRepository costRecordRepository;

    public AnomalyDetectionService(CostRecordRepository costRecordRepository) {
        this.costRecordRepository = costRecordRepository;
    }

    /**
     * Identifies spending spikes in the last 90 days that deviate statistically from the mean.
     */
    public List<Map<String, Object>> detectAnomalies(Long accountId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(90);

        List<Object[]> dailyData;
        if (accountId != null) {
            dailyData = costRecordRepository.sumCostByDayAndAccount(accountId, start, end);
        } else {
            dailyData = costRecordRepository.sumCostByDay(start, end);
        }

        List<Map<String, Object>> anomalies = new ArrayList<>();
        int size = dailyData.size();
        if (size < 10) {
            // Need a minimal baseline of days to calculate variance
            return anomalies;
        }

        // 1. Calculate Mean
        double sum = 0;
        for (Object[] row : dailyData) {
            sum += (Double) row[1];
        }
        double mean = sum / size;

        // 2. Calculate Standard Deviation (StdDev)
        double varianceSum = 0;
        for (Object[] row : dailyData) {
            double cost = (Double) row[1];
            varianceSum += Math.pow(cost - mean, 2);
        }
        double stdDev = Math.sqrt(varianceSum / size);

        if (stdDev == 0) {
            return anomalies; // All days have exact same cost
        }

        // 3. Scan for spikes (Z-Score >= 1.8)
        for (Object[] row : dailyData) {
            LocalDate date = (LocalDate) row[0];
            double cost = (Double) row[1];

            // Z-score calculation
            double zScore = (cost - mean) / stdDev;

            // Only flag spikes, ignore cost drops
            if (zScore >= 1.7) {
                Map<String, Object> anomaly = new HashMap<>();
                anomaly.put("date", date.toString());
                anomaly.put("cost", Math.round(cost * 100.0) / 100.0);
                anomaly.put("expectedCost", Math.round(mean * 100.0) / 100.0);
                
                double deviationPct = ((cost - mean) / mean) * 100.0;
                anomaly.put("deviationPercentage", Math.round(deviationPct * 100.0) / 100.0);
                
                String severity = (zScore >= 2.3) ? "CRITICAL" : "WARNING";
                anomaly.put("severity", severity);

                String desc = String.format("Spend surged to $%s which is %s%% above your baseline average of $%s.",
                        Math.round(cost * 100.0) / 100.0,
                        Math.round(deviationPct),
                        Math.round(mean * 100.0) / 100.0);
                anomaly.put("description", desc);

                anomalies.add(anomaly);
            }
        }

        // Sort anomalies to return most recent first
        anomalies.sort((a, b) -> b.get("date").toString().compareTo(a.get("date").toString()));

        return anomalies;
    }
}
