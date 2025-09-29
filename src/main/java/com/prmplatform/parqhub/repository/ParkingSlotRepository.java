package com.prmplatform.parqhub.repository;

import com.prmplatform.parqhub.model.ParkingSlot;
import com.prmplatform.parqhub.model.ParkingSlot.SlotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {
    List<ParkingSlot> findByParkingLotId(Long lotId);
    List<ParkingSlot> findByParkingLotIdAndStatus(Long lotId, SlotStatus status);
    long countByParkingLotIdAndStatus(Long lotId, SlotStatus status);
    long countByStatus(SlotStatus status);

    @Query("SELECT p.parkingLot.city, COUNT(p) FROM ParkingSlot p WHERE p.status = :status GROUP BY p.parkingLot.city")
    List<Object[]> countByCityAndStatus(@org.springframework.data.repository.query.Param("status") SlotStatus status);

    Page<ParkingSlot> findByStatus(SlotStatus status, Pageable pageable);
}