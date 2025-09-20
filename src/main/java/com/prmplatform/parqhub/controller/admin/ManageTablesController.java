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
    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final VehicleLogRepository vehicleLogRepository;
    private final ReportRepository reportRepository;

    public ManageTablesController(AdminRepository adminRepository, UserRepository userRepository,
                                  VehicleRepository vehicleRepository, ParkingLotRepository parkingLotRepository,
                                  ParkingSlotRepository parkingSlotRepository, BookingRepository bookingRepository,
                                  PaymentRepository paymentRepository, NotificationRepository notificationRepository,
                                  VehicleLogRepository vehicleLogRepository, ReportRepository reportRepository) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkingLotRepository = parkingLotRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
        this.vehicleLogRepository = vehicleLogRepository;
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
        initializeSampleData();

        return "admin/tables";
    }

    private void initializeSampleData() {
        if (adminRepository.count() == 0) {
            Admin sampleAdmin = new Admin();
            sampleAdmin.setName("Super Admin");
            sampleAdmin.setEmail("admin@parqhub.com");
            sampleAdmin.setPassword("admin123");
            sampleAdmin.setRole(Admin.Role.SUPER_ADMIN);
            adminRepository.save(sampleAdmin);
        }

        if (userRepository.count() == 0) {
            User sampleUser = new User();
            sampleUser.setName("John Doe");
            sampleUser.setEmail("john@example.com");
            sampleUser.setPassword("user123");
            sampleUser.setContactNo("+1234567890");
            userRepository.save(sampleUser);
        }

        if (parkingLotRepository.count() == 0) {
            ParkingLot sampleLot = new ParkingLot();
            sampleLot.setCity("Colombo");
            sampleLot.setLocation("City Center");
            sampleLot.setTotalSlots(50);
            parkingLotRepository.save(sampleLot);
        }

        if (vehicleRepository.count() == 0) {
            Vehicle sampleVehicle = new Vehicle();
            sampleVehicle.setVehicleNo("ABC-1234");
            sampleVehicle.setVehicleType(Vehicle.VehicleType.Car);
            sampleVehicle.setBrand("Toyota");
            sampleVehicle.setModel("Camry");
            sampleVehicle.setColor("Blue");
            userRepository.findById(1L).ifPresent(sampleVehicle::setUser);
            vehicleRepository.save(sampleVehicle);
        }

        if (parkingSlotRepository.count() == 0) {
            Optional<ParkingLot> firstLot = parkingLotRepository.findById(1L);
            if (firstLot.isPresent()) {
                ParkingLot lot = firstLot.get();
                for (int i = 1; i <= lot.getTotalSlots(); i++) {
                    ParkingSlot slot = new ParkingSlot();
                    slot.setParkingLot(lot);
                    if (i <= lot.getTotalSlots() * 0.6) {
                        slot.setStatus(ParkingSlot.SlotStatus.Available);
                    } else if (i <= lot.getTotalSlots() * 0.8) {
                        slot.setStatus(ParkingSlot.SlotStatus.Booked);
                    } else {
                        slot.setStatus(ParkingSlot.SlotStatus.Occupied);
                    }
                    parkingSlotRepository.save(slot);
                }
            }
        }

        if (bookingRepository.count() == 0) {
            Booking sampleBooking = new Booking();
            sampleBooking.setStartTime(LocalDateTime.now());
            sampleBooking.setEndTime(LocalDateTime.now().plusHours(2));
            sampleBooking.setPaymentStatus(Booking.PaymentStatus.Pending);
            userRepository.findById(1L).ifPresent(sampleBooking::setUser);
            vehicleRepository.findById(1L).ifPresent(sampleBooking::setVehicle);
            parkingSlotRepository.findById(1L).ifPresent(sampleBooking::setParkingSlot);
            bookingRepository.save(sampleBooking);
        }

        if (paymentRepository.count() == 0) {
            Payment samplePayment = new Payment();
            samplePayment.setAmount(new BigDecimal("100.00"));
            samplePayment.setMethod(Payment.PaymentMethod.Credit_Card);
            samplePayment.setStatus(Payment.PaymentStatus.Completed);
            samplePayment.setTimestamp(LocalDateTime.now());
            bookingRepository.findById(1L).ifPresent(samplePayment::setBooking);
            paymentRepository.save(samplePayment);
        }

        if (notificationRepository.count() == 0) {
            Notification sampleNotification = new Notification();
            sampleNotification.setType(Notification.NotificationType.Full_Slot);
            sampleNotification.setDescription("Parking lot is full");
            sampleNotification.setTimestamp(LocalDateTime.now());
            userRepository.findById(1L).ifPresent(sampleNotification::setUser);
            adminRepository.findById(1L).ifPresent(sampleNotification::setAdmin);
            notificationRepository.save(sampleNotification);
        }

        if (vehicleLogRepository.count() == 0) {
            VehicleLog sampleLog = new VehicleLog();
            sampleLog.setEntryTime(LocalDateTime.now().minusHours(1));
            sampleLog.setExitTime(LocalDateTime.now());
            vehicleRepository.findById(1L).ifPresent(sampleLog::setVehicle);
            parkingLotRepository.findById(1L).ifPresent(sampleLog::setParkingLot);
            vehicleLogRepository.save(sampleLog);
        }

        if (reportRepository.count() == 0) {
            Report sampleReport = new Report();
            sampleReport.setType(Report.ReportType.Financial);
            sampleReport.setGeneratedDate(LocalDateTime.now());
            adminRepository.findById(1L).ifPresent(sampleReport::setAdmin);
            reportRepository.save(sampleReport);
        }
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
            case "parking_lot":
                pageResult = parkingLotRepository.findAll(pageable);
                break;
            case "parking_slot":
                pageResult = parkingSlotRepository.findAll(pageable);
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
            case "vehicle_log":
                pageResult = vehicleLogRepository.findAll(pageable);
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
                case "parking_lot":
                    return parkingLotRepository.findById(id)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().build());
                case "parking_slot":
                    return parkingSlotRepository.findById(id)
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
                case "vehicle_log":
                    return vehicleLogRepository.findById(id)
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
                case "parking_lot":
                    parkingLotRepository.save((ParkingLot) item);
                    break;
                case "parking_slot":
                    parkingSlotRepository.save((ParkingSlot) item);
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
                case "vehicle_log":
                    vehicleLogRepository.save((VehicleLog) item);
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
                case "parking_lot":
                    if (!parkingLotRepository.existsById(id)) return "error: Parking lot not found";
                    parkingLotRepository.deleteById(id);
                    break;
                case "parking_slot":
                    if (!parkingSlotRepository.existsById(id)) return "error: Parking slot not found";
                    parkingSlotRepository.deleteById(id);
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
                case "vehicle_log":
                    if (!vehicleLogRepository.existsById(id)) return "error: Vehicle log not found";
                    vehicleLogRepository.deleteById(id);
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
            case "parking_lot": return new ParkingLot();
            case "parking_slot": return new ParkingSlot();
            case "booking": return new Booking();
            case "payment": return new Payment();
            case "notification": return new Notification();
            case "vehicle_log": return new VehicleLog();
            case "report": return new Report();
            default: return null;
        }
    }
}