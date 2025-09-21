package com.prmplatform.parqhub.controller.user;

import com.prmplatform.parqhub.model.Booking;
import com.prmplatform.parqhub.model.ParkingLot;
import com.prmplatform.parqhub.model.ParkingSlot;
import com.prmplatform.parqhub.model.Payment;
import com.prmplatform.parqhub.model.User;
import com.prmplatform.parqhub.model.VehicleLog;
import com.prmplatform.parqhub.repository.BookingRepository;
import com.prmplatform.parqhub.repository.ParkingLotRepository;
import com.prmplatform.parqhub.repository.ParkingSlotRepository;
import com.prmplatform.parqhub.repository.PaymentRepository;
import com.prmplatform.parqhub.repository.VehicleLogRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class PaymentController {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final VehicleLogRepository vehicleLogRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSlotRepository parkingSlotRepository;

    @Autowired
    public PaymentController(BookingRepository bookingRepository,
                             PaymentRepository paymentRepository,
                             VehicleLogRepository vehicleLogRepository,
                             ParkingLotRepository parkingLotRepository,
                             ParkingSlotRepository parkingSlotRepository) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.vehicleLogRepository = vehicleLogRepository;
        this.parkingLotRepository = parkingLotRepository;
        this.parkingSlotRepository = parkingSlotRepository;
    }

    @GetMapping("/payment-gateway")
    public String paymentGateway(@RequestParam Long bookingId, HttpSession session, Model model) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return "redirect:/user/login";
            }

            Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
            if (!bookingOpt.isPresent()) {
                model.addAttribute("error", "Booking not found");
                return "user/payment-gateway";
            }

            Booking booking = bookingOpt.get();
            if (!booking.getUser().getId().equals(user.getId())) {
                model.addAttribute("error", "Unauthorized access to booking");
                return "user/payment-gateway";
            }

            // Check if booking is already completed
            if (booking.getPaymentStatus() == Booking.PaymentStatus.Completed) {
                Optional<Payment> paymentOpt = paymentRepository.findByBookingId(bookingId);
                if (paymentOpt.isPresent()) {
                    model.addAttribute("error", "This booking has already been paid. View details in Payment History.");
                    return "user/payment-gateway";
                }
            }

            // Check if vehicle has exited
            Optional<VehicleLog> vehicleLogOpt = vehicleLogRepository.findByVehicleIdAndExitTimeIsNotNull(booking.getVehicle().getId());
            if (!vehicleLogOpt.isPresent()) {
                model.addAttribute("error", "Vehicle has not exited yet. Please log exit first.");
                return "user/payment-gateway";
            }

            VehicleLog vehicleLog = vehicleLogOpt.get();

            // Get parking lot info safely
            ParkingLot parkingLot;
            if (booking.getParkingSlot() != null && booking.getParkingSlot().getParkingLot() != null) {
                parkingLot = booking.getParkingSlot().getParkingLot();
            } else {
                // Fallback to vehicle log's parking lot
                if (vehicleLog.getParkingLot() == null) {
                    model.addAttribute("error", "Parking lot information not available");
                    return "user/payment-gateway";
                }
                parkingLot = vehicleLog.getParkingLot();
            }

            long hours = ChronoUnit.HOURS.between(vehicleLog.getEntryTime(), vehicleLog.getExitTime());
            if (hours == 0) hours = 1; // Minimum 1 hour charge
            BigDecimal hoursBigDecimal = new BigDecimal(hours);
            BigDecimal amount = hoursBigDecimal.multiply(parkingLot.getPriceHr());

            model.addAttribute("selectedBooking", booking);
            model.addAttribute("vehicleLog", vehicleLog);
            model.addAttribute("hours", hours);
            model.addAttribute("pricePerHour", parkingLot.getPriceHr());
            model.addAttribute("amount", amount);

            return "user/payment-gateway";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load payment page: " + e.getMessage());
            e.printStackTrace();
            return "user/payment-gateway";
        }
    }

    @PostMapping("/processPayment")
    @Transactional
    public String processPayment(@RequestParam Long bookingId,
                                 @RequestParam String paymentMethod,
                                 HttpSession session,
                                 Model model) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return "redirect:/user/login";
            }

            Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
            if (!bookingOpt.isPresent()) {
                model.addAttribute("error", "Booking not found");
                return "user/payment-gateway";
            }

            Booking booking = bookingOpt.get();
            if (!booking.getUser().getId().equals(user.getId())) {
                model.addAttribute("error", "Unauthorized access to booking");
                return "user/payment-gateway";
            }

            if (booking.getPaymentStatus() != Booking.PaymentStatus.Pending) {
                model.addAttribute("error", "Payment already processed or invalid status");
                return "user/payment-gateway";
            }

            Optional<VehicleLog> vehicleLogOpt = vehicleLogRepository.findByVehicleIdAndExitTimeIsNotNull(booking.getVehicle().getId());
            if (!vehicleLogOpt.isPresent()) {
                model.addAttribute("error", "Vehicle has not exited yet");
                return "user/payment-gateway";
            }

            VehicleLog vehicleLog = vehicleLogOpt.get();

            // Get parking information safely before modifying the booking
            ParkingLot parkingLot;
            ParkingSlot originalSlot = null;
            Long originalSlotId = null;
            Long originalParkingLotId = null;

            // Store parking information before clearing the reference
            if (booking.getParkingSlot() != null) {
                originalSlot = booking.getParkingSlot();
                originalSlotId = originalSlot.getId();
                if (originalSlot.getParkingLot() != null) {
                    originalParkingLotId = originalSlot.getParkingLot().getId();
                    parkingLot = originalSlot.getParkingLot();
                } else {
                    // Fallback to vehicle log's parking lot
                    parkingLot = vehicleLog.getParkingLot();
                }
            } else {
                // Fallback to vehicle log's parking lot
                if (vehicleLog.getParkingLot() == null) {
                    model.addAttribute("error", "Parking lot not found");
                    return "user/payment-gateway";
                }
                parkingLot = vehicleLog.getParkingLot();
            }

            long hours = ChronoUnit.HOURS.between(vehicleLog.getEntryTime(), vehicleLog.getExitTime());
            if (hours == 0) hours = 1; // Minimum 1 hour charge
            BigDecimal hoursBigDecimal = new BigDecimal(hours);
            BigDecimal amount = hoursBigDecimal.multiply(parkingLot.getPriceHr());

            // STEP 1: Create and save the payment FIRST (with historical slot info)
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setAmount(amount);
            payment.setMethod(Payment.PaymentMethod.valueOf(paymentMethod));
            payment.setStatus(Payment.PaymentStatus.Completed);
            payment.setTimestamp(LocalDateTime.now());

            // Store original slot information for historical records
            payment.setOriginalSlotId(originalSlotId);
            payment.setOriginalParkingLotId(originalParkingLotId);

            Payment savedPayment = paymentRepository.save(payment);

            // STEP 2: Free up the parking slot (if it exists and is still referenced)
            if (originalSlot != null) {
                originalSlot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
                parkingSlotRepository.save(originalSlot);
                System.out.println("PaymentController: Freed up parking slot ID: " + originalSlot.getId());
            }

            // STEP 3: Update booking - SET parkingSlot to NULL to break the relationship
            booking.setEndTime(vehicleLog.getExitTime());
            booking.setPaymentStatus(Booking.PaymentStatus.Completed);

            // CRITICAL: Set parkingSlot to null to break the relationship and avoid constraint violation
            booking.setParkingSlot(null);

            // Save the booking with the null parkingSlot reference
            Booking savedBooking = bookingRepository.save(booking);
            System.out.println("PaymentController: Booking saved successfully. ParkingSlot reference cleared. Booking ID: " + savedBooking.getId());

            String successMessage = "Payment processed successfully for LKR " + amount.toString();
            String encodedSuccess = URLEncoder.encode(successMessage, StandardCharsets.UTF_8);

            return "redirect:/user/payments?success=" + encodedSuccess;

        } catch (Exception e) {
            // Rollback will happen automatically due to @Transactional
            System.err.println("PaymentController: Payment processing failed: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Failed to process payment: " + e.getMessage());
            return "user/payment-gateway";
        }
    }

    @GetMapping("/payments")
    public String paymentHistory(HttpSession session, Model model,
                                 @RequestParam(required = false) String success,
                                 @RequestParam(required = false) String error) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return "redirect:/user/login";
            }

            List<Booking> userBookings = bookingRepository.findByUserId(user.getId());
            List<Payment> userPayments = paymentRepository.findByUserId(user.getId());

            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            Double totalSpentToday = paymentRepository.sumAmountByUserIdAndCompletedAndTimestampAfter(user.getId(), startOfDay);
            if (totalSpentToday == null) totalSpentToday = 0.0;

            model.addAttribute("userId", user.getId());
            model.addAttribute("userName", user.getName());
            model.addAttribute("bookings", userBookings);
            model.addAttribute("payments", userPayments);
            model.addAttribute("totalSpentToday", totalSpentToday);

            // Handle redirect parameters
            if (success != null) {
                model.addAttribute("success", success);
            }
            if (error != null) {
                model.addAttribute("error", error);
            }

            return "user/payments";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load payment history: " + e.getMessage());
            e.printStackTrace();
            return "user/payments";
        }
    }
}