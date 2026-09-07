package com.multicloud.costmonitor.controller;

import com.multicloud.costmonitor.repository.CloudAccountRepository;
import com.multicloud.costmonitor.repository.CostRecordRepository;
import com.multicloud.costmonitor.repository.BudgetAlertRepository;
import com.multicloud.costmonitor.model.CostRecord;
import com.multicloud.costmonitor.service.ForecastService;
import com.multicloud.costmonitor.service.AnomalyDetectionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final CostRecordRepository costRecordRepository;
    private final CloudAccountRepository cloudAccountRepository;
    private final BudgetAlertRepository budgetAlertRepository;
    private final ForecastService forecastService;
    private final AnomalyDetectionService anomalyDetectionService;

    public DashboardController(CostRecordRepository costRecordRepository,
                               CloudAccountRepository cloudAccountRepository,
                               BudgetAlertRepository budgetAlertRepository,
                               ForecastService forecastService,
                               AnomalyDetectionService anomalyDetectionService) {
        this.costRecordRepository = costRecordRepository;
        this.cloudAccountRepository = cloudAccountRepository;
        this.budgetAlertRepository = budgetAlertRepository;
        this.forecastService = forecastService;
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        LocalDate today = LocalDate.now();
        LocalDate startOfThisMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        
        LocalDate startOfLastMonth = today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endOfLastMonth = today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

        // This Month Spent
        Double thisMonthVal = costRecordRepository.getTotalCost(startOfThisMonth, today);
        double thisMonthSpent = (thisMonthVal != null) ? thisMonthVal : 0.0;

        // Last Month Spent
        Double lastMonthVal = costRecordRepository.getTotalCost(startOfLastMonth, endOfLastMonth);
        double lastMonthSpent = (lastMonthVal != null) ? lastMonthVal : 0.0;

        // Change percentage
        double changePct = 0.0;
        if (lastMonthSpent > 0) {
            changePct = ((thisMonthSpent - lastMonthSpent) / lastMonthSpent) * 100.0;
        }

        // Forecasted Spend (Linear Extrapolation)
        int dayOfMonth = today.getDayOfMonth();
        int lengthOfMonth = today.lengthOfMonth();
        double dailyAvg = thisMonthSpent / Math.max(dayOfMonth, 1);
        double forecastedSpend = dailyAvg * lengthOfMonth;

        // Counts
        long activeAccounts = cloudAccountRepository.count();
        long unreadAlerts = budgetAlertRepository.findByIsReadFalseOrderByTriggeredAtDesc().size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("thisMonthSpent", Math.round(thisMonthSpent * 100.0) / 100.0);
        stats.put("lastMonthSpent", Math.round(lastMonthSpent * 100.0) / 100.0);
        stats.put("changePercentage", Math.round(changePct * 100.0) / 100.0);
        stats.put("forecastedSpend", Math.round(forecastedSpend * 100.0) / 100.0);
        stats.put("activeAccountsCount", activeAccounts);
        stats.put("unreadAlertsCount", unreadAlerts);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/charts")
    public ResponseEntity<?> getChartData(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30); // Default to last 30 days
        }

        // 1. Daily Trend
        List<Object[]> dailyRaw;
        if (accountId != null) {
            dailyRaw = costRecordRepository.sumCostByDayAndAccount(accountId, startDate, endDate);
        } else {
            dailyRaw = costRecordRepository.sumCostByDay(startDate, endDate);
        }

        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (Object[] row : dailyRaw) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", row[0].toString());
            item.put("cost", Math.round((Double) row[1] * 100.0) / 100.0);
            dailyTrend.add(item);
        }

        // 2. Provider Breakdown
        List<Object[]> providerRaw = costRecordRepository.sumCostByProvider(startDate, endDate);
        List<Map<String, Object>> byProvider = new ArrayList<>();
        for (Object[] row : providerRaw) {
            Map<String, Object> item = new HashMap<>();
            item.put("provider", row[0]);
            item.put("cost", Math.round((Double) row[1] * 100.0) / 100.0);
            byProvider.add(item);
        }

        // 3. Service Breakdown
        List<Object[]> serviceRaw = costRecordRepository.sumCostByServiceName(startDate, endDate);
        List<Map<String, Object>> byService = new ArrayList<>();
        for (Object[] row : serviceRaw) {
            Map<String, Object> item = new HashMap<>();
            item.put("service", row[0]);
            item.put("cost", Math.round((Double) row[1] * 100.0) / 100.0);
            byService.add(item);
        }

        // 4. Resource Type Breakdown
        List<Object[]> typeRaw = costRecordRepository.sumCostByResourceType(startDate, endDate);
        List<Map<String, Object>> byResourceType = new ArrayList<>();
        for (Object[] row : typeRaw) {
            Map<String, Object> item = new HashMap<>();
            item.put("resourceType", row[0]);
            item.put("cost", Math.round((Double) row[1] * 100.0) / 100.0);
            byResourceType.add(item);
        }

        Map<String, Object> charts = new HashMap<>();
        charts.put("dailyTrend", dailyTrend);
        charts.put("byProvider", byProvider);
        charts.put("byService", byService);
        charts.put("byResourceType", byResourceType);

        return ResponseEntity.ok(charts);
    }

    @GetMapping("/forecast")
    public ResponseEntity<?> getForecast(@RequestParam(required = false) Long accountId) {
        return ResponseEntity.ok(forecastService.generateForecast(accountId));
    }

    @GetMapping("/anomalies")
    public ResponseEntity<?> getAnomalies(@RequestParam(required = false) Long accountId) {
        return ResponseEntity.ok(anomalyDetectionService.detectAnomalies(accountId));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(180);
        }

        List<CostRecord> records;
        if (accountId != null) {
            records = costRecordRepository.findByCloudAccountIdAndRecordDateBetween(accountId, startDate, endDate);
        } else {
            records = costRecordRepository.findByRecordDateBetween(startDate, endDate);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Record ID,Date,Cloud Provider,Account Name,Resource Type,Service Name,Usage Quantity,Usage Unit,Cost (USD)\n");

        for (CostRecord r : records) {
            csv.append(String.format("%d,%s,%s,%s,%s,%s,%.2f,%s,%.2f\n",
                    r.getId(),
                    r.getRecordDate().toString(),
                    r.getCloudAccount().getProvider(),
                    r.getCloudAccount().getName().replace(",", " "),
                    r.getResourceType(),
                    r.getServiceName().replace(",", " "),
                    r.getUsageQuantity(),
                    r.getUsageUnit(),
                    r.getCostAmount()
            ));
        }

        byte[] output = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=billing_ledger.csv")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                .body(output);
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getMonthlyReports() {
        List<Map<String, Object>> reports = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 6; i++) {
            LocalDate monthDate = today.minusMonths(i);
            LocalDate start = monthDate.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate end = monthDate.with(TemporalAdjusters.lastDayOfMonth());
            if (end.isAfter(today)) {
                end = today;
            }

            Double awsCostVal = costRecordRepository.getTotalCostByProvider("AWS", start, end);
            Double azureCostVal = costRecordRepository.getTotalCostByProvider("AZURE", start, end);
            Double ociCostVal = costRecordRepository.getTotalCostByProvider("OCI", start, end);

            double awsCost = awsCostVal != null ? awsCostVal : 0.0;
            double azureCost = azureCostVal != null ? azureCostVal : 0.0;
            double ociCost = ociCostVal != null ? ociCostVal : 0.0;
            double totalCost = awsCost + azureCost + ociCost;

            long accountCount = cloudAccountRepository.count();

            int daysInPeriod = (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            double avgDaily = totalCost / Math.max(daysInPeriod, 1);

            Map<String, Object> report = new HashMap<>();
            // Format month as e.g. "July 2026"
            String monthName = start.getMonth().name();
            monthName = monthName.charAt(0) + monthName.substring(1).toLowerCase();
            report.put("month", monthName + " " + start.getYear());
            report.put("awsCost", Math.round(awsCost * 100.0) / 100.0);
            report.put("azureCost", Math.round(azureCost * 100.0) / 100.0);
            report.put("ociCost", Math.round(ociCost * 100.0) / 100.0);
            report.put("totalCost", Math.round(totalCost * 100.0) / 100.0);
            report.put("activeAccountsCount", accountCount);
            report.put("avgDailyCost", Math.round(avgDaily * 100.0) / 100.0);

            reports.add(report);
        }
        return ResponseEntity.ok(reports);
    }
}
