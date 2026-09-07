package com.multicloud.costmonitor.repository;

import com.multicloud.costmonitor.model.SentEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SentEmailRepository extends JpaRepository<SentEmail, Long> {
    List<SentEmail> findAllByOrderBySentAtDesc();
}
