package com.multicloud.costmonitor.controller;

import com.multicloud.costmonitor.model.CloudAccount;
import com.multicloud.costmonitor.repository.CloudAccountRepository;
import com.multicloud.costmonitor.repository.CostRecordRepository;
import com.multicloud.costmonitor.repository.BudgetRepository;
import com.multicloud.costmonitor.repository.CloudResourceRepository;
import com.multicloud.costmonitor.service.CloudBillingService;
import com.multicloud.costmonitor.service.SyncScheduler;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class CloudAccountController {

    private final CloudAccountRepository cloudAccountRepository;
    private final CostRecordRepository costRecordRepository;
    private final BudgetRepository budgetRepository;
    private final CloudResourceRepository cloudResourceRepository;
    private final CloudBillingService cloudBillingService;
    private final SyncScheduler syncScheduler;
    private final com.multicloud.costmonitor.service.SseBroadcasterService sseBroadcasterService;

    public CloudAccountController(CloudAccountRepository cloudAccountRepository,
                                  CostRecordRepository costRecordRepository,
                                  BudgetRepository budgetRepository,
                                  CloudResourceRepository cloudResourceRepository,
                                  CloudBillingService cloudBillingService,
                                  SyncScheduler syncScheduler,
                                  com.multicloud.costmonitor.service.SseBroadcasterService sseBroadcasterService) {
        this.cloudAccountRepository = cloudAccountRepository;
        this.costRecordRepository = costRecordRepository;
        this.budgetRepository = budgetRepository;
        this.cloudResourceRepository = cloudResourceRepository;
        this.cloudBillingService = cloudBillingService;
        this.syncScheduler = syncScheduler;
        this.sseBroadcasterService = sseBroadcasterService;
    }

    @GetMapping
    public ResponseEntity<List<CloudAccount>> getAllAccounts() {
        return ResponseEntity.ok(cloudAccountRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> addAccount(@Valid @RequestBody CloudAccount account) {
        account.setStatus("CONNECTING");
        account.setLastSyncTime(LocalDateTime.now());
        CloudAccount savedAccount = cloudAccountRepository.save(account);

        new Thread(() -> {
            try {
                sseBroadcasterService.broadcast("sync-progress", Map.of(
                    "accountId", savedAccount.getId(),
                    "name", savedAccount.getName(),
                    "status", "AUTHENTICATING",
                    "progress", 25
                ));
                Thread.sleep(1500);

                sseBroadcasterService.broadcast("sync-progress", Map.of(
                    "accountId", savedAccount.getId(),
                    "name", savedAccount.getName(),
                    "status", "IMPORTING_BILLING_LEDGER",
                    "progress", 55
                ));
                Thread.sleep(1500);

                cloudBillingService.syncHistoricalData(savedAccount, 180);

                sseBroadcasterService.broadcast("sync-progress", Map.of(
                    "accountId", savedAccount.getId(),
                    "name", savedAccount.getName(),
                    "status", "SYNCING_RESOURCE_INVENTORY",
                    "progress", 85
                ));
                Thread.sleep(1000);

                savedAccount.setStatus("CONNECTED");
                cloudAccountRepository.save(savedAccount);

                syncScheduler.evaluateBudgets();

                sseBroadcasterService.broadcast("sync-progress", Map.of(
                    "accountId", savedAccount.getId(),
                    "name", savedAccount.getName(),
                    "status", "COMPLETED",
                    "progress", 100
                ));
            } catch (Exception e) {
                savedAccount.setStatus("FAILED");
                cloudAccountRepository.save(savedAccount);
                sseBroadcasterService.broadcast("sync-progress", Map.of(
                    "accountId", savedAccount.getId(),
                    "name", savedAccount.getName(),
                    "status", "FAILED",
                    "progress", 0
                ));
            }
        }).start();

        return ResponseEntity.ok(savedAccount);
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<?> syncAccount(@PathVariable Long id) {
        CloudAccount account = cloudAccountRepository.findById(id).orElse(null);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        new Thread(() -> {
            try {
                sseBroadcasterService.broadcast("sync-progress", Map.of(
                    "accountId", account.getId(),
                    "name", account.getName(),
                    "status", "AUTHENTICATING",
                    "progress", 30
                ));
                Thread.sleep(1000);

                sseBroadcasterService.broadcast("sync-progress", Map.of(
                    "accountId", account.getId(),
                    "name", account.getName(),
                    "status", "SYNCING_LATEST_COSTS",
                    "progress", 70
                ));

                cloudBillingService.syncCurrentDayData(account);

                account.setStatus("CONNECTED");
                account.setLastSyncTime(LocalDateTime.now());
                cloudAccountRepository.save(account);

                syncScheduler.evaluateBudgets();

                sseBroadcasterService.broadcast("sync-progress", Map.of(
                    "accountId", account.getId(),
                    "name", account.getName(),
                    "status", "COMPLETED",
                    "progress", 100
                ));
            } catch (Exception e) {
                account.setStatus("FAILED");
                cloudAccountRepository.save(account);
                sseBroadcasterService.broadcast("sync-progress", Map.of(
                    "accountId", account.getId(),
                    "name", account.getName(),
                    "status", "FAILED",
                    "progress", 0
                ));
            }
        }).start();

        return ResponseEntity.ok(account);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteAccount(@PathVariable Long id) {
        if (!cloudAccountRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        costRecordRepository.deleteByCloudAccountId(id);
        budgetRepository.deleteByCloudAccountId(id);
        cloudResourceRepository.deleteByCloudAccountId(id);
        cloudAccountRepository.deleteById(id);

        syncScheduler.evaluateBudgets();

        return ResponseEntity.ok(Map.of("message", "Cloud account and associated historical costs deleted successfully."));
    }
}
