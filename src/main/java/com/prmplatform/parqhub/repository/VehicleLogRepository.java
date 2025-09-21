package com.prmplatform.parqhub.repository;

import com.prmplatform.parqhub.model.VehicleLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleLogRepository extends JpaRepository<VehicleLog, Long> {
    long countByExitTimeIsNull();
    Optional<VehicleLog> findByVehicleIdAndExitTimeIsNull(Long vehicleId);
    Optional<VehicleLog> findTopByVehicleIdAndExitTimeIsNotNullOrderByEntryTimeDesc(Long vehicleId);
    Optional<VehicleLog> findByVehicleIdAndExitTimeIsNotNull(Long vehicleId);
}