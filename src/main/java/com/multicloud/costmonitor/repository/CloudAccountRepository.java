package com.multicloud.costmonitor.repository;

import com.multicloud.costmonitor.model.CloudAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CloudAccountRepository extends JpaRepository<CloudAccount, Long> {
}
