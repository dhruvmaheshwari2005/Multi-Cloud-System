package com.multicloud.costmonitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "cloud_resources")
public class CloudResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String resourceId; // e.g. i-0792348abcdef, arn:aws:s3:::prod-bucket

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String resourceType; // e.g. Compute, Storage, Database, Network

    private String sizeType; // e.g. t3.medium, Standard_D2_v3

    private String region; // e.g. us-east-1, eastus

    @Column(nullable = false)
    private String status = "RUNNING"; // RUNNING, STOPPED, TERMINATED

    private double dailyCost; // USD daily run rate

    private String tags; // e.g. "Env=Prod,Owner=FinOps"

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cloud_account_id", nullable = false)
    private CloudAccount cloudAccount;

    public CloudResource() {}

    public CloudResource(String resourceId, String name, String resourceType, String sizeType, String region, String status, double dailyCost, String tags, CloudAccount cloudAccount) {
        this.resourceId = resourceId;
        this.name = name;
        this.resourceType = resourceType;
        this.sizeType = sizeType;
        this.region = region;
        this.status = status;
        this.dailyCost = dailyCost;
        this.tags = tags;
        this.cloudAccount = cloudAccount;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getSizeType() { return sizeType; }
    public void setSizeType(String sizeType) { this.sizeType = sizeType; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getDailyCost() { return dailyCost; }
    public void setDailyCost(double dailyCost) { this.dailyCost = dailyCost; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public CloudAccount getCloudAccount() { return cloudAccount; }
    public void setCloudAccount(CloudAccount cloudAccount) { this.cloudAccount = cloudAccount; }
}
