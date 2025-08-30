package com.prmplatform.parqhub.repository;

import com.prmplatform.parqhub.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByUserId(Long userId);
    Optional<Vehicle> findByVehicleNo(String vehicleNo);
    boolean existsByVehicleNo(String vehicleNo);
}
