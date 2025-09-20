package com.prmplatform.parqhub.repository;

import com.prmplatform.parqhub.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByUserIdOrderByStartTimeDesc(Long userId);
    List<Booking> findByVehicleId(Long vehicleId);
    long countByPaymentStatus(String paymentStatus);
}
