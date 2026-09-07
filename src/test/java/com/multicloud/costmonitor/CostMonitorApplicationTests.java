package com.multicloud.costmonitor;

import com.multicloud.costmonitor.model.Budget;
import com.multicloud.costmonitor.model.BudgetAlert;
import com.multicloud.costmonitor.model.CloudAccount;
import com.multicloud.costmonitor.model.CostRecord;
import com.multicloud.costmonitor.model.User;
import com.multicloud.costmonitor.repository.*;
import com.multicloud.costmonitor.service.CloudBillingService;
import com.multicloud.costmonitor.service.SyncScheduler;
import com.multicloud.costmonitor.service.ForecastService;
import com.multicloud.costmonitor.service.AnomalyDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import com.multicloud.costmonitor.controller.DashboardController;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CostMonitorApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CloudAccountRepository cloudAccountRepository;

    @Autowired
    private CostRecordRepository costRecordRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private BudgetAlertRepository budgetAlertRepository;

    @Autowired
    private CloudBillingService cloudBillingService;

    @Autowired
    private SyncScheduler syncScheduler;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DashboardController dashboardController;

    @Autowired
    private CloudResourceRepository cloudResourceRepository;

    @Autowired
    private ForecastService forecastService;

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    @BeforeEach
    public void setup() {
        budgetAlertRepository.deleteAll();
        budgetRepository.deleteAll();
        cloudResourceRepository.deleteAll();
        costRecordRepository.deleteAll();
        cloudAccountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void contextLoads() {
        assertNotNull(userRepository);
        assertNotNull(cloudAccountRepository);
        assertNotNull(costRecordRepository);
        assertNotNull(budgetRepository);
        assertNotNull(budgetAlertRepository);
    }

    @Test
    public void testUserCreationAndPasswordEncoding() {
        User user = new User("testAdminUser", "admin123", "admin@org.com");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertTrue(passwordEncoder.matches("admin123", saved.getPassword()));
    }

    @Test
    public void testMockBillingSyncAndAggregations() {
        CloudAccount account = new CloudAccount("AWS", "Test AWS Account", "123456789012", "KEY", "SECRET", "us-east-1");
        CloudAccount savedAccount = cloudAccountRepository.save(account);

        cloudBillingService.syncHistoricalData(savedAccount, 30);
        
        long count = costRecordRepository.count();
        assertTrue(count > 0, "Cost records should have been seeded");

        Double totalCost = costRecordRepository.getTotalCost(LocalDate.now().minusDays(30), LocalDate.now());
        assertNotNull(totalCost);
        assertTrue(totalCost > 0);
    }

    @Test
    public void testBudgetEvaluationAndAlerting() {
        User user = new User("alertReceiver", "pass123", "receiver@org.com");
        userRepository.save(user);

        CloudAccount account = new CloudAccount("Azure", "Test Azure Account", "azure-sub-id", "KEY", "SECRET", "tenant");
        CloudAccount savedAccount = cloudAccountRepository.save(account);
        cloudBillingService.syncHistoricalData(savedAccount, 30);

        Budget budget = new Budget("Small Budget", 10.0, 50.0, true, savedAccount);
        budgetRepository.save(budget);

        syncScheduler.evaluateBudgets();

        List<Budget> budgets = budgetRepository.findAll();
        assertFalse(budgets.isEmpty());
        Budget updatedBudget = budgets.get(0);
        assertTrue(updatedBudget.getCurrentSpent() > 0);

        List<BudgetAlert> alerts = budgetAlertRepository.findAll();
        assertFalse(alerts.isEmpty(), "Budget alert should have triggered");
        BudgetAlert alert = alerts.get(0);
        assertEquals("Small Budget", alert.getBudget().getName());
        assertTrue(alert.getSpentAmount() >= 5.0);
    }

    @Test
    public void testCloudResourcesInventory() {
        CloudAccount account = new CloudAccount("AWS", "AWS Inventory Test", "123456", "KEY", "SECRET", "us-east-1");
        CloudAccount savedAccount = cloudAccountRepository.save(account);

        // syncHistoricalData seeds resources automatically
        cloudBillingService.syncHistoricalData(savedAccount, 30);

        List<com.multicloud.costmonitor.model.CloudResource> resources = cloudResourceRepository.findByCloudAccountId(savedAccount.getId());
        assertFalse(resources.isEmpty());
        assertEquals(4, resources.size(), "AWS should seed exactly 4 resource items");

        com.multicloud.costmonitor.model.CloudResource vm = resources.stream().filter(r -> r.getResourceType().equals("Compute")).findFirst().orElse(null);
        assertNotNull(vm);
        assertEquals("web-prod-01", vm.getName());
        assertEquals("RUNNING", vm.getStatus());
        assertTrue(vm.getTags().contains("Env=Prod"));
    }

    @Test
    public void testForecastingRegression() {
        CloudAccount account = new CloudAccount("AWS", "AWS Forecast Test", "123456", "KEY", "SECRET", "us-east-1");
        CloudAccount savedAccount = cloudAccountRepository.save(account);
        cloudBillingService.syncHistoricalData(savedAccount, 30);

        List<Map<String, Object>> forecast = forecastService.generateForecast(savedAccount.getId());
        assertFalse(forecast.isEmpty());
        assertEquals(30, forecast.size(), "Should forecast exactly 30 days of data");
        
        Map<String, Object> firstPoint = forecast.get(0);
        assertNotNull(firstPoint.get("date"));
        assertNotNull(firstPoint.get("cost"));
    }

    @Test
    public void testAnomalyDetection() {
        CloudAccount account = new CloudAccount("AWS", "AWS Anomaly Test", "123456", "KEY", "SECRET", "us-east-1");
        CloudAccount savedAccount = cloudAccountRepository.save(account);
        
        // Seed standard historical data
        cloudBillingService.syncHistoricalData(savedAccount, 60);

        // Inject an artificial massive cost spike record to trigger the Z-score detection
        LocalDate spikeDate = LocalDate.now().minusDays(5);
        CostRecord spike = new CostRecord(savedAccount, "Compute", "Amazon EC2", 850.0, 720.0, "Hrs", spikeDate);
        costRecordRepository.save(spike);

         List<Map<String, Object>> anomalies = anomalyDetectionService.detectAnomalies(savedAccount.getId());
        assertFalse(anomalies.isEmpty(), "Statistical anomaly should have been detected");
        
        Map<String, Object> anomaly = anomalies.get(0);
        assertEquals(spikeDate.toString(), anomaly.get("date"));
        assertEquals("CRITICAL", anomaly.get("severity"));
        assertTrue(Double.parseDouble(anomaly.get("cost").toString()) >= 850.0);
    }

    @Test
    public void testProfileUpdate() {
        User user = new User("originalUser", passwordEncoder.encode("secret"), "orig@org.com");
        userRepository.save(user);

        org.springframework.security.core.context.SecurityContext context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken token =
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("originalUser", "secret", java.util.Collections.emptyList());
        context.setAuthentication(token);
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);

        user.setUsername("updatedUser");
        user.setEmail("updated@org.com");
        userRepository.save(user);

        User updated = userRepository.findByUsername("updatedUser").orElse(null);
        assertNotNull(updated);
        assertEquals("updated@org.com", updated.getEmail());
    }

    @Test
    public void testManualEmailAlertTrigger() {
        User user = new User("alertReceiver", passwordEncoder.encode("pass"), "receiver@org.com");
        userRepository.save(user);

        CloudAccount account = new CloudAccount("AWS", "Test Account", "123456", "KEY", "SECRET", "us-east-1");
        cloudAccountRepository.save(account);

        Budget budget = new Budget("Alert Trigger Test", 1000.0, 50.0, true, account);
        budget.setCurrentSpent(750.0);
        budgetRepository.save(budget);

        BudgetAlert alert = new BudgetAlert(budget, budget.getCurrentSpent(), budget.getLimitAmount(), budget.getThresholdPercentage());
        budgetAlertRepository.save(alert);

        List<BudgetAlert> alerts = budgetAlertRepository.findAll();
        assertFalse(alerts.isEmpty());
        assertEquals("Alert Trigger Test", alerts.get(0).getBudget().getName());
    }

    @Test
    public void testMonthlyReportsEndpoint() {
        CloudAccount account = new CloudAccount("AWS", "Reports Test", "123456", "KEY", "SECRET", "us-east-1");
        cloudAccountRepository.save(account);
        cloudBillingService.syncHistoricalData(account, 30);

        ResponseEntity<?> response = dashboardController.getMonthlyReports();
        assertEquals(200, response.getStatusCode().value());
        
        List<?> reports = (List<?>) response.getBody();
        assertNotNull(reports);
        assertEquals(6, reports.size(), "Should return exactly 6 monthly periods");
    }
}
