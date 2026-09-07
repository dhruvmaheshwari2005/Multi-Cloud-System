package com.multicloud.costmonitor.service;

import com.multicloud.costmonitor.model.Budget;
import com.multicloud.costmonitor.model.CloudAccount;
import com.multicloud.costmonitor.model.User;
import com.multicloud.costmonitor.repository.BudgetRepository;
import com.multicloud.costmonitor.repository.CloudAccountRepository;
import com.multicloud.costmonitor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final UserRepository userRepository;
    private final CloudAccountRepository cloudAccountRepository;
    private final BudgetRepository budgetRepository;
    private final CloudBillingService cloudBillingService;
    private final SyncScheduler syncScheduler;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UserRepository userRepository,
                          CloudAccountRepository cloudAccountRepository,
                          BudgetRepository budgetRepository,
                          CloudBillingService cloudBillingService,
                          SyncScheduler syncScheduler,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.cloudAccountRepository = cloudAccountRepository;
        this.budgetRepository = budgetRepository;
        this.cloudBillingService = cloudBillingService;
        this.syncScheduler = syncScheduler;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing Maheshwari Cloud Database Seeder...");

        // 1. Create Default Demo User
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User("admin", passwordEncoder.encode("password123"), "admin@maheshwaricloud.com");
            userRepository.save(admin);
            logger.info("Default administrator created: admin / password123");
        }

        // 2. Create Default Cloud Accounts
        if (cloudAccountRepository.count() == 0) {
            CloudAccount aws = new CloudAccount("AWS", "AWS Production Hub", "123456789012", "AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", "us-east-1");
            aws.setStatus("CONNECTED");
            aws.setLastSyncTime(LocalDateTime.now());
            cloudAccountRepository.save(aws);
            cloudBillingService.syncHistoricalData(aws, 90);

            CloudAccount azure = new CloudAccount("AZURE", "Azure Testing Cluster", "azure-sub-1122", "azure-client-3344", "azure-secret-5566", "azure-tenant-7788");
            azure.setStatus("CONNECTED");
            azure.setLastSyncTime(LocalDateTime.now());
            cloudAccountRepository.save(azure);
            cloudBillingService.syncHistoricalData(azure, 90);

            CloudAccount oci = new CloudAccount("OCI", "OCI Enterprise Core", "ocid1.tenancy.oc1..aaaaaaaaxxxxxx", "ocid1.user.oc1..aaaaaaaaxxxxxx", "PEM_KEY_EXAMPLE", "us-ashburn-1");
            oci.setStatus("CONNECTED");
            oci.setLastSyncTime(LocalDateTime.now());
            cloudAccountRepository.save(oci);
            cloudBillingService.syncHistoricalData(oci, 90);

            logger.info("Default cloud accounts integrated and synchronized.");

            // 3. Create Default Budgets
            Budget globalBudget = new Budget("Global Monthly Limit", 4000.0, 50.0, true, null);
            budgetRepository.save(globalBudget);

            Budget awsBudget = new Budget("AWS Compute Cap", 3000.0, 80.0, true, aws);
            budgetRepository.save(awsBudget);

            logger.info("Default budgets set.");

            // 4. Evaluate Budgets and Alerts
            syncScheduler.evaluateBudgets();
            logger.info("Initial budget thresholds evaluated.");
        }
    }
}
