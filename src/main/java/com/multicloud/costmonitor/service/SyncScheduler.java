package com.multicloud.costmonitor.service;

import com.multicloud.costmonitor.model.Budget;
import com.multicloud.costmonitor.model.BudgetAlert;
import com.multicloud.costmonitor.model.CloudAccount;
import com.multicloud.costmonitor.model.User;
import com.multicloud.costmonitor.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class SyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SyncScheduler.class);

    private final CloudAccountRepository cloudAccountRepository;
    private final CostRecordRepository costRecordRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetAlertRepository budgetAlertRepository;
    private final UserRepository userRepository;
    private final CloudBillingService cloudBillingService;
    private final EmailService emailService;

    public SyncScheduler(CloudAccountRepository cloudAccountRepository,
                         CostRecordRepository costRecordRepository,
                         BudgetRepository budgetRepository,
                         BudgetAlertRepository budgetAlertRepository,
                         UserRepository userRepository,
                         CloudBillingService cloudBillingService,
                         EmailService emailService) {
        this.cloudAccountRepository = cloudAccountRepository;
        this.costRecordRepository = costRecordRepository;
        this.budgetRepository = budgetRepository;
        this.budgetAlertRepository = budgetAlertRepository;
        this.userRepository = userRepository;
        this.cloudBillingService = cloudBillingService;
        this.emailService = emailService;
    }

    /**
     * Simulated scheduled cost sync engine. Runs every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void runScheduledSync() {
        logger.info("Initializing scheduled multi-cloud cost aggregation...");
        List<CloudAccount> accounts = cloudAccountRepository.findAll();
        for (CloudAccount account : accounts) {
            try {
                account.setStatus("SYNCING");
                cloudAccountRepository.save(account);

                cloudBillingService.syncCurrentDayData(account);

                account.setStatus("CONNECTED");
                account.setLastSyncTime(LocalDateTime.now());
                cloudAccountRepository.save(account);
                logger.info("Sync success: {}", account.getName());
            } catch (Exception e) {
                account.setStatus("FAILED");
                cloudAccountRepository.save(account);
                logger.error("Sync failed for cloud account {}: {}", account.getName(), e.getMessage());
            }
        }

        evaluateBudgets();
    }

    /**
     * Evaluates all budgets against actual spending for the current month.
     */
    @Transactional
    public void evaluateBudgets() {
        logger.info("Evaluating active budgets...");
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());

        List<Budget> budgets = budgetRepository.findAll();
        List<User> users = userRepository.findAll();

        for (Budget budget : budgets) {
            double currentSpent = 0.0;
            if (budget.getCloudAccount() != null) {
                Double spent = costRecordRepository.getTotalCostByAccount(budget.getCloudAccount().getId(), startOfMonth, today);
                currentSpent = (spent != null) ? spent : 0.0;
            } else {
                Double spent = costRecordRepository.getTotalCost(startOfMonth, today);
                currentSpent = (spent != null) ? spent : 0.0;
            }

            currentSpent = Math.round(currentSpent * 100.0) / 100.0;
            budget.setCurrentSpent(currentSpent);
            budgetRepository.save(budget);

            double thresholdAmount = budget.getLimitAmount() * (budget.getThresholdPercentage() / 100.0);

            if (currentSpent >= thresholdAmount) {
                boolean alreadyAlerted = hasAlertedThisMonth(budget, startOfMonth);

                if (!alreadyAlerted) {
                    BudgetAlert alert = new BudgetAlert(budget, currentSpent, budget.getLimitAmount(), budget.getThresholdPercentage());
                    budgetAlertRepository.save(alert);
                    logger.warn("🚨 Budget Alert triggered: '{}' has spent ${} of limit ${} (Threshold {}%)",
                            budget.getName(), currentSpent, budget.getLimitAmount(), budget.getThresholdPercentage());

                    if (budget.isEmailNotification()) {
                        for (User user : users) {
                            emailService.sendBudgetAlertEmail(user.getEmail(), budget.getName(), currentSpent, budget.getLimitAmount(), budget.getThresholdPercentage());
                        }
                    }
                }
            }
        }
    }

    private boolean hasAlertedThisMonth(Budget budget, LocalDate startOfMonth) {
        List<BudgetAlert> alerts = budgetAlertRepository.findByOrderByTriggeredAtDesc();
        LocalDateTime firstOfThisMonth = startOfMonth.atStartOfDay();

        return alerts.stream()
                .filter(a -> a.getBudget().getId().equals(budget.getId()))
                .anyMatch(a -> a.getTriggeredAt().isAfter(firstOfThisMonth));
    }
}
