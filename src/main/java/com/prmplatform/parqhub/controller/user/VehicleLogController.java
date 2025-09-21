package com.prmplatform.parqhub.controller.user;

import com.prmplatform.parqhub.model.Booking;
import com.prmplatform.parqhub.model.User;
import com.prmplatform.parqhub.model.Vehicle;
import com.prmplatform.parqhub.model.ParkingLot;
import com.prmplatform.parqhub.model.VehicleLog;
import com.prmplatform.parqhub.repository.BookingRepository;
import com.prmplatform.parqhub.repository.VehicleLogRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class VehicleLogController {

    @Autowired
    private VehicleLogRepository vehicleLogRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @PostMapping("/log-entry")
    @ResponseBody
    public Map<String, String> logEntry(@RequestParam Long bookingId, @RequestParam Long vehicleId, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        try {
            LocalDateTime now = LocalDateTime.now();
            User user = (User) session.getAttribute("loggedInUser");

            if (user == null) {
                response.put("status", "error");
                response.put("message", "User not logged in");
                return response;
            }

            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

            // Validate user ownership
            if (!booking.getUser().getId().equals(user.getId())) {
                response.put("status", "error");
                response.put("message", "Unauthorized access to booking");
                return response;
            }

            // Validate payment status
            if (!booking.getPaymentStatus().name().equals("Pending")) {
                response.put("status", "error");
                response.put("message", "Cannot log entry for non-pending booking");
                return response;
            }

            // Validate vehicle
            if (!booking.getVehicle().getId().equals(vehicleId)) {
                response.put("status", "error");
                response.put("message", "Vehicle does not match booking");
                return response;
            }

            // Check if booking has a valid parking slot
            if (booking.getParkingSlot() == null) {
                response.put("status", "error");
                response.put("message", "Booking has no assigned parking slot");
                return response;
            }

            Vehicle vehicle = booking.getVehicle();
            ParkingLot parkingLot = booking.getParkingSlot().getParkingLot();

            // Validate parking lot exists and has a valid ID
            if (parkingLot == null) {
                response.put("status", "error");
                response.put("message", "No parking lot assigned to this booking");
                return response;
            }

            if (parkingLot.getId() == null) {
                response.put("status", "error");
                response.put("message", "Parking lot ID is missing");
                return response;
            }

            // Check for existing active log
            if (vehicleLogRepository.findByVehicleAndExitTimeIsNull(vehicle).isPresent()) {
                response.put("status", "error");
                response.put("message", "Vehicle already has an active log entry");
                return response;
            }

            // Create VehicleLog with proper entity relationships
            VehicleLog log = new VehicleLog();
            log.setVehicle(vehicle);
            log.setEntryTime(now);
            log.setParkingLot(parkingLot);

            // Double-check before saving
            if (log.getParkingLot() == null || log.getParkingLot().getId() == null) {
                response.put("status", "error");
                response.put("message", "Invalid parking lot reference");
                return response;
            }

            VehicleLog savedLog = vehicleLogRepository.save(log);

            response.put("status", "success");
            response.put("message", "Entry logged successfully");
            response.put("entryTime", savedLog.getEntryTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            return response;
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to log entry: " + e.getMessage());
            return response;
        }
    }

    @PostMapping("/log-exit")
    @ResponseBody
    public Map<String, String> logExit(@RequestParam Long bookingId, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        try {
            LocalDateTime now = LocalDateTime.now();
            User user = (User) session.getAttribute("loggedInUser");

            if (user == null) {
                response.put("status", "error");
                response.put("message", "User not logged in");
                return response;
            }

            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

            if (!booking.getUser().getId().equals(user.getId())) {
                response.put("status", "error");
                response.put("message", "Unauthorized access to booking");
                return response;
            }

            if (!booking.getPaymentStatus().name().equals("Pending")) {
                response.put("status", "error");
                response.put("message", "Cannot log exit for non-pending booking");
                return response;
            }

            VehicleLog log = vehicleLogRepository.findByVehicleAndExitTimeIsNull(booking.getVehicle())
                    .orElseThrow(() -> new IllegalArgumentException("No active entry found for this vehicle"));

            // ONLY set the exit time. DO NOT update booking or parking slot.
            log.setExitTime(now);
            VehicleLog savedLog = vehicleLogRepository.save(log);

            response.put("status", "success");
            response.put("message", "Exit logged successfully");
            response.put("redirectUrl", "/user/payment-gateway?bookingId=" + bookingId);
            response.put("exitTime", savedLog.getExitTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            return response;
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to log exit: " + e.getMessage());
            return response;
        }
    }

    @GetMapping("/log-entry-time")
    @ResponseBody
    public Map<String, String> getLogEntryTime(@RequestParam Long bookingId, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                response.put("status", "error");
                response.put("message", "User not logged in");
                return response;
            }

            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

            if (!booking.getUser().getId().equals(user.getId())) {
                response.put("status", "error");
                response.put("message", "Unauthorized access to booking");
                return response;
            }

            VehicleLog log = vehicleLogRepository.findByVehicleAndExitTimeIsNull(booking.getVehicle())
                    .orElse(null);

            if (log == null || log.getEntryTime() == null) {
                response.put("status", "error");
                response.put("message", "No active entry found for this vehicle");
                return response;
            }

            response.put("status", "success");
            response.put("entryTime", log.getEntryTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            return response;
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to retrieve entry time: " + e.getMessage());
            return response;
        }
    }

    @GetMapping("/log-exit-time")
    @ResponseBody
    public Map<String, String> getLogExitTime(@RequestParam Long bookingId, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                response.put("status", "error");
                response.put("message", "User not logged in");
                return response;
            }

            Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
            if (!bookingOpt.isPresent()) {
                response.put("status", "error");
                response.put("message", "Booking not found");
                return response;
            }
            Booking booking = bookingOpt.get();

            if (!booking.getUser().getId().equals(user.getId())) {
                response.put("status", "error");
                response.put("message", "Unauthorized access to booking");
                return response;
            }

            // Get the most recent completed log for this vehicle
            VehicleLog log = vehicleLogRepository.findTopByVehicleAndExitTimeIsNotNullOrderByEntryTimeDesc(booking.getVehicle())
                    .orElse(null);

            if (log == null || log.getExitTime() == null) {
                response.put("status", "error");
                response.put("message", "No exit logged for this vehicle");
                return response;
            }

            response.put("status", "success");
            response.put("exitTime", log.getExitTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            return response;
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to retrieve exit time: " + e.getMessage());
            return response;
        }
    }


    public static class LogStatus {
        private boolean hasEntry;
        private boolean hasExit;

        public boolean isHasEntry() {
            return hasEntry;
        }

        public void setHasEntry(boolean hasEntry) {
            this.hasEntry = hasEntry;
        }

        public boolean isHasExit() {
            return hasExit;
        }

        public void setHasExit(boolean hasExit) {
            this.hasExit = hasExit;
        }
    }
}