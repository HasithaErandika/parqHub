package com.prmplatform.parqhub.service;

import com.prmplatform.parqhub.model.ParkingLot;
import com.prmplatform.parqhub.model.ParkingSlot;
import com.prmplatform.parqhub.repository.ParkingLotRepository;
import com.prmplatform.parqhub.repository.ParkingSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ParkingLotServiceTest {

    @Mock
    private ParkingLotRepository parkingLotRepository;

    @Mock
    private ParkingSlotRepository parkingSlotRepository;

    @InjectMocks
    private ParkingLotService parkingLotService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateParkingSlotsForLot() {
        // Arrange
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setId(1L);
        parkingLot.setTotalSlots(5);

        when(parkingSlotRepository.findByParkingLotId(1L)).thenReturn(Arrays.asList());

        // Act
        parkingLotService.createParkingSlotsForLot(parkingLot);

        // Assert
        verify(parkingSlotRepository, times(1)).findByParkingLotId(1L);
        verify(parkingSlotRepository, times(5)).save(any(ParkingSlot.class));
    }

    @Test
    void testUpdateParkingSlotsForLot_AddSlots() {
        // Arrange
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setId(1L);
        parkingLot.setTotalSlots(7); // New total: 7

        // Act
        parkingLotService.updateParkingSlotsForLot(parkingLot, 5); // Old total: 5

        // Assert
        verify(parkingSlotRepository, times(2)).save(any(ParkingSlot.class));
    }

    @Test
    void testUpdateParkingSlotsForLot_RemoveSlots() {
        // Arrange
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setId(1L);
        parkingLot.setTotalSlots(3); // New total: 3

        ParkingSlot slot1 = new ParkingSlot();
        slot1.setId(1L);
        ParkingSlot slot2 = new ParkingSlot();
        slot2.setId(2L);
        ParkingSlot slot3 = new ParkingSlot();
        slot3.setId(3L);
        ParkingSlot slot4 = new ParkingSlot();
        slot4.setId(4L);
        ParkingSlot slot5 = new ParkingSlot();
        slot5.setId(5L);

        List<ParkingSlot> slots = Arrays.asList(slot1, slot2, slot3, slot4, slot5);

        when(parkingSlotRepository.findByParkingLotId(1L)).thenReturn(slots);

        // Act
        parkingLotService.updateParkingSlotsForLot(parkingLot, 5); // Old total: 5

        // Assert
        verify(parkingSlotRepository, times(1)).findByParkingLotId(1L);
        verify(parkingSlotRepository, times(2)).delete(any(ParkingSlot.class));
    }

    @Test
    void testDeleteParkingSlotsForLot() {
        // Arrange
        ParkingSlot slot1 = new ParkingSlot();
        slot1.setId(1L);
        ParkingSlot slot2 = new ParkingSlot();
        slot2.setId(2L);

        List<ParkingSlot> slots = Arrays.asList(slot1, slot2);

        when(parkingSlotRepository.findByParkingLotId(1L)).thenReturn(slots);

        // Act
        parkingLotService.deleteParkingSlotsForLot(1L);

        // Assert
        verify(parkingSlotRepository, times(1)).findByParkingLotId(1L);
        verify(parkingSlotRepository, times(1)).deleteAll(slots);
    }
}