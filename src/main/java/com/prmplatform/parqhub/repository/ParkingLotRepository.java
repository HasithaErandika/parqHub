package com.prmplatform.parqhub.repository;

import com.prmplatform.parqhub.model.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {
    List<ParkingLot> findByCity(String city);
    Optional<ParkingLot> findByLocation(String location);
}
