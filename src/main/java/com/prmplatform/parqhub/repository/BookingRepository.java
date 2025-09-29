package com.prmplatform.parqhub.repository;

import com.prmplatform.parqhub.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByUserIdOrderByStartTimeDesc(Long userId);
    List<Booking> findByVehicleId(Long vehicleId);
    long countByPaymentStatus(Booking.PaymentStatus paymentStatus);
    
    @Query("SELECT b FROM Booking b " +
           "LEFT JOIN FETCH b.parkingSlot ps " +
           "LEFT JOIN FETCH ps.parkingLot pl " +
           "WHERE b.user.id = :userId " +
           "ORDER BY b.startTime DESC")
    List<Booking> findByUserIdWithParkingDetailsOrderByStartTimeDesc(@Param("userId") Long userId);

    Page<Booking> findByPaymentStatus(Booking.PaymentStatus paymentStatus, Pageable pageable);

}
