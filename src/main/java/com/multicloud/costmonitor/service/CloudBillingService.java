package com.multicloud.costmonitor.service;

import com.multicloud.costmonitor.model.CloudAccount;

public interface CloudBillingService {
    /**
     * Syncs historical cost/usage data for a new cloud account.
     * Generates records for the past specified number of days.
     */
    void syncHistoricalData(CloudAccount account, int days);

    /**
     * Simulates incremental cost synchronization for the current day.
     */
    void syncCurrentDayData(CloudAccount account);
}
