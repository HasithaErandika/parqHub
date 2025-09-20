package com.prmplatform.parqhub.controller.user;

import com.prmplatform.parqhub.model.*;
import com.prmplatform.parqhub.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class UserController {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;

    @Autowired
    public UserController(UserRepository userRepository, 
                        VehicleRepository vehicleRepository,
                        ParkingLotRepository parkingLotRepository,
                        ParkingSlotRepository parkingSlotRepository,
                        BookingRepository bookingRepository,
                        PaymentRepository paymentRepository,
                        NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkingLotRepository = parkingLotRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
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

        // Get user statistics
        List<Vehicle> userVehicles = vehicleRepository.findByUserId(user.getId());
        List<Booking> userBookings = bookingRepository.findByUserIdOrderByStartTimeDesc(user.getId());
        
        // Calculate statistics
        long totalVehicles = userVehicles.size();
        long activeBookings = userBookings.stream()
                .filter(booking -> booking.getPaymentStatus() == Booking.PaymentStatus.Completed)
                .count();
        
        model.addAttribute("userName", user.getName());
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("userContactNo", user.getContactNo());
        model.addAttribute("totalVehicles", totalVehicles);
        model.addAttribute("activeBookings", activeBookings);
        model.addAttribute("recentBookings", userBookings.subList(0, Math.min(userBookings.size(), 3)));

        return "user/dashboard";
    }

    @GetMapping("/vehicles")
    public String vehicles(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/user/login";
        }

        List<Vehicle> userVehicles = vehicleRepository.findByUserId(user.getId());
        List<Booking> vehicleActivity = bookingRepository.findByUserIdOrderByStartTimeDesc(user.getId());
        
        model.addAttribute("userName", user.getName());
        model.addAttribute("vehicles", userVehicles);
        model.addAttribute("vehicleActivity", vehicleActivity);
        model.addAttribute("totalVehicles", userVehicles.size());

        return "user/vehicles";
    }

    @PostMapping("/vehicles/add")
    @ResponseBody
    public String addVehicle(@RequestParam String brand,
                           @RequestParam String model,
                           @RequestParam String vehicleNo,
                           @RequestParam String color,
                           @RequestParam int year,
                           @RequestParam String vehicleType,
                           HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "error:User not logged in";
        }

        // Check if vehicle number already exists
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

    @GetMapping("/findparking")
    public String findParking(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/user/login";
        }

        List<ParkingLot> parkingLots = parkingLotRepository.findAll();
        List<Vehicle> userVehicles = vehicleRepository.findByUserId(user.getId());
        
        model.addAttribute("userName", user.getName());
        model.addAttribute("parkingLots", parkingLots);
        model.addAttribute("userVehicles", userVehicles);

        return "user/findparking";
    }

    @GetMapping("/parking-slots/{lotId}")
    @ResponseBody
    public List<ParkingSlot> getParkingSlots(@PathVariable Long lotId) {
        return parkingSlotRepository.findByParkingLotId(lotId);
    }

    @PostMapping("/book-parking")
    @ResponseBody
    public String bookParking(@RequestParam Long slotId,
                             @RequestParam Long vehicleId,
                             @RequestParam String startTime,
                             @RequestParam int duration,
                             HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "error:User not logged in";
        }

        try {
            // Parse start time
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime startDateTime = LocalDateTime.parse(startTime, formatter);
            LocalDateTime endDateTime = startDateTime.plusHours(duration);

            // Get vehicle and slot
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            Optional<ParkingSlot> slotOpt = parkingSlotRepository.findById(slotId);

            if (!vehicleOpt.isPresent() || !slotOpt.isPresent()) {
                return "error:Invalid vehicle or slot";
            }

            Vehicle vehicle = vehicleOpt.get();
            ParkingSlot slot = slotOpt.get();

            // Check if slot is available
            if (slot.getStatus() != ParkingSlot.SlotStatus.AVAILABLE) {
                return "error:Slot is not available";
            }

            // Create booking
            Booking booking = new Booking();
            booking.setUser(user);
            booking.setVehicle(vehicle);
            booking.setParkingSlot(slot);
            booking.setStartTime(startDateTime);
            booking.setEndTime(endDateTime);
            booking.setPaymentStatus(Booking.PaymentStatus.Pending);

            // Update slot status
            slot.setStatus(ParkingSlot.SlotStatus.BOOKED);

            // Save both
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

        List<Booking> userBookings = bookingRepository.findByUserIdOrderByStartTimeDesc(user.getId());
        
        model.addAttribute("userName", user.getName());
        model.addAttribute("bookings", userBookings);

        return "user/bookings";
    }

    @GetMapping("/payments")
    public String paymentHistory(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/user/login";
        }

        // Get payments through bookings
        List<Booking> userBookings = bookingRepository.findByUserId(user.getId());
        
        model.addAttribute("userName", user.getName());
        model.addAttribute("bookings", userBookings);

        return "user/payments";
    }

    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/user/login";
        }

        List<Notification> userNotifications = notificationRepository.findByUserIdOrderByTimestampDesc(user.getId());
        
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
            
            // Update session
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
