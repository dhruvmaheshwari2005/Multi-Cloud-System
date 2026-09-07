package com.multicloud.costmonitor.controller;

import com.multicloud.costmonitor.model.CloudResource;
import com.multicloud.costmonitor.repository.CloudResourceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceInventoryController {

    private final CloudResourceRepository cloudResourceRepository;

    public ResourceInventoryController(CloudResourceRepository cloudResourceRepository) {
        this.cloudResourceRepository = cloudResourceRepository;
    }

    @GetMapping
    public ResponseEntity<List<CloudResource>> getAllResources() {
        return ResponseEntity.ok(cloudResourceRepository.findAll());
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<CloudResource>> getResourcesByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(cloudResourceRepository.findByCloudAccountId(accountId));
    }
}
