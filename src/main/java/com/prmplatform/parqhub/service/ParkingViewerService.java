package com.prmplatform.parqhub.service;

import com.prmplatform.parqhub.model.ParkingLot;
import com.prmplatform.parqhub.model.ParkingSlot;
import com.prmplatform.parqhub.repository.ParkingLotRepository;
import com.prmplatform.parqhub.repository.ParkingSlotRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ParkingViewerService {

    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSlotRepository parkingSlotRepository;

    public ParkingViewerService(ParkingLotRepository parkingLotRepository, 
                               ParkingSlotRepository parkingSlotRepository) {
        this.parkingLotRepository = parkingLotRepository;
        this.parkingSlotRepository = parkingSlotRepository;
    }

    /**
     * Get all parking lots for dropdown selection
     */
    public List<ParkingLot> getAllParkingLots() {
        return parkingLotRepository.findAll();
    }

    /**
     * Get parking lot by ID
     */
    public Optional<ParkingLot> getParkingLotById(Long lotId) {
        return parkingLotRepository.findById(lotId);
    }

    /**
     * Get all parking slots for a specific parking lot
     */
    public List<ParkingSlot> getParkingSlotsByLotId(Long lotId) {
        return parkingSlotRepository.findByParkingLotId(lotId);
    }

    /**
     * Get parking slot statistics for a specific lot
     */
    public ParkingSlotStatistics getParkingSlotStatistics(Long lotId) {
        List<ParkingSlot> slots = getParkingSlotsByLotId(lotId);
        
        long availableCount = slots.stream()
                .filter(slot -> slot.getStatus() == ParkingSlot.SlotStatus.Available)
                .count();
        
        long bookedCount = slots.stream()
                .filter(slot -> slot.getStatus() == ParkingSlot.SlotStatus.Booked)
                .count();
        
        long occupiedCount = slots.stream()
                .filter(slot -> slot.getStatus() == ParkingSlot.SlotStatus.Occupied)
                .count();

        return new ParkingSlotStatistics(availableCount, bookedCount, occupiedCount, slots.size());
    }

    /**
     * Get parking lot with its slots and statistics
     */
    public ParkingLotView getParkingLotView(Long lotId) {
        Optional<ParkingLot> lotOpt = getParkingLotById(lotId);
        if (lotOpt.isEmpty()) {
            return null;
        }

        ParkingLot lot = lotOpt.get();
        List<ParkingSlot> slots = getParkingSlotsByLotId(lotId);
        ParkingSlotStatistics statistics = getParkingSlotStatistics(lotId);

        return new ParkingLotView(lot, slots, statistics);
    }

    /**
     * Check if parking lot exists
     */
    public boolean parkingLotExists(Long lotId) {
        return parkingLotRepository.existsById(lotId);
    }

    /**
     * Get parking lot by city and location
     */
    public List<ParkingLot> getParkingLotsByCity(String city) {
        return parkingLotRepository.findByCityIgnoreCase(city);
    }

    /**
     * Get parking lot by location
     */
    public List<ParkingLot> getParkingLotsByLocation(String location) {
        return parkingLotRepository.findByLocationIgnoreCase(location);
    }

    /**
     * Inner class for parking slot statistics
     */
    public static class ParkingSlotStatistics {
        private final long availableCount;
        private final long bookedCount;
        private final long occupiedCount;
        private final long totalCount;

        public ParkingSlotStatistics(long availableCount, long bookedCount, long occupiedCount, long totalCount) {
            this.availableCount = availableCount;
            this.bookedCount = bookedCount;
            this.occupiedCount = occupiedCount;
            this.totalCount = totalCount;
        }

        public long getAvailableCount() { return availableCount; }
        public long getBookedCount() { return bookedCount; }
        public long getOccupiedCount() { return occupiedCount; }
        public long getTotalCount() { return totalCount; }
        
        public double getAvailablePercentage() {
            return totalCount > 0 ? (double) availableCount / totalCount * 100 : 0;
        }
        
        public double getBookedPercentage() {
            return totalCount > 0 ? (double) bookedCount / totalCount * 100 : 0;
        }
        
        public double getOccupiedPercentage() {
            return totalCount > 0 ? (double) occupiedCount / totalCount * 100 : 0;
        }
    }

    /**
     * Inner class for complete parking lot view
     */
    public static class ParkingLotView {
        private final ParkingLot parkingLot;
        private final List<ParkingSlot> parkingSlots;
        private final ParkingSlotStatistics statistics;

        public ParkingLotView(ParkingLot parkingLot, List<ParkingSlot> parkingSlots, ParkingSlotStatistics statistics) {
            this.parkingLot = parkingLot;
            this.parkingSlots = parkingSlots;
            this.statistics = statistics;
        }

        public ParkingLot getParkingLot() { return parkingLot; }
        public List<ParkingSlot> getParkingSlots() { return parkingSlots; }
        public ParkingSlotStatistics getStatistics() { return statistics; }
    }
}
