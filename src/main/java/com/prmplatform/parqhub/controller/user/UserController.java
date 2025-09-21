package com.prmplatform.parqhub.controller.user;

import com.prmplatform.parqhub.model.User;
import com.prmplatform.parqhub.model.Vehicle;
import com.prmplatform.parqhub.model.ParkingSlot;
import com.prmplatform.parqhub.model.Booking;
import com.prmplatform.parqhub.model.Notification;
import com.prmplatform.parqhub.model.VehicleDTO;
import com.prmplatform.parqhub.model.ParkingSlotDTO;
import com.prmplatform.parqhub.repository.UserRepository;
import com.prmplatform.parqhub.repository.VehicleRepository;
import com.prmplatform.parqhub.repository.ParkingSlotRepository;
import com.prmplatform.parqhub.repository.BookingRepository;
import com.prmplatform.parqhub.repository.NotificationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class UserController {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final BookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;

    @Autowired
    public UserController(UserRepository userRepository,
                          VehicleRepository vehicleRepository,
                          ParkingSlotRepository parkingSlotRepository,
                          BookingRepository bookingRepository,
                          NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "user-login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        Model model,
                        HttpSession session) {
        return userRepository.findByEmailAndPassword(email, password)
                .map(user -> {
                    session.setAttribute("loggedInUser", user);
                    return "redirect:/user/dashboard";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "Invalid email or password");
                    return "user-login";
                });
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/user/login";
        }

        List<Vehicle> userVehicles = vehicleRepository.findByUserId(user.getId());
        List<Booking> userBookings = bookingRepository.findByUserIdWithParkingDetailsOrderByStartTimeDesc(user.getId());

        long totalVehicles = userVehicles.size();
        long activeBookings = userBookings.stream()
                .filter(booking -> booking.getPaymentStatus() == Booking.PaymentStatus.Pending)
                .count();
        
        long completedBookings = userBookings.stream()
                .filter(booking -> booking.getPaymentStatus() == Booking.PaymentStatus.Completed)
                .count();

        // Get recent bookings (last 5)
        List<Booking> recentBookings = userBookings.stream()
                .limit(5)
                .collect(Collectors.toList());

        model.addAttribute("userId", user.getId());
        model.addAttribute("userName", user.getName());
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("userContactNo", user.getContactNo());
        model.addAttribute("totalVehicles", totalVehicles);
        model.addAttribute("activeBookings", activeBookings);
        model.addAttribute("completedBookings", completedBookings);
        model.addAttribute("recentBookings", recentBookings);

        return "user/dashboard";
    }

    @GetMapping("/vehicles")
    public String vehicles(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/user/login";
        }

        List<Vehicle> userVehicles = vehicleRepository.findByUserId(user.getId());
        List<Booking> vehicleActivity = bookingRepository.findByUserIdWithParkingDetailsOrderByStartTimeDesc(user.getId());

        model.addAttribute("userId", user.getId());
        model.addAttribute("userName", user.getName());
        model.addAttribute("vehicles", userVehicles);
        model.addAttribute("vehicleActivity", vehicleActivity);
        model.addAttribute("totalVehicles", userVehicles.size());

        return "user/vehicles";
    }

    @GetMapping("/vehicles/json")
    @ResponseBody
    public List<VehicleDTO> getUserVehicles(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return List.of();
        }
        return vehicleRepository.findByUserId(user.getId()).stream()
                .map(v -> new VehicleDTO(v.getId(), v.getVehicleNo(), v.getVehicleType().name()))
                .collect(Collectors.toList());
    }

    @PostMapping("/vehicles/add")
    @ResponseBody
    public String addVehicle(@RequestParam String brand,
                             @RequestParam String model,
                             @RequestParam String vehicleNo,
                             @RequestParam String color,
                             @RequestParam String vehicleType,
                             HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "error:User not logged in";
        }

        if (vehicleRepository.existsByVehicleNo(vehicleNo)) {
            return "error:Vehicle number already exists";
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setUser(user);
        vehicle.setBrand(brand);
        vehicle.setModel(model);
        vehicle.setVehicleNo(vehicleNo);
        vehicle.setColor(color);
        vehicle.setVehicleType(Vehicle.VehicleType.valueOf(vehicleType));

        vehicleRepository.save(vehicle);
        return "success:Vehicle added successfully";
    }

    @GetMapping("/parking-slots/{lotId}")
    @ResponseBody
    public List<ParkingSlotDTO> getParkingSlots(@PathVariable Long lotId) {
        List<ParkingSlot> slots = parkingSlotRepository.findByParkingLotId(lotId);
        return slots.stream()
                .map(slot -> new ParkingSlotDTO(slot.getId(), slot.getStatus().name()))
                .collect(Collectors.toList());
    }

    @PostMapping("/book-slot")
    @ResponseBody
    public String bookSlot(@RequestParam Long slotId,
                           @RequestParam Long vehicleId,
                           @RequestParam String startTime,
                           HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "error:User not logged in";
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime startDateTime = LocalDateTime.parse(startTime, formatter);

            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            Optional<ParkingSlot> slotOpt = parkingSlotRepository.findById(slotId);

            if (!vehicleOpt.isPresent() || !slotOpt.isPresent()) {
                return "error:Invalid vehicle or slot";
            }

            Vehicle vehicle = vehicleOpt.get();
            ParkingSlot slot = slotOpt.get();

            if (slot.getStatus() != ParkingSlot.SlotStatus.AVAILABLE) {
                return "error:Slot is not available";
            }

            Booking booking = new Booking();
            booking.setUser(user);
            booking.setVehicle(vehicle);
            booking.setParkingSlot(slot);
            booking.setStartTime(startDateTime);
            booking.setEndTime(null); // Set end_time to null
            booking.setPaymentStatus(Booking.PaymentStatus.Pending);

            slot.setStatus(ParkingSlot.SlotStatus.BOOKED);

            bookingRepository.save(booking);
            parkingSlotRepository.save(slot);

            return "success:Booking created successfully. Booking ID: " + booking.getId();
        } catch (Exception e) {
            return "error:Failed to create booking: " + e.getMessage();
        }
    }

    @GetMapping("/bookings")
    public String myBookings(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/user/login";
        }

        List<Booking> userBookings = bookingRepository.findByUserIdWithParkingDetailsOrderByStartTimeDesc(user.getId());

        model.addAttribute("userId", user.getId());
        model.addAttribute("userName", user.getName());
        model.addAttribute("bookings", userBookings);

        return "user/bookings";
    }

    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/user/login";
        }

        List<Notification> userNotifications = notificationRepository.findByUserIdOrderByTimestampDesc(user.getId());

        model.addAttribute("userId", user.getId());
        model.addAttribute("userName", user.getName());
        model.addAttribute("notifications", userNotifications);

        return "user/notifications";
    }

    @GetMapping("/settings")
    public String settings(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/user/login";
        }

        model.addAttribute("userId", user.getId());
        model.addAttribute("userName", user.getName());
        model.addAttribute("user", user);

        return "user/settings";
    }

    @PostMapping("/settings/update")
    @ResponseBody
    public String updateSettings(@RequestParam String name,
                                 @RequestParam String contactNo,
                                 HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "error:User not logged in";
        }

        try {
            user.setName(name);
            user.setContactNo(contactNo);
            userRepository.save(user);

            session.setAttribute("loggedInUser", user);

            return "success:Settings updated successfully";
        } catch (Exception e) {
            return "error:Failed to update settings: " + e.getMessage();
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/user/login";
    }
}