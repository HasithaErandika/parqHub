package com.prmplatform.parqhub.controller.admin;

import com.prmplatform.parqhub.model.Admin;
import com.prmplatform.parqhub.model.ParkingLot;
import com.prmplatform.parqhub.model.ParkingSlot;
import com.prmplatform.parqhub.service.ParkingViewerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class ParkingViewerController {

    private final ParkingViewerService parkingViewerService;

    public ParkingViewerController(ParkingViewerService parkingViewerService) {
        this.parkingViewerService = parkingViewerService;
    }

    /**
     * Display the parking viewer page
     */
    @GetMapping("/parking-viewer")
    public String parkingViewer(@RequestParam(required = false) Long lotId, 
                               HttpSession session, 
                               Model model) {
        Admin admin = (Admin) session.getAttribute("loggedInAdmin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("adminName", admin.getName());
        model.addAttribute("adminRole", admin.getRole());
        
        // Get all parking lots for dropdown
        List<ParkingLot> parkingLots = parkingViewerService.getAllParkingLots();
        model.addAttribute("parkingLots", parkingLots);
        
        if (lotId != null && parkingViewerService.parkingLotExists(lotId)) {
            ParkingViewerService.ParkingLotView lotView = parkingViewerService.getParkingLotView(lotId);
            if (lotView != null) {
                model.addAttribute("selectedLot", lotView.getParkingLot());
                model.addAttribute("parkingSlots", lotView.getParkingSlots());
                
                ParkingViewerService.ParkingSlotStatistics stats = lotView.getStatistics();
                model.addAttribute("availableCount", stats.getAvailableCount());
                model.addAttribute("bookedCount", stats.getBookedCount());
                model.addAttribute("occupiedCount", stats.getOccupiedCount());
                model.addAttribute("totalCount", stats.getTotalCount());
                model.addAttribute("statistics", stats);
            }
        }
        
        return "admin/parkingViewer";
    }

    /**
     * API endpoint to get parking slots for a specific lot
     */
    @GetMapping("/api/parking-slots/{lotId}")
    @ResponseBody
    public ResponseEntity<List<ParkingSlot>> getParkingSlots(@PathVariable Long lotId) {
        if (!parkingViewerService.parkingLotExists(lotId)) {
            return ResponseEntity.notFound().build();
        }
        
        List<ParkingSlot> slots = parkingViewerService.getParkingSlotsByLotId(lotId);
        return ResponseEntity.ok(slots);
    }

    /**
     * API endpoint to get parking lot statistics
     */
    @GetMapping("/api/parking-statistics/{lotId}")
    @ResponseBody
    public ResponseEntity<ParkingViewerService.ParkingSlotStatistics> getParkingStatistics(@PathVariable Long lotId) {
        if (!parkingViewerService.parkingLotExists(lotId)) {
            return ResponseEntity.notFound().build();
        }
        
        ParkingViewerService.ParkingSlotStatistics stats = parkingViewerService.getParkingSlotStatistics(lotId);
        return ResponseEntity.ok(stats);
    }

    /**
     * API endpoint to get parking lot details
     */
    @GetMapping("/api/parking-lot/{lotId}")
    @ResponseBody
    public ResponseEntity<ParkingLot> getParkingLot(@PathVariable Long lotId) {
        return parkingViewerService.getParkingLotById(lotId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * API endpoint to get all parking lots
     */
    @GetMapping("/api/parking-lots")
    @ResponseBody
    public ResponseEntity<List<ParkingLot>> getAllParkingLots() {
        List<ParkingLot> lots = parkingViewerService.getAllParkingLots();
        return ResponseEntity.ok(lots);
    }

    /**
     * API endpoint to search parking lots by city
     */
    @GetMapping("/api/parking-lots/search")
    @ResponseBody
    public ResponseEntity<List<ParkingLot>> searchParkingLots(@RequestParam(required = false) String city,
                                                             @RequestParam(required = false) String location) {
        List<ParkingLot> lots;
        
        if (city != null && location != null) {
            lots = parkingViewerService.getParkingLotsByCity(city);
            lots = lots.stream()
                    .filter(lot -> lot.getLocation().toLowerCase().contains(location.toLowerCase()))
                    .toList();
        } else if (city != null) {
            lots = parkingViewerService.getParkingLotsByCity(city);
        } else if (location != null) {
            lots = parkingViewerService.getParkingLotsByLocation(location);
        } else {
            lots = parkingViewerService.getAllParkingLots();
        }
        
        return ResponseEntity.ok(lots);
    }
}
