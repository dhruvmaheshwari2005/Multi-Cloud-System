package com.multicloud.costmonitor.repository;

import com.multicloud.costmonitor.model.CloudResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CloudResourceRepository extends JpaRepository<CloudResource, Long> {
    List<CloudResource> findByCloudAccountId(Long cloudAccountId);
    void deleteByCloudAccountId(Long cloudAccountId);
}
