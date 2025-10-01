package com.prmplatform.parqhub.controller.admin;

import com.prmplatform.parqhub.model.*;
import com.prmplatform.parqhub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Collections;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminManageSlotControllerTest {

    @Mock
    private ParkingSlotRepository parkingSlotRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    @InjectMocks
    private AdminManageSlotController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testManageParkingSlot_Unauthorized() {
        when(session.getAttribute("loggedInAdmin")).thenReturn(null);
        
        String viewName = controller.manageParkingSlot(1L, session, model);
        
        assertEquals("redirect:/admin/login", viewName);
    }

    @Test
    void testManageParkingSlot_InvalidSlotId() {
        Admin admin = new Admin();
        admin.setName("Test Admin");
        admin.setRole(Admin.Role.SUPER_ADMIN);
        
        when(session.getAttribute("loggedInAdmin")).thenReturn(admin);
        when(parkingSlotRepository.findById(999L)).thenReturn(Optional.empty());
        
        String viewName = controller.manageParkingSlot(999L, session, model);
        
        assertEquals("redirect:/admin/parking-viewer", viewName);
    }

    @Test
    void testManageParkingSlot_ValidSlot_NoBooking() {
        Admin admin = new Admin();
        admin.setName("Test Admin");
        admin.setRole(Admin.Role.SUPER_ADMIN);
        
        ParkingSlot slot = new ParkingSlot();
        slot.setId(1L);
        slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
        
        ParkingLot lot = new ParkingLot();
        lot.setCity("Test City");
        lot.setLocation("Test Location");
        slot.setParkingLot(lot);
        
        when(session.getAttribute("loggedInAdmin")).thenReturn(admin);
        when(parkingSlotRepository.findById(1L)).thenReturn(Optional.of(slot));
        when(bookingRepository.findByParkingSlotId(1L)).thenReturn(Collections.emptyList());
        
        String viewName = controller.manageParkingSlot(1L, session, model);
        
        assertEquals("admin/manageParkingSlot", viewName);
        verify(model).addAttribute("adminName", "Test Admin");
        verify(model).addAttribute("adminRole", Admin.Role.SUPER_ADMIN);
        verify(model).addAttribute("slot", slot);
    }

    @Test
    void testSendNotification_Unauthorized() {
        when(session.getAttribute("loggedInAdmin")).thenReturn(null);
        
        ResponseEntity<String> response = controller.sendNotification(1L, "OVERSTAY", "Test description", session);
        
        assertEquals(401, response.getStatusCode().value());
        assertEquals("Unauthorized", response.getBody());
    }

    @Test
    void testSendNotification_InvalidSlotId() {
        Admin admin = new Admin();
        admin.setName("Test Admin");
        admin.setRole(Admin.Role.SUPER_ADMIN);
        
        when(session.getAttribute("loggedInAdmin")).thenReturn(admin);
        when(parkingSlotRepository.findById(999L)).thenReturn(Optional.empty());
        
        ResponseEntity<String> response = controller.sendNotification(999L, "OVERSTAY", "Test description", session);
        
        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid slot ID", response.getBody());
    }

    @Test
    void testSendNotification_NoActiveBooking() {
        Admin admin = new Admin();
        admin.setName("Test Admin");
        admin.setRole(Admin.Role.SUPER_ADMIN);
        
        ParkingSlot slot = new ParkingSlot();
        slot.setId(1L);
        slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
        
        when(session.getAttribute("loggedInAdmin")).thenReturn(admin);
        when(parkingSlotRepository.findById(1L)).thenReturn(Optional.of(slot));
        when(bookingRepository.findByParkingSlotId(1L)).thenReturn(Collections.emptyList());
        
        ResponseEntity<String> response = controller.sendNotification(1L, "OVERSTAY", "Test description", session);
        
        assertEquals(400, response.getStatusCode().value());
        assertEquals("No active booking found for this slot", response.getBody());
    }
}