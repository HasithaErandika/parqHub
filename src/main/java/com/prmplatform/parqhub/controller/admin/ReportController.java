package com.prmplatform.parqhub.controller.admin;

import com.prmplatform.parqhub.model.Admin;
import com.prmplatform.parqhub.model.Booking;
import com.prmplatform.parqhub.model.ParkingSlot;
import com.prmplatform.parqhub.model.Report;
import com.prmplatform.parqhub.repository.ReportRepository;
import com.prmplatform.parqhub.repository.PaymentRepository;
import com.prmplatform.parqhub.repository.BookingRepository;
import com.prmplatform.parqhub.repository.ParkingSlotRepository;
import com.prmplatform.parqhub.repository.ParkingLotRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class ReportController {

    private final ReportRepository reportRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingLotRepository parkingLotRepository;

    public ReportController(ReportRepository reportRepository,
                           PaymentRepository paymentRepository,
                           BookingRepository bookingRepository,
                           ParkingSlotRepository parkingSlotRepository,
                           ParkingLotRepository parkingLotRepository) {
        this.reportRepository = reportRepository;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.parkingLotRepository = parkingLotRepository;
    }

    /**
     * Display the main reports dashboard
     */
    @GetMapping("/reports")
    public String reportsDashboard(HttpSession session, Model model) {
        Admin admin = (Admin) session.getAttribute("loggedInAdmin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("adminName", admin.getName());
        model.addAttribute("adminRole", admin.getRole());
        
        return "admin/reports";
    }

    /**
     * Display the financial report page
     */
    @GetMapping("/report/financial")
    public String financialReport(HttpSession session, Model model) {
        Admin admin = (Admin) session.getAttribute("loggedInAdmin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("adminName", admin.getName());
        model.addAttribute("adminRole", admin.getRole());
        
        return "admin/report/financeReport";
    }

    /**
     * Display the occupancy report page
     */
    @GetMapping("/report/occupancy")
    public String occupancyReport(HttpSession session, Model model) {
        Admin admin = (Admin) session.getAttribute("loggedInAdmin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("adminName", admin.getName());
        model.addAttribute("adminRole", admin.getRole());
        
        return "admin/report/occupancyReport";
    }

    /**
     * Display the performance report page
     */
    @GetMapping("/report/performance")
    public String performanceReport(HttpSession session, Model model) {
        Admin admin = (Admin) session.getAttribute("loggedInAdmin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("adminName", admin.getName());
        model.addAttribute("adminRole", admin.getRole());
        
        return "admin/report/performanceReport";
    }

    /**
     * Generate financial report data
     */
    @GetMapping("/api/reports/financial")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFinancialReportData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String endDate) {
        
        Map<String, Object> reportData = new HashMap<>();
        
        try {
            // Calculate financial metrics
            long totalPayments = paymentRepository.count();
            double totalRevenue = paymentRepository.findAll().stream()
                    .mapToDouble(payment -> payment.getAmount().doubleValue())
                    .sum();
            double avgPayment = totalPayments > 0 ? totalRevenue / totalPayments : 0;
            
            // Count payments by status
            long completedPayments = paymentRepository.findAll().stream()
                    .filter(payment -> payment.getStatus() == com.prmplatform.parqhub.model.Payment.PaymentStatus.Completed)
                    .count();
            long pendingPayments = paymentRepository.findAll().stream()
                    .filter(payment -> payment.getStatus() == com.prmplatform.parqhub.model.Payment.PaymentStatus.Pending)
                    .count();
            long failedPayments = paymentRepository.findAll().stream()
                    .filter(payment -> payment.getStatus() == com.prmplatform.parqhub.model.Payment.PaymentStatus.Failed)
                    .count();
            
            reportData.put("totalRevenue", totalRevenue);
            reportData.put("totalPayments", totalPayments);
            reportData.put("avgPayment", avgPayment);
            reportData.put("completedPayments", completedPayments);
            reportData.put("pendingPayments", pendingPayments);
            reportData.put("failedPayments", failedPayments);
            
            return ResponseEntity.ok(reportData);
        } catch (Exception e) {
            reportData.put("error", "Failed to generate financial report: " + e.getMessage());
            return ResponseEntity.internalServerError().body(reportData);
        }
    }

    /**
     * Generate occupancy report data
     */
    @GetMapping("/api/reports/occupancy")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOccupancyReportData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String endDate,
            @RequestParam(required = false) Long parkingLotId) {
        
        Map<String, Object> reportData = new HashMap<>();
        
        try {
            // Calculate occupancy metrics
            long totalSlots = parkingSlotRepository.count();
            long availableSlots = parkingSlotRepository.findAll().stream()
                    .filter(slot -> slot.getStatus() == ParkingSlot.SlotStatus.AVAILABLE)
                    .count();
            long bookedSlots = parkingSlotRepository.findAll().stream()
                    .filter(slot -> slot.getStatus() == ParkingSlot.SlotStatus.BOOKED)
                    .count();
            long occupiedSlots = parkingSlotRepository.findAll().stream()
                    .filter(slot -> slot.getStatus() == ParkingSlot.SlotStatus.OCCUPIED)
                    .count();
            
            double avgOccupancy = totalSlots > 0 ? (double) (bookedSlots + occupiedSlots) / totalSlots * 100 : 0;
            long totalBookings = bookingRepository.count();
            
            reportData.put("totalSlots", totalSlots);
            reportData.put("availableSlots", availableSlots);
            reportData.put("bookedSlots", bookedSlots);
            reportData.put("occupiedSlots", occupiedSlots);
            reportData.put("avgOccupancy", avgOccupancy);
            reportData.put("totalBookings", totalBookings);
            
            return ResponseEntity.ok(reportData);
        } catch (Exception e) {
            reportData.put("error", "Failed to generate occupancy report: " + e.getMessage());
            return ResponseEntity.internalServerError().body(reportData);
        }
    }

    /**
     * Generate performance report data
     */
    @GetMapping("/api/reports/performance")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPerformanceReportData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String endDate) {
        
        Map<String, Object> reportData = new HashMap<>();
        
        try {
            // Calculate performance metrics
            long totalBookings = bookingRepository.count();
            long totalPayments = paymentRepository.count();
            long totalSlots = parkingSlotRepository.count();
            long totalLots = parkingLotRepository.count();
            
            // Calculate success rates
            long successfulBookings = bookingRepository.findAll().stream()
                    .filter(booking -> booking.getPaymentStatus() == Booking.PaymentStatus.Completed)
                    .count();
            long successfulPayments = paymentRepository.findAll().stream()
                    .filter(payment -> payment.getStatus() == com.prmplatform.parqhub.model.Payment.PaymentStatus.Completed)
                    .count();
            
            double bookingSuccessRate = totalBookings > 0 ? (double) successfulBookings / totalBookings * 100 : 0;
            double paymentSuccessRate = totalPayments > 0 ? (double) successfulPayments / totalPayments * 100 : 0;
            
            reportData.put("totalBookings", totalBookings);
            reportData.put("totalPayments", totalPayments);
            reportData.put("totalSlots", totalSlots);
            reportData.put("totalLots", totalLots);
            reportData.put("bookingSuccessRate", bookingSuccessRate);
            reportData.put("paymentSuccessRate", paymentSuccessRate);
            reportData.put("systemUptime", 99.8); // Simulated
            reportData.put("avgResponseTime", 120); // Simulated
            
            return ResponseEntity.ok(reportData);
        } catch (Exception e) {
            reportData.put("error", "Failed to generate performance report: " + e.getMessage());
            return ResponseEntity.internalServerError().body(reportData);
        }
    }

    /**
     * Create a new report record
     */
    @PostMapping("/api/reports/generate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateReport(
            @RequestParam String reportType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String endDate,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Admin admin = (Admin) session.getAttribute("loggedInAdmin");
            if (admin == null) {
                response.put("error", "Admin not logged in");
                return ResponseEntity.status(401).body(response);
            }
            
            // Create report record
            Report report = new Report();
            report.setType(Report.ReportType.valueOf(reportType.toUpperCase()));
            report.setGeneratedDate(LocalDateTime.now());
            report.setAdmin(admin);
            
            Report savedReport = reportRepository.save(report);
            
            response.put("success", true);
            response.put("reportId", savedReport.getId());
            response.put("message", "Report generated successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Failed to generate report: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get all reports
     */
    @GetMapping("/api/reports")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllReports() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("reports", reportRepository.findAll());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Failed to fetch reports: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
