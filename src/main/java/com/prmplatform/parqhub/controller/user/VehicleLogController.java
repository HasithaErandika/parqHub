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
    public String logEntry(@RequestParam Long bookingId, @RequestParam Long vehicleId, @RequestParam Long lotId, HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return "error:User not logged in";
            }

            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
            if (!booking.getUser().getId().equals(user.getId())) {
                return "error:Unauthorized access to booking";
            }
            if (booking.getPaymentStatus().name().equals("Completed") || booking.getPaymentStatus().name().equals("Failed")) {
                return "error:Cannot log entry for non-pending booking";
            }
            if (vehicleLogRepository.findByVehicleIdAndExitTimeIsNull(vehicleId).isPresent()) {
                return "error:Vehicle already has an active log entry";
            }

            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
            ParkingLot parkingLot = parkingLotRepository.findById(lotId)
                    .orElseThrow(() -> new IllegalArgumentException("Parking lot not found"));

            VehicleLog log = new VehicleLog();
            log.setVehicle(vehicle);
            log.setParkingLot(parkingLot);
            log.setEntryTime(LocalDateTime.now());
            vehicleLogRepository.save(log);
            return "success:Entry logged successfully";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/log-exit")
    @ResponseBody
    public String logExit(@RequestParam Long bookingId, @RequestParam Long vehicleId, @RequestParam Long lotId, HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return "error:User not logged in";
            }

            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
            if (!booking.getUser().getId().equals(user.getId())) {
                return "error:Unauthorized access to booking";
            }
            if (booking.getPaymentStatus().name().equals("Completed") || booking.getPaymentStatus().name().equals("Failed")) {
                return "error:Cannot log exit for non-pending booking";
            }
            VehicleLog log = vehicleLogRepository.findByVehicleIdAndExitTimeIsNull(vehicleId)
                    .orElseThrow(() -> new IllegalArgumentException("No active entry found for this vehicle"));
            if (!log.getParkingLot().getId().equals(lotId)) {
                return "error:Vehicle is not logged in this parking lot";
            }

            log.setExitTime(LocalDateTime.now());
            vehicleLogRepository.save(log);
            return "success:Exit logged successfully";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @GetMapping("/log-status")
    @ResponseBody
    public LogStatus getLogStatus(@RequestParam Long bookingId, HttpSession session) {
        LogStatus status = new LogStatus();
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            status.setHasEntry(false);
            status.setHasExit(false);
            return status;
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || !booking.getUser().getId().equals(user.getId())) {
            status.setHasEntry(false);
            status.setHasExit(false);
            return status;
        }
        VehicleLog log = vehicleLogRepository.findByVehicleIdAndExitTimeIsNull(booking.getVehicle().getId())
                .orElse(null);
        status.setHasEntry(log != null);
        status.setHasExit(log != null && log.getExitTime() != null);
        return status;
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