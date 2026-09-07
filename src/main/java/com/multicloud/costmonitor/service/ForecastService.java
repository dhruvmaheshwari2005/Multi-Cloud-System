package com.multicloud.costmonitor.service;

import com.multicloud.costmonitor.repository.CostRecordRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
public class ForecastService {

    private final CostRecordRepository costRecordRepository;

    public ForecastService(CostRecordRepository costRecordRepository) {
        this.costRecordRepository = costRecordRepository;
    }

    /**
     * Projects cost for the next 30 days based on the last 30 days of data.
     */
    public List<Map<String, Object>> generateForecast(Long accountId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);

        List<Object[]> historicalRaw;
        if (accountId != null) {
            historicalRaw = costRecordRepository.sumCostByDayAndAccount(accountId, start, end);
        } else {
            historicalRaw = costRecordRepository.sumCostByDay(start, end);
        }

        List<Map<String, Object>> forecastPoints = new ArrayList<>();
        if (historicalRaw.size() < 2) {
            // Not enough points to run regression, return empty or static forecast
            return forecastPoints;
        }

        int n = historicalRaw.size();
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = (Double) historicalRaw.get(i)[1];
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        // Linear Regression parameters: y = m * x + c
        double denominator = (n * sumX2 - sumX * sumX);
        double m = (denominator != 0) ? (n * sumXY - sumX * sumY) / denominator : 0;
        double c = (sumY - m * sumX) / n;

        // Predict the next 30 days
        LocalDate lastHistDate = (LocalDate) historicalRaw.get(n - 1)[0];
        for (int i = 1; i <= 30; i++) {
            LocalDate forecastDate = lastHistDate.plusDays(i);
            double predictedVal = m * (n - 1 + i) + c;
            
            // Avoid negative forecasted costs
            if (predictedVal < 0) {
                predictedVal = 0;
            }
            
            predictedVal = Math.round(predictedVal * 100.0) / 100.0;

            Map<String, Object> point = new HashMap<>();
            point.put("date", forecastDate.toString());
            point.put("cost", predictedVal);
            forecastPoints.add(point);
        }

        return forecastPoints;
    }
}
