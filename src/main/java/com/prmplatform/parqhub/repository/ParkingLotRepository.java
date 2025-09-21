package com.prmplatform.parqhub.repository;

import com.prmplatform.parqhub.model.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {
    List<ParkingLot> findByCityIgnoreCase(String city);
    List<ParkingLot> findByLocationIgnoreCase(String location);

    @Query("SELECT p FROM ParkingLot p WHERE LOWER(p.city) = LOWER(:city) AND LOWER(p.location) = LOWER(:location)")
    Optional<ParkingLot> findFirstByCityIgnoreCaseAndLocationIgnoreCase(@Param("city") String city, @Param("location") String location);

    @Query("SELECT DISTINCT LOWER(p.city) FROM ParkingLot p WHERE p.city IS NOT NULL ORDER BY LOWER(p.city) ASC")
    List<String> findDistinctCities();

    @Query("SELECT DISTINCT LOWER(p.location) FROM ParkingLot p WHERE LOWER(p.city) = LOWER(:city) AND p.location IS NOT NULL ORDER BY LOWER(p.location) ASC")
    List<String> findDistinctLocationsByCity(@Param("city") String city);

    @Query("SELECT pl FROM ParkingLot pl WHERE " +
            "(:city IS NULL OR LOWER(pl.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
            "(:location IS NULL OR LOWER(pl.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "(:maxPrice IS NULL OR pl.priceHr <= :maxPrice) AND " +
            "(:availableOnly = false OR EXISTS (SELECT ps FROM pl.parkingSlots ps WHERE ps.status = 'AVAILABLE'))")
    List<ParkingLot> findByFilters(@Param("city") String city,
                                   @Param("location") String location,
                                   @Param("maxPrice") BigDecimal maxPrice,
                                   @Param("availableOnly") boolean availableOnly);

    @Query("SELECT p FROM ParkingLot p JOIN FETCH p.parkingSlots")
    List<ParkingLot> findAllWithSlots();
}