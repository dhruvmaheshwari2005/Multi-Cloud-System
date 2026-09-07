package com.multicloud.costmonitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "cloud_accounts")
public class CloudAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String provider; // AWS, AZURE, OCI

    @NotBlank
    @Column(nullable = false)
    private String name; // e.g., "Production AWS", "Dev Azure"

    @NotBlank
    @Column(nullable = false)
    private String accountIdentifier; // e.g., AWS Account ID, Azure Subscription ID, OCI Tenancy OCID

    // Simplified connection details
    private String credentialKey; // API Access Key or Client ID
    
    @Column(length = 1000)
    private String credentialSecret; // Secret Key, Client Secret, or Private Key
    
    private String optionalConfig; // Additional parameters like region or tenant ID

    @Column(nullable = false)
    private String status = "CONNECTED"; // CONNECTED, SYNCING, FAILED

    private LocalDateTime lastSyncTime;

    public CloudAccount() {}

    public CloudAccount(String provider, String name, String accountIdentifier, String credentialKey, String credentialSecret, String optionalConfig) {
        this.provider = provider;
        this.name = name;
        this.accountIdentifier = accountIdentifier;
        this.credentialKey = credentialKey;
        this.credentialSecret = credentialSecret;
        this.optionalConfig = optionalConfig;
        this.status = "CONNECTED";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAccountIdentifier() { return accountIdentifier; }
    public void setAccountIdentifier(String accountIdentifier) { this.accountIdentifier = accountIdentifier; }

    public String getCredentialKey() { return credentialKey; }
    public void setCredentialKey(String credentialKey) { this.credentialKey = credentialKey; }

    public String getCredentialSecret() { return credentialSecret; }
    public void setCredentialSecret(String credentialSecret) { this.credentialSecret = credentialSecret; }

    public String getOptionalConfig() { return optionalConfig; }
    public void setOptionalConfig(String optionalConfig) { this.optionalConfig = optionalConfig; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(LocalDateTime lastSyncTime) { this.lastSyncTime = lastSyncTime; }
}
