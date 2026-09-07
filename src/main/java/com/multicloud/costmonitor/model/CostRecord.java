package com.multicloud.costmonitor.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "cost_records")
public class CostRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloud_account_id", nullable = false)
    private CloudAccount cloudAccount;

    @Column(nullable = false)
    private String resourceType; // e.g. Compute, Storage, Database, Network

    @Column(nullable = false)
    private String serviceName; // e.g. Amazon EC2, Azure Blob Storage, Autonomous DB

    @Column(nullable = false)
    private double costAmount; // In USD

    private double usageQuantity;

    private String usageUnit; // e.g. Hrs, GB-Month, Requests

    @Column(nullable = false)
    private LocalDate recordDate;

    public CostRecord() {}

    public CostRecord(CloudAccount cloudAccount, String resourceType, String serviceName, double costAmount, double usageQuantity, String usageUnit, LocalDate recordDate) {
        this.cloudAccount = cloudAccount;
        this.resourceType = resourceType;
        this.serviceName = serviceName;
        this.costAmount = costAmount;
        this.usageQuantity = usageQuantity;
        this.usageUnit = usageUnit;
        this.recordDate = recordDate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CloudAccount getCloudAccount() { return cloudAccount; }
    public void setCloudAccount(CloudAccount cloudAccount) { this.cloudAccount = cloudAccount; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public double getCostAmount() { return costAmount; }
    public void setCostAmount(double costAmount) { this.costAmount = costAmount; }

    public double getUsageQuantity() { return usageQuantity; }
    public void setUsageQuantity(double usageQuantity) { this.usageQuantity = usageQuantity; }

    public String getUsageUnit() { return usageUnit; }
    public void setUsageUnit(String usageUnit) { this.usageUnit = usageUnit; }

    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
}
