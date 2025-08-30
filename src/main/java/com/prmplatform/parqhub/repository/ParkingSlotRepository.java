package com.prmplatform.parqhub.repository;

import com.prmplatform.parqhub.model.ParkingSlot;
import com.prmplatform.parqhub.model.ParkingSlot.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {
    List<ParkingSlot> findByParkingLotId(Long lotId);
    List<ParkingSlot> findByParkingLotIdAndStatus(Long lotId, SlotStatus status);
    long countByParkingLotIdAndStatus(Long lotId, SlotStatus status);
}
