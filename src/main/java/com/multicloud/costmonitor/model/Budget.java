package com.multicloud.costmonitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Positive
    @Column(nullable = false)
    private double limitAmount;

    @Column(nullable = false)
    private double thresholdPercentage = 80.0; // Trigger alert when currentSpent >= limitAmount * (thresholdPercentage / 100)

    @Column(nullable = false)
    private boolean emailNotification = true;

    private double currentSpent = 0.0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cloud_account_id", nullable = true)
    private CloudAccount cloudAccount; // If null, this is a global (all-accounts) budget

    public Budget() {}

    public Budget(String name, double limitAmount, double thresholdPercentage, boolean emailNotification, CloudAccount cloudAccount) {
        this.name = name;
        this.limitAmount = limitAmount;
        this.thresholdPercentage = thresholdPercentage;
        this.emailNotification = emailNotification;
        this.cloudAccount = cloudAccount;
        this.currentSpent = 0.0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLimitAmount() { return limitAmount; }
    public void setLimitAmount(double limitAmount) { this.limitAmount = limitAmount; }

    public double getThresholdPercentage() { return thresholdPercentage; }
    public void setThresholdPercentage(double thresholdPercentage) { this.thresholdPercentage = thresholdPercentage; }

    public boolean isEmailNotification() { return emailNotification; }
    public void setEmailNotification(boolean emailNotification) { this.emailNotification = emailNotification; }

    public double getCurrentSpent() { return currentSpent; }
    public void setCurrentSpent(double currentSpent) { this.currentSpent = currentSpent; }

    public CloudAccount getCloudAccount() { return cloudAccount; }
    public void setCloudAccount(CloudAccount cloudAccount) { this.cloudAccount = cloudAccount; }
}
