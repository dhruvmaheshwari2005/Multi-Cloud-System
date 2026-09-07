package com.multicloud.costmonitor.repository;

import com.multicloud.costmonitor.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByCloudAccountId(Long cloudAccountId);
    void deleteByCloudAccountId(Long cloudAccountId);
}
