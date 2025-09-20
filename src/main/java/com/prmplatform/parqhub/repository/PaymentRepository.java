package com.prmplatform.parqhub.repository;

import com.prmplatform.parqhub.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingId(Long bookingId);
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'Completed' AND p.timestamp >= :startOfDay")
    Double sumAmountByCompletedAndTimestampAfter(LocalDateTime startOfDay);

}
