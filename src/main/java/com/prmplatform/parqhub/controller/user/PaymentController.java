package com.prmplatform.parqhub.controller.user;

import com.prmplatform.parqhub.model.Booking;
import com.prmplatform.parqhub.model.ParkingSlot;
import com.prmplatform.parqhub.model.User;
import com.prmplatform.parqhub.model.Vehicle;
import com.prmplatform.parqhub.repository.BookingRepository;
import com.prmplatform.parqhub.repository.ParkingSlotRepository;
import com.prmplatform.parqhub.repository.VehicleRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class PaymentController {

    private final VehicleRepository vehicleRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final BookingRepository bookingRepository;

    @Autowired
    public PaymentController(VehicleRepository vehicleRepository,
                             ParkingSlotRepository parkingSlotRepository,
                             BookingRepository bookingRepository) {
        this.vehicleRepository = vehicleRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/paymentGateway")
    public String showPaymentGateway(@RequestParam Long slotId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/user/login";
        }

        Optional<ParkingSlot> slotOpt = parkingSlotRepository.findById(slotId);
        if (!slotOpt.isPresent() || slotOpt.get().getStatus() != ParkingSlot.SlotStatus.AVAILABLE) {
            model.addAttribute("error", "Selected slot is not available.");
            List<Vehicle> userVehicles = vehicleRepository.findByUserId(user.getId());
            model.addAttribute("userName", user.getName());
            model.addAttribute("userVehicles", userVehicles);
            model.addAttribute("slotId", slotId);
            return "user/paymentGateway";
        }

        List<Vehicle> userVehicles = vehicleRepository.findByUserId(user.getId());
        model.addAttribute("userName", user.getName());
        model.addAttribute("userVehicles", userVehicles);
        model.addAttribute("slotId", slotId);

        return "user/paymentGateway";
    }

    @PostMapping("/processPayment")
    public String processPayment(@RequestParam Long slotId,
                                 @RequestParam Long vehicleId,
                                 @RequestParam Integer duration,
                                 HttpSession session,
                                 Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/user/login";
        }

        Optional<ParkingSlot> slotOpt = parkingSlotRepository.findById(slotId);
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);

        if (!slotOpt.isPresent() || !vehicleOpt.isPresent()) {
            model.addAttribute("error", "Invalid slot or vehicle selected.");
            List<Vehicle> userVehicles = vehicleRepository.findByUserId(user.getId());
            model.addAttribute("userName", user.getName());
            model.addAttribute("userVehicles", userVehicles);
            model.addAttribute("slotId", slotId);
            return "user/paymentGateway";
        }

        ParkingSlot slot = slotOpt.get();
        if (slot.getStatus() != ParkingSlot.SlotStatus.AVAILABLE) {
            model.addAttribute("error", "Selected slot is no longer available.");
            List<Vehicle> userVehicles = vehicleRepository.findByUserId(user.getId());
            model.addAttribute("userName", user.getName());
            model.addAttribute("userVehicles", userVehicles);
            model.addAttribute("slotId", slotId);
            return "user/paymentGateway";
        }

        Vehicle vehicle = vehicleOpt.get();
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(duration);

        // Create a booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setVehicle(vehicle);
        booking.setParkingSlot(slot);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setPaymentStatus(Booking.PaymentStatus.Completed); // Assuming payment is processed successfully

        // Update slot status
        slot.setStatus(ParkingSlot.SlotStatus.BOOKED);
        parkingSlotRepository.save(slot);
        bookingRepository.save(booking);

        // Redirect to bookings page with success message
        model.addAttribute("success", "Payment processed successfully. Booking confirmed.");
        return "redirect:/user/bookings";
    }
}