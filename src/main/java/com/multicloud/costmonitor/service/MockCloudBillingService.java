package com.multicloud.costmonitor.service;

import com.multicloud.costmonitor.model.CloudAccount;
import com.multicloud.costmonitor.model.CostRecord;
import com.multicloud.costmonitor.model.CloudResource;
import com.multicloud.costmonitor.repository.CostRecordRepository;
import com.multicloud.costmonitor.repository.CloudResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class MockCloudBillingService implements CloudBillingService {

    private final CostRecordRepository costRecordRepository;
    private final CloudResourceRepository cloudResourceRepository;

    public MockCloudBillingService(CostRecordRepository costRecordRepository, CloudResourceRepository cloudResourceRepository) {
        this.costRecordRepository = costRecordRepository;
        this.cloudResourceRepository = cloudResourceRepository;
    }

    @Override
    @Transactional
    public void syncHistoricalData(CloudAccount account, int days) {
        // Use a stable seed based on account properties to keep generation somewhat repeatable
        long seed = account.getName().hashCode() + account.getProvider().hashCode();
        Random random = new Random(seed);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<CostRecord> records = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if ("AWS".equalsIgnoreCase(account.getProvider())) {
                records.add(createRecord(account, "Compute", "Amazon EC2", 35.0, 70.0, 720.0, "Hrs", date, random));
                records.add(createRecord(account, "Storage", "Amazon S3", 5.0, 15.0, 1500.0, "GB-Month", date, random));
                records.add(createRecord(account, "Database", "Amazon RDS", 20.0, 50.0, 24.0, "Hrs", date, random));
                records.add(createRecord(account, "Network", "Amazon CloudFront", 2.0, 8.0, 800.0, "GB", date, random));
            } else if ("AZURE".equalsIgnoreCase(account.getProvider())) {
                records.add(createRecord(account, "Compute", "Azure Virtual Machines", 30.0, 60.0, 680.0, "Hrs", date, random));
                records.add(createRecord(account, "Storage", "Azure Blob Storage", 4.0, 12.0, 1200.0, "GB-Month", date, random));
                records.add(createRecord(account, "Database", "Azure SQL Database", 15.0, 45.0, 24.0, "Hrs", date, random));
                records.add(createRecord(account, "Network", "Azure ExpressRoute", 3.0, 9.0, 24.0, "Hrs", date, random));
            } else if ("OCI".equalsIgnoreCase(account.getProvider())) {
                records.add(createRecord(account, "Compute", "OCI Compute", 15.0, 35.0, 480.0, "Hrs", date, random));
                records.add(createRecord(account, "Storage", "OCI Object Storage", 2.0, 7.0, 800.0, "GB-Month", date, random));
                records.add(createRecord(account, "Database", "Autonomous Database", 12.0, 30.0, 24.0, "Hrs", date, random));
                records.add(createRecord(account, "Network", "OCI FastConnect", 2.0, 6.0, 24.0, "Hrs", date, random));
            }
        }

        costRecordRepository.saveAll(records);

        // Seed resource inventory items
        List<CloudResource> resources = new ArrayList<>();
        String suffix = account.getId() != null ? String.valueOf(account.getId()) : "temp";
        
        if ("AWS".equalsIgnoreCase(account.getProvider())) {
            resources.add(new CloudResource("i-0792348abcdef-" + suffix, "web-prod-01", "Compute", "t3.medium", "us-east-1", "RUNNING", 0.96, "Env=Prod,Owner=FinOps", account));
            resources.add(new CloudResource("i-0994348abcdef-" + suffix, "dev-sandbox-vm", "Compute", "t3.small", "us-east-1", "STOPPED", 0.48, "Env=Dev,Project=RnD", account));
            resources.add(new CloudResource("arn:aws:s3:::customer-assets-" + suffix, "customer-assets", "Storage", "Standard S3", "us-east-1", "RUNNING", 4.50, "Env=Prod,Tier=Hot", account));
            resources.add(new CloudResource("rds-mysql-master-" + suffix, "mysql-db-primary", "Database", "db.m5.large", "us-east-1", "RUNNING", 6.80, "Env=Prod,App=Orders", account));
        } else if ("AZURE".equalsIgnoreCase(account.getProvider())) {
            resources.add(new CloudResource("/subscriptions/az-sub/vm-app-01-" + suffix, "vm-app-01", "Compute", "Standard_D2_v3", "eastus", "RUNNING", 2.40, "Environment=Staging,Owner=DevOps", account));
            resources.add(new CloudResource("/subscriptions/az-sub/blob-data-" + suffix, "azure-blob-logs", "Storage", "LRS Hot", "eastus", "RUNNING", 1.80, "Environment=Prod,Retention=90days", account));
            resources.add(new CloudResource("/subscriptions/az-sub/sql-db-core-" + suffix, "sql-db-core", "Database", "GP_Gen5_4", "eastus", "RUNNING", 8.50, "Environment=Prod,Dept=Sales", account));
        } else if ("OCI".equalsIgnoreCase(account.getProvider())) {
            resources.add(new CloudResource("ocid1.instance.oc1..compute-01-" + suffix, "oci-node-01", "Compute", "VM.Standard2.1", "us-ashburn-1", "RUNNING", 1.20, "Owner=Infrastructure,Project=Core", account));
            resources.add(new CloudResource("ocid1.bucket.oc1..object-store-" + suffix, "oci-object-store", "Storage", "Standard", "us-ashburn-1", "RUNNING", 0.85, "Usage=Backups", account));
            resources.add(new CloudResource("ocid1.autonomousdb.oc1..db-sys-" + suffix, "oci-autonomous-db", "Database", "ATP.Dedicated", "us-ashburn-1", "RUNNING", 12.00, "Env=Prod,DB=Oracle", account));
        }
        cloudResourceRepository.saveAll(resources);
    }

    @Override
    @Transactional
    public void syncCurrentDayData(CloudAccount account) {
        Random random = new Random();
        LocalDate today = LocalDate.now();

        List<CostRecord> existing = costRecordRepository.findByCloudAccountIdAndRecordDateBetween(account.getId(), today, today);
        if (!existing.isEmpty()) {
            costRecordRepository.deleteAll(existing);
        }

        List<CostRecord> records = new ArrayList<>();
        if ("AWS".equalsIgnoreCase(account.getProvider())) {
            records.add(createRecord(account, "Compute", "Amazon EC2", 35.0, 70.0, 720.0, "Hrs", today, random));
            records.add(createRecord(account, "Storage", "Amazon S3", 5.0, 15.0, 1500.0, "GB-Month", today, random));
            records.add(createRecord(account, "Database", "Amazon RDS", 20.0, 50.0, 24.0, "Hrs", today, random));
            records.add(createRecord(account, "Network", "Amazon CloudFront", 2.0, 8.0, 800.0, "GB", today, random));
        } else if ("AZURE".equalsIgnoreCase(account.getProvider())) {
            records.add(createRecord(account, "Compute", "Azure Virtual Machines", 30.0, 60.0, 680.0, "Hrs", today, random));
            records.add(createRecord(account, "Storage", "Azure Blob Storage", 4.0, 12.0, 1200.0, "GB-Month", today, random));
            records.add(createRecord(account, "Database", "Azure SQL Database", 15.0, 45.0, 24.0, "Hrs", today, random));
            records.add(createRecord(account, "Network", "Azure ExpressRoute", 3.0, 9.0, 24.0, "Hrs", today, random));
        } else if ("OCI".equalsIgnoreCase(account.getProvider())) {
            records.add(createRecord(account, "Compute", "OCI Compute", 15.0, 35.0, 480.0, "Hrs", today, random));
            records.add(createRecord(account, "Storage", "OCI Object Storage", 2.0, 7.0, 800.0, "GB-Month", today, random));
            records.add(createRecord(account, "Database", "Autonomous Database", 12.0, 30.0, 24.0, "Hrs", today, random));
            records.add(createRecord(account, "Network", "OCI FastConnect", 2.0, 6.0, 24.0, "Hrs", today, random));
        }

        costRecordRepository.saveAll(records);
    }

    private CostRecord createRecord(CloudAccount account, String type, String service, double minCost, double maxCost, double baseQty, String unit, LocalDate date, Random random) {
        double cost = minCost + (maxCost - minCost) * random.nextDouble();

        // Natural drops on weekends
        if (date.getDayOfWeek().getValue() >= 6) {
            cost = cost * 0.70;
        }

        // Random anomalies (cost spikes)
        if (random.nextInt(35) == 0) { 
            cost = cost * (1.5 + random.nextDouble() * 1.5);
        }

        double ratio = cost / ((minCost + maxCost) / 2.0);
        double quantity = baseQty * ratio;

        cost = Math.round(cost * 100.0) / 100.0;
        quantity = Math.round(quantity * 100.0) / 100.0;

        return new CostRecord(account, type, service, cost, quantity, unit, date);
    }
}
