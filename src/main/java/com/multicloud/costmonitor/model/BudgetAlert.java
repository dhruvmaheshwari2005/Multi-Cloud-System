package com.multicloud.costmonitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_alerts")
public class BudgetAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Column(nullable = false)
    private double spentAmount;

    @Column(nullable = false)
    private double limitAmount;

    @Column(nullable = false)
    private double thresholdPercentage;

    @Column(nullable = false)
    private LocalDateTime triggeredAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean isRead = false;

    public BudgetAlert() {}

    public BudgetAlert(Budget budget, double spentAmount, double limitAmount, double thresholdPercentage) {
        this.budget = budget;
        this.spentAmount = spentAmount;
        this.limitAmount = limitAmount;
        this.thresholdPercentage = thresholdPercentage;
        this.triggeredAt = LocalDateTime.now();
        this.isRead = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Budget getBudget() { return budget; }
    public void setBudget(Budget budget) { this.budget = budget; }

    public double getSpentAmount() { return spentAmount; }
    public void setSpentAmount(double spentAmount) { this.spentAmount = spentAmount; }

    public double getLimitAmount() { return limitAmount; }
    public void setLimitAmount(double limitAmount) { this.limitAmount = limitAmount; }

    public double getThresholdPercentage() { return thresholdPercentage; }
    public void setThresholdPercentage(double thresholdPercentage) { this.thresholdPercentage = thresholdPercentage; }

    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
