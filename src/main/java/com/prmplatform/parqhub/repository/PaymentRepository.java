package com.prmplatform.parqhub.repository;

import com.prmplatform.parqhub.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find payment by booking ID
    Optional<Payment> findByBookingId(Long bookingId);

    // Find payments by user ID (through booking relationship)
    @Query("SELECT p FROM Payment p WHERE p.booking.user.id = :userId")
    List<Payment> findByUserId(Long userId);

    // Sum amount for completed payments for a user after a timestamp
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'Completed' AND p.booking.user.id = :userId AND p.timestamp >= :timestamp")
    Double sumAmountByUserIdAndCompletedAndTimestampAfter(Long userId, LocalDateTime timestamp);

    // Original method for total spent today (all users)
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'Completed' AND p.timestamp >= :startOfDay")
    Double sumAmountByCompletedAndTimestampAfter(LocalDateTime startOfDay);

    @Query("SELECT p FROM Payment p WHERE LOWER(p.method) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.status) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Payment> findByMethodContainingIgnoreCaseOrStatusContainingIgnoreCase(@Param("search") String search, @Param("search") String search2, Pageable pageable);

}