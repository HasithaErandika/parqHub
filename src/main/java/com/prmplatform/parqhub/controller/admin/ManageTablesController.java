package com.prmplatform.parqhub.controller.admin;

import com.prmplatform.parqhub.model.*;
import com.prmplatform.parqhub.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/admin/tables")
public class ManageTablesController {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingLotRepository parkinglotRepository;
    private final ParkingSlotRepository parkingslotRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final VehicleLogRepository vehiclelogRepository;
    private final ReportRepository reportRepository;

    public ManageTablesController(AdminRepository adminRepository, UserRepository userRepository,
                                  VehicleRepository vehicleRepository, ParkingLotRepository parkinglotRepository,
                                  ParkingSlotRepository parkingslotRepository, BookingRepository bookingRepository,
                                  PaymentRepository paymentRepository, NotificationRepository notificationRepository,
                                  VehicleLogRepository vehiclelogRepository, ReportRepository reportRepository) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkinglotRepository = parkinglotRepository;
        this.parkingslotRepository = parkingslotRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
        this.vehiclelogRepository = vehiclelogRepository;
        this.reportRepository = reportRepository;
    }

    @GetMapping
    public String tables(HttpSession session, Model model) {
        Admin admin = (Admin) session.getAttribute("loggedInAdmin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("adminName", admin.getName());
        model.addAttribute("adminRole", admin.getRole());

        return "admin/tables";
    }

    @GetMapping("/{tableName}")
    public String manageTable(@PathVariable String tableName,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(defaultValue = "id") String sortBy,
                              @RequestParam(defaultValue = "asc") String sortDir,
                              @RequestParam(defaultValue = "") String search,
                              HttpSession session, Model model) {
        Admin admin = (Admin) session.getAttribute("loggedInAdmin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("adminName", admin.getName());
        model.addAttribute("adminRole", admin.getRole());
        model.addAttribute("tableName", tableName);
        model.addAttribute("searchTerm", search);

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<?> pageResult;

        switch (tableName) {
            case "admins":
                pageResult = adminRepository.findAll(pageable);
                break;
            case "users":
                pageResult = userRepository.findAll(pageable);
                break;
            case "vehicle":
                pageResult = vehicleRepository.findAll(pageable);
                break;
            case "parkinglot":
                pageResult = parkinglotRepository.findAll(pageable);
                break;
            case "parkingslot":
                pageResult = parkingslotRepository.findAll(pageable);
                break;
            case "booking":
                pageResult = bookingRepository.findAll(pageable);
                break;
            case "payment":
                pageResult = paymentRepository.findAll(pageable);
                break;
            case "notification":
                pageResult = notificationRepository.findAll(pageable);
                break;
            case "vehiclelog":
                pageResult = vehiclelogRepository.findAll(pageable);
                break;
            case "report":
                pageResult = reportRepository.findAll(pageable);
                break;
            default:
                return "redirect:/admin/tables";
        }

        model.addAttribute("items", pageResult.getContent());
        model.addAttribute("totalItems", pageResult.getTotalElements());
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("startItem", page * size + 1);
        model.addAttribute("endItem", Math.min((page + 1) * size, pageResult.getTotalElements()));
        model.addAttribute("newItem", createNewItem(tableName));

        return "admin/manageTable";
    }

    @GetMapping("/{tableName}/{id}")
    @ResponseBody
    public ResponseEntity<?> getItem(@PathVariable String tableName, @PathVariable Long id) {
        try {
            switch (tableName) {
                case "admins":
                    return adminRepository.findById(id)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().build());
                case "users":
                    return userRepository.findById(id)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().build());
                case "vehicle":
                    return vehicleRepository.findById(id)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().build());
                case "parkinglot":
                    return parkinglotRepository.findById(id)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().build());
                case "parkingslot":
                    return parkingslotRepository.findById(id)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().build());
                case "booking":
                    return bookingRepository.findById(id)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().build());
                case "payment":
                    return paymentRepository.findById(id)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().build());
                case "notification":
                    return notificationRepository.findById(id)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().build());
                case "vehiclelog":
                    return vehiclelogRepository.findById(id)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().build());
                case "report":
                    return reportRepository.findById(id)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().build());
                default:
                    return ResponseEntity.badRequest().body("Invalid table name");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching item: " + e.getMessage());
        }
    }

    @PostMapping("/{tableName}/save")
    public String saveItem(@PathVariable String tableName, @ModelAttribute Object item, RedirectAttributes redirectAttributes) {
        try {
            switch (tableName) {
                case "admins":
                    adminRepository.save((Admin) item);
                    break;
                case "users":
                    userRepository.save((User) item);
                    break;
                case "vehicle":
                    vehicleRepository.save((Vehicle) item);
                    break;
                case "parkinglot":
                    parkinglotRepository.save((ParkingLot) item);
                    break;
                case "parkingslot":
                    parkingslotRepository.save((ParkingSlot) item);
                    break;
                case "booking":
                    Booking booking = (Booking) item;
                    bookingRepository.save(booking);
                    break;
                case "payment":
                    Payment payment = (Payment) item;
                    payment.setTimestamp(LocalDateTime.now());
                    paymentRepository.save(payment);
                    break;
                case "notification":
                    Notification notification = (Notification) item;
                    notification.setTimestamp(LocalDateTime.now());
                    notificationRepository.save(notification);
                    break;
                case "vehiclelog":
                    vehiclelogRepository.save((VehicleLog) item);
                    break;
                case "report":
                    Report report = (Report) item;
                    report.setGeneratedDate(LocalDateTime.now());
                    reportRepository.save(report);
                    break;
                default:
                    redirectAttributes.addFlashAttribute("error", "Invalid table name");
                    return "redirect:/admin/tables";
            }
            redirectAttributes.addFlashAttribute("success", "Item saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving item: " + e.getMessage());
        }
        return "redirect:/admin/tables/" + tableName;
    }

    @DeleteMapping("/{tableName}/{id}")
    @ResponseBody
    public String deleteItem(@PathVariable String tableName, @PathVariable Long id) {
        try {
            switch (tableName) {
                case "admins":
                    if (!adminRepository.existsById(id)) return "error: Admin not found";
                    adminRepository.deleteById(id);
                    break;
                case "users":
                    if (!userRepository.existsById(id)) return "error: User not found";
                    userRepository.deleteById(id);
                    break;
                case "vehicle":
                    if (!vehicleRepository.existsById(id)) return "error: Vehicle not found";
                    vehicleRepository.deleteById(id);
                    break;
                case "parkinglot":
                    if (!parkinglotRepository.existsById(id)) return "error: Parking lot not found";
                    parkinglotRepository.deleteById(id);
                    break;
                case "parkingslot":
                    if (!parkingslotRepository.existsById(id)) return "error: Parking slot not found";
                    parkingslotRepository.deleteById(id);
                    break;
                case "booking":
                    if (!bookingRepository.existsById(id)) return "error: Booking not found";
                    bookingRepository.deleteById(id);
                    break;
                case "payment":
                    if (!paymentRepository.existsById(id)) return "error: Payment not found";
                    paymentRepository.deleteById(id);
                    break;
                case "notification":
                    if (!notificationRepository.existsById(id)) return "error: Notification not found";
                    notificationRepository.deleteById(id);
                    break;
                case "vehiclelog":
                    if (!vehiclelogRepository.existsById(id)) return "error: Vehicle log not found";
                    vehiclelogRepository.deleteById(id);
                    break;
                case "report":
                    if (!reportRepository.existsById(id)) return "error: Report not found";
                    reportRepository.deleteById(id);
                    break;
                default:
                    return "error: Invalid table name";
            }
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    private Object createNewItem(String tableName) {
        switch (tableName) {
            case "admins": return new Admin();
            case "users": return new User();
            case "vehicle": return new Vehicle();
            case "parkinglot": return new ParkingLot();
            case "parkingslot": return new ParkingSlot();
            case "booking": return new Booking();
            case "payment": return new Payment();
            case "notification": return new Notification();
            case "vehiclelog": return new VehicleLog();
            case "report": return new Report();
            default: return null;
        }
    }
}