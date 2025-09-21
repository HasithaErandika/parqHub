package com.prmplatform.parqhub.controller.user;

import com.prmplatform.parqhub.model.Booking;
import com.prmplatform.parqhub.model.User;
import com.prmplatform.parqhub.model.Vehicle;
import com.prmplatform.parqhub.model.ParkingLot;
import com.prmplatform.parqhub.model.VehicleLog;
import com.prmplatform.parqhub.repository.BookingRepository;
import com.prmplatform.parqhub.repository.VehicleLogRepository;
import com.prmplatform.parqhub.repository.VehicleRepository;
import com.prmplatform.parqhub.repository.ParkingLotRepository;
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

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @PostMapping("/log-entry")
    @ResponseBody
    public Map<String, String> logEntry(@RequestParam Long bookingId, @RequestParam Long vehicleId, @RequestParam Long lotId, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        try {
            LocalDateTime now = LocalDateTime.now();
            System.out.println("logEntry: Request received at " + now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                    " with bookingId=" + bookingId + ", vehicleId=" + vehicleId + ", lotId=" + lotId);
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                response.put("status", "error");
                response.put("message", "User not logged in");
                System.out.println("logEntry: User not logged in");
                return response;
            }

            Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
            if (!bookingOpt.isPresent()) {
                response.put("status", "error");
                response.put("message", "Booking not found");
                System.out.println("logEntry: Booking not found for ID: " + bookingId);
                return response;
            }
            Booking booking = bookingOpt.get();
            System.out.println("logEntry: Found booking ID: " + bookingId);

            // Validate user ownership
            if (!booking.getUser().getId().equals(user.getId())) {
                response.put("status", "error");
                response.put("message", "Unauthorized access to booking");
                System.out.println("logEntry: Unauthorized access for user ID: " + user.getId() + ", booking user ID: " + booking.getUser().getId());
                return response;
            }

            // Validate payment status
            if (!booking.getPaymentStatus().name().equals("Pending")) {
                response.put("status", "error");
                response.put("message", "Cannot log entry for non-pending booking");
                System.out.println("logEntry: Invalid payment status: " + booking.getPaymentStatus().name());
                return response;
            }

            // Validate vehicle
            if (!booking.getVehicle().getId().equals(vehicleId)) {
                response.put("status", "error");
                response.put("message", "Vehicle does not match booking");
                System.out.println("logEntry: Vehicle ID " + vehicleId + " does not match booking vehicle ID: " + booking.getVehicle().getId());
                return response;
            }

            // Validate parking lot
            if (!booking.getParkingSlot().getParkingLot().getId().equals(lotId)) {
                response.put("status", "error");
                response.put("message", "Parking lot does not match booking");
                System.out.println("logEntry: Parking lot ID " + lotId + " does not match booking parking lot ID: " + booking.getParkingSlot().getParkingLot().getId());
                return response;
            }

            // Check for existing active log
            if (vehicleLogRepository.findByVehicleIdAndExitTimeIsNull(vehicleId).isPresent()) {
                response.put("status", "error");
                response.put("message", "Vehicle already has an active log entry");
                System.out.println("logEntry: Active log exists for vehicle ID: " + vehicleId);
                return response;
            }

            Vehicle vehicle = booking.getVehicle();
            ParkingLot parkingLot = booking.getParkingSlot().getParkingLot();

            VehicleLog log = new VehicleLog();
            log.setVehicle(vehicle);
            log.setParkingLot(parkingLot);
            log.setEntryTime(now);
            VehicleLog savedLog = vehicleLogRepository.save(log);
            System.out.println("logEntry: Saved VehicleLog ID: " + savedLog.getId() + ", entryTime: " +
                    savedLog.getEntryTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            response.put("status", "success");
            response.put("message", "Entry logged successfully");
            return response;
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            System.out.println("logEntry: Error: " + e.getMessage());
            return response;
        }
    }

    @PostMapping("/log-exit")
    @ResponseBody
    public Map<String, String> logExit(@RequestParam Long bookingId, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        try {
            LocalDateTime now = LocalDateTime.now();
            System.out.println("logExit: Request received at " + now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                    " with bookingId=" + bookingId);
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                response.put("status", "error");
                response.put("message", "User not logged in");
                System.out.println("logExit: User not logged in");
                return response;
            }

            Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
            if (!bookingOpt.isPresent()) {
                response.put("status", "error");
                response.put("message", "Booking not found");
                System.out.println("logExit: Booking not found for ID: " + bookingId);
                return response;
            }
            Booking booking = bookingOpt.get();
            System.out.println("logExit: Found booking ID: " + bookingId);
            if (!booking.getUser().getId().equals(user.getId())) {
                response.put("status", "error");
                response.put("message", "Unauthorized access to booking");
                System.out.println("logExit: Unauthorized access for user ID: " + user.getId() + ", booking user ID: " + booking.getUser().getId());
                return response;
            }
            if (!booking.getPaymentStatus().name().equals("Pending")) {
                response.put("status", "error");
                response.put("message", "Cannot log exit for non-pending booking");
                System.out.println("logExit: Invalid payment status: " + booking.getPaymentStatus().name());
                return response;
            }

            Optional<VehicleLog> logOpt = vehicleLogRepository.findByVehicleIdAndExitTimeIsNull(booking.getVehicle().getId());
            if (!logOpt.isPresent()) {
                response.put("status", "error");
                response.put("message", "No active entry found for this vehicle");
                System.out.println("logExit: No active entry for vehicle ID: " + booking.getVehicle().getId());
                return response;
            }
            VehicleLog log = logOpt.get();
            System.out.println("logExit: Found VehicleLog ID: " + log.getId());

            // ONLY set the exit time. DO NOT update booking or parking slot.
            log.setExitTime(now);
            VehicleLog savedLog = vehicleLogRepository.save(log);
            System.out.println("logExit: Updated VehicleLog ID: " + savedLog.getId() + ", exitTime: " +
                    savedLog.getExitTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            response.put("status", "success");
            response.put("message", "Exit logged successfully");
            response.put("redirectUrl", "/user/payment-gateway?bookingId=" + bookingId);
            return response;
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            System.out.println("logExit: Error: " + e.getMessage());
            return response;
        }
    }

    @GetMapping("/log-entry-time")
    @ResponseBody
    public Map<String, String> getLogEntryTime(@RequestParam Long bookingId, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        try {
            System.out.println("logEntryTime: Request received for booking ID: " + bookingId);
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                response.put("status", "error");
                response.put("message", "User not logged in");
                System.out.println("logEntryTime: User not logged in for booking ID: " + bookingId);
                return response;
            }

            Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
            if (!bookingOpt.isPresent()) {
                response.put("status", "error");
                response.put("message", "Booking not found");
                System.out.println("logEntryTime: Booking not found for ID: " + bookingId);
                return response;
            }
            Booking booking = bookingOpt.get();
            System.out.println("logEntryTime: Found booking ID: " + bookingId);
            if (!booking.getUser().getId().equals(user.getId())) {
                response.put("status", "error");
                response.put("message", "Unauthorized access to booking");
                System.out.println("logEntryTime: Unauthorized access for user ID: " + user.getId() + ", booking user ID: " + booking.getUser().getId());
                return response;
            }

            VehicleLog log = vehicleLogRepository.findByVehicleIdAndExitTimeIsNull(booking.getVehicle().getId())
                    .orElse(null);
            if (log == null || log.getEntryTime() == null) {
                response.put("status", "error");
                response.put("message", "No active entry found for this vehicle");
                System.out.println("logEntryTime: No active entry for vehicle ID: " + booking.getVehicle().getId());
                return response;
            }

            response.put("status", "success");
            response.put("entryTime", log.getEntryTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            System.out.println("logEntryTime: Entry time for booking ID: " + bookingId + ": " + response.get("entryTime"));
            return response;
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            System.out.println("logEntryTime: Error: " + e.getMessage());
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