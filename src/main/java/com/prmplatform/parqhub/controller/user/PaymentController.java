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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
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

            // Handle completed bookings that may not have parking slot
            if (booking.getParkingSlot() == null) {
                model.addAttribute("error", "Cannot process payment for completed booking");
                return "user/payment-gateway";
            }

            Optional<VehicleLog> vehicleLogOpt = vehicleLogRepository.findByVehicleIdAndExitTimeIsNotNull(booking.getVehicle().getId());
            if (!vehicleLogOpt.isPresent()) {
                model.addAttribute("error", "Vehicle has not exited yet");
                return "user/payment-gateway";
            }

            VehicleLog vehicleLog = vehicleLogOpt.get();
            
            Optional<ParkingLot> parkingLotOpt = parkingLotRepository.findById(booking.getParkingSlot().getParkingLot().getId());
            if (!parkingLotOpt.isPresent()) {
                model.addAttribute("error", "Parking lot not found");
                return "user/payment-gateway";
            }

            ParkingLot parkingLot = parkingLotOpt.get();

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
            return "error"; // Fallback to error page
        }
    }

    @PostMapping("/processPayment")
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

            if (!booking.getPaymentStatus().name().equals("Pending")) {
                model.addAttribute("error", "Payment already processed or invalid status");
                return "user/payment-gateway";
            }

            // Handle completed bookings that may not have parking slot
            if (booking.getParkingSlot() == null) {
                model.addAttribute("error", "Cannot process payment for completed booking");
                return "user/payment-gateway";
            }

            Optional<VehicleLog> vehicleLogOpt = vehicleLogRepository.findByVehicleIdAndExitTimeIsNotNull(booking.getVehicle().getId());
            if (!vehicleLogOpt.isPresent()) {
                model.addAttribute("error", "Vehicle has not exited yet");
                return "user/payment-gateway";
            }

            VehicleLog vehicleLog = vehicleLogOpt.get();
            
            Optional<ParkingLot> parkingLotOpt = parkingLotRepository.findById(booking.getParkingSlot().getParkingLot().getId());
            if (!parkingLotOpt.isPresent()) {
                model.addAttribute("error", "Parking lot not found");
                return "user/payment-gateway";
            }

            ParkingLot parkingLot = parkingLotOpt.get();

            long hours = ChronoUnit.HOURS.between(vehicleLog.getEntryTime(), vehicleLog.getExitTime());
            if (hours == 0) hours = 1; // Minimum 1 hour charge
            BigDecimal hoursBigDecimal = new BigDecimal(hours);
            BigDecimal amount = hoursBigDecimal.multiply(parkingLot.getPriceHr());

            // Simulate payment processing
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setAmount(amount);
            payment.setMethod(Payment.PaymentMethod.valueOf(paymentMethod));
            payment.setStatus(Payment.PaymentStatus.Completed);
            payment.setTimestamp(LocalDateTime.now());

            // Update booking with end time and payment status
            booking.setEndTime(vehicleLog.getExitTime());
            booking.setPaymentStatus(Booking.PaymentStatus.Completed);

            // Update parking slot status to AVAILABLE
            ParkingSlot slot = booking.getParkingSlot();
            slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);

            // Save all entities
            paymentRepository.save(payment);
            bookingRepository.save(booking);
            parkingSlotRepository.save(slot);

            model.addAttribute("success", "Payment processed successfully for LKR " + amount.toString());
            return "redirect:/user/payments";

        } catch (Exception e) {
            model.addAttribute("error", "Failed to process payment: " + e.getMessage());
            return "user/payment-gateway";
        }
    }

    @GetMapping("/payments")
    public String paymentHistory(HttpSession session, Model model) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return "redirect:/user/login";
            }

            List<Booking> userBookings = bookingRepository.findByUserIdWithParkingDetailsOrderByStartTimeDesc(user.getId());
            List<Payment> userPayments = paymentRepository.findAll().stream()
                    .filter(payment -> userBookings.stream().anyMatch(booking -> booking.getId().equals(payment.getBooking().getId())))
                    .toList();

            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            Double totalSpentToday = paymentRepository.sumAmountByCompletedAndTimestampAfter(startOfDay);
            if (totalSpentToday == null) totalSpentToday = 0.0;

            model.addAttribute("userId", user.getId());
            model.addAttribute("userName", user.getName());
            model.addAttribute("bookings", userBookings);
            model.addAttribute("payments", userPayments);
            model.addAttribute("totalSpentToday", totalSpentToday);

            return "user/payments";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load payment history: " + e.getMessage());
            return "error"; // Fallback to error page
        }
    }
}