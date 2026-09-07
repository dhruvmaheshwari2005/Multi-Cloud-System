package com.multicloud.costmonitor.repository;

import com.multicloud.costmonitor.model.BudgetAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BudgetAlertRepository extends JpaRepository<BudgetAlert, Long> {
    List<BudgetAlert> findByOrderByTriggeredAtDesc();
    List<BudgetAlert> findByIsReadFalseOrderByTriggeredAtDesc();
}
