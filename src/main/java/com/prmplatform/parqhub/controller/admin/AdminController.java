package com.prmplatform.parqhub.controller.admin;

import com.prmplatform.parqhub.model.Admin;
import com.prmplatform.parqhub.model.ParkingSlot.SlotStatus;
import com.prmplatform.parqhub.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminRepository adminRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;
    private final VehicleLogRepository vehicleLogRepository;
    private final UserRepository userRepository;

    public AdminController(AdminRepository adminRepository, ParkingSlotRepository parkingSlotRepository,
                           PaymentRepository paymentRepository, BookingRepository bookingRepository,
                           NotificationRepository notificationRepository, VehicleLogRepository vehicleLogRepository,
                           UserRepository userRepository) {
        this.adminRepository = adminRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
        this.vehicleLogRepository = vehicleLogRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "admin-login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        Model model,
                        HttpSession session) {
        return adminRepository.findByEmailAndPassword(email, password)
                .map(admin -> {
                    session.setAttribute("loggedInAdmin", admin);
                    return "redirect:/admin/dashboard";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "Invalid email or password");
                    return "admin-login";
                });
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Admin admin = (Admin) session.getAttribute("loggedInAdmin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("adminName", admin.getName());
        model.addAttribute("adminRole", admin.getRole());

        try {
            if (admin.getRole().name().equals("OPERATIONS_MANAGER") || admin.getRole().name().equals("SUPER_ADMIN")) {
                List<Object[]> slotSummaries = parkingSlotRepository.countByCityAndStatus(SlotStatus.AVAILABLE);
                Map<String, Long> availableSlotsByCity = new HashMap<>();
                for (Object[] result : slotSummaries) {
                    availableSlotsByCity.put((String) result[0], (Long) result[1]);
                }
                model.addAttribute("availableSlotsByCity", availableSlotsByCity);
                model.addAttribute("totalActiveSlots", parkingSlotRepository.countByStatus(SlotStatus.OCCUPIED));
            }

            if (admin.getRole().name().equals("FINANCE_OFFICER") || admin.getRole().name().equals("SUPER_ADMIN")) {
                LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
                Double todayRevenue = paymentRepository.sumAmountByCompletedAndTimestampAfter(startOfDay);
                model.addAttribute("todayRevenue", todayRevenue != null ? todayRevenue : 0.0);
                model.addAttribute("pendingPayments", bookingRepository.countByPaymentStatus("Pending"));
            }

            if (admin.getRole().name().equals("CUSTOMER_SERVICE_OFFICER") || admin.getRole().name().equals("SUPER_ADMIN")) {
                model.addAttribute("totalUsers", userRepository.count());
                model.addAttribute("pendingBookings", bookingRepository.countByPaymentStatus("Pending"));
            }

            if (admin.getRole().name().equals("SECURITY_SUPERVISOR") || admin.getRole().name().equals("SUPER_ADMIN")) {
                model.addAttribute("activeVehicles", vehicleLogRepository.countByExitTimeIsNull());
                model.addAttribute("securityIncidents", notificationRepository.countByType("SECURITY_INCIDENT"));
            }

            if (admin.getRole().name().equals("IT_SUPPORT") || admin.getRole().name().equals("SUPER_ADMIN")) {
                model.addAttribute("systemUptime", "99.9%"); // Placeholder
                model.addAttribute("errorLogs", notificationRepository.countByType("NONE"));
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error fetching dashboard data: " + e.getMessage());
        }

        return "admin/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }
}