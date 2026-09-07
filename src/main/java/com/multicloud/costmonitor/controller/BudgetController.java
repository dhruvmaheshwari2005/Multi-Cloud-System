package com.multicloud.costmonitor.controller;

import com.multicloud.costmonitor.model.Budget;
import com.multicloud.costmonitor.model.BudgetAlert;
import com.multicloud.costmonitor.model.CloudAccount;
import com.multicloud.costmonitor.model.User;
import com.multicloud.costmonitor.repository.BudgetAlertRepository;
import com.multicloud.costmonitor.repository.BudgetRepository;
import com.multicloud.costmonitor.repository.CloudAccountRepository;
import com.multicloud.costmonitor.repository.UserRepository;
import com.multicloud.costmonitor.service.EmailService;
import com.multicloud.costmonitor.service.SyncScheduler;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetRepository budgetRepository;
    private final BudgetAlertRepository budgetAlertRepository;
    private final CloudAccountRepository cloudAccountRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SyncScheduler syncScheduler;

    public BudgetController(BudgetRepository budgetRepository,
                            BudgetAlertRepository budgetAlertRepository,
                            CloudAccountRepository cloudAccountRepository,
                            UserRepository userRepository,
                            EmailService emailService,
                            SyncScheduler syncScheduler) {
        this.budgetRepository = budgetRepository;
        this.budgetAlertRepository = budgetAlertRepository;
        this.cloudAccountRepository = cloudAccountRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.syncScheduler = syncScheduler;
    }

    @GetMapping
    public ResponseEntity<List<Budget>> getAllBudgets() {
        return ResponseEntity.ok(budgetRepository.findAll());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createBudget(@Valid @RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        double limitAmount = Double.parseDouble(payload.get("limitAmount").toString());
        double thresholdPercentage = payload.containsKey("thresholdPercentage") 
                ? Double.parseDouble(payload.get("thresholdPercentage").toString()) 
                : 80.0;
        boolean emailNotification = payload.containsKey("emailNotification")
                ? Boolean.parseBoolean(payload.get("emailNotification").toString())
                : true;

        Long cloudAccountId = null;
        if (payload.containsKey("cloudAccountId") && payload.get("cloudAccountId") != null) {
            String idStr = payload.get("cloudAccountId").toString();
            if (!idStr.trim().isEmpty()) {
                cloudAccountId = Long.parseLong(idStr);
            }
        }

        CloudAccount cloudAccount = null;
        if (cloudAccountId != null) {
            cloudAccount = cloudAccountRepository.findById(cloudAccountId).orElse(null);
            if (cloudAccount == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Error: Cloud account not found"));
            }
        }

        Budget budget = new Budget(name, limitAmount, thresholdPercentage, emailNotification, cloudAccount);
        Budget savedBudget = budgetRepository.save(budget);

        // Run budget check immediately to compute currentSpent
        syncScheduler.evaluateBudgets();

        // Reload to get updated currentSpent
        return ResponseEntity.ok(budgetRepository.findById(savedBudget.getId()).orElse(savedBudget));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteBudget(@PathVariable Long id) {
        if (!budgetRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        budgetRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Budget deleted successfully."));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<BudgetAlert>> getAlerts() {
        return ResponseEntity.ok(budgetAlertRepository.findByOrderByTriggeredAtDesc());
    }

    @GetMapping("/alerts/unread")
    public ResponseEntity<List<BudgetAlert>> getUnreadAlerts() {
        return ResponseEntity.ok(budgetAlertRepository.findByIsReadFalseOrderByTriggeredAtDesc());
    }

    @PostMapping("/alerts/{id}/read")
    @Transactional
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        BudgetAlert alert = budgetAlertRepository.findById(id).orElse(null);
        if (alert == null) {
            return ResponseEntity.notFound().build();
        }
        alert.setRead(true);
        budgetAlertRepository.save(alert);
        return ResponseEntity.ok(Map.of("message", "Alert marked as read."));
    }

    @PostMapping("/alerts/read-all")
    @Transactional
    public ResponseEntity<?> markAllAsRead() {
        List<BudgetAlert> alerts = budgetAlertRepository.findByIsReadFalseOrderByTriggeredAtDesc();
        for (BudgetAlert alert : alerts) {
            alert.setRead(true);
        }
        budgetAlertRepository.saveAll(alerts);
        return ResponseEntity.ok(Map.of("message", "All alerts marked as read."));
    }

    @PostMapping("/{id}/trigger-email")
    @Transactional
    public ResponseEntity<?> triggerBudgetEmail(@PathVariable Long id) {
        Budget budget = budgetRepository.findById(id).orElse(null);
        if (budget == null) {
            return ResponseEntity.notFound().build();
        }

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: No users registered to receive alerts."));
        }

        // Trigger simulation email for all registered users
        for (User user : users) {
            emailService.sendBudgetAlertEmail(
                user.getEmail(),
                budget.getName(),
                budget.getCurrentSpent(),
                budget.getLimitAmount(),
                budget.getThresholdPercentage()
            );
        }

        // Log a persistent alert in database for visual dashboard tracking
        BudgetAlert alert = new BudgetAlert(budget, budget.getCurrentSpent(), budget.getLimitAmount(), budget.getThresholdPercentage());
        budgetAlertRepository.save(alert);

        return ResponseEntity.ok(Map.of("message", "Test budget alert email dispatched successfully!"));
    }
}
