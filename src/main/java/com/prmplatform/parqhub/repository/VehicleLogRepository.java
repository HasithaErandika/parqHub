package com.prmplatform.parqhub.repository;

import com.prmplatform.parqhub.model.VehicleLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleLogRepository extends JpaRepository<VehicleLog, Long> {
    long countByExitTimeIsNull();
}