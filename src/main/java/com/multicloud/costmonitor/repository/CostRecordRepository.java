package com.multicloud.costmonitor.repository;

import com.multicloud.costmonitor.model.CostRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CostRecordRepository extends JpaRepository<CostRecord, Long> {

    List<CostRecord> findByCloudAccountIdAndRecordDateBetween(Long cloudAccountId, LocalDate start, LocalDate end);
    List<CostRecord> findByRecordDateBetween(LocalDate start, LocalDate end);
    
    void deleteByCloudAccountId(Long cloudAccountId);

    @Query("SELECT SUM(c.costAmount) FROM CostRecord c WHERE c.recordDate BETWEEN :start AND :end")
    Double getTotalCost(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT SUM(c.costAmount) FROM CostRecord c WHERE c.cloudAccount.id = :accountId AND c.recordDate BETWEEN :start AND :end")
    Double getTotalCostByAccount(@Param("accountId") Long accountId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT SUM(c.costAmount) FROM CostRecord c WHERE UPPER(c.cloudAccount.provider) = UPPER(:provider) AND c.recordDate BETWEEN :start AND :end")
    Double getTotalCostByProvider(@Param("provider") String provider, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT c.cloudAccount.provider, SUM(c.costAmount) FROM CostRecord c " +
           "WHERE c.recordDate BETWEEN :start AND :end " +
           "GROUP BY c.cloudAccount.provider")
    List<Object[]> sumCostByProvider(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT c.serviceName, SUM(c.costAmount) FROM CostRecord c " +
           "WHERE c.recordDate BETWEEN :start AND :end " +
           "GROUP BY c.serviceName")
    List<Object[]> sumCostByServiceName(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT c.resourceType, SUM(c.costAmount) FROM CostRecord c " +
           "WHERE c.recordDate BETWEEN :start AND :end " +
           "GROUP BY c.resourceType")
    List<Object[]> sumCostByResourceType(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT c.recordDate, SUM(c.costAmount) FROM CostRecord c " +
           "WHERE c.recordDate BETWEEN :start AND :end " +
           "GROUP BY c.recordDate ORDER BY c.recordDate ASC")
    List<Object[]> sumCostByDay(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT c.recordDate, SUM(c.costAmount) FROM CostRecord c " +
           "WHERE c.cloudAccount.id = :accountId AND c.recordDate BETWEEN :start AND :end " +
           "GROUP BY c.recordDate ORDER BY c.recordDate ASC")
    List<Object[]> sumCostByDayAndAccount(@Param("accountId") Long accountId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
