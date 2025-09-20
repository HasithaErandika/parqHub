package com.prmplatform.parqhub.repository;

import com.prmplatform.parqhub.model.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {
    List<ParkingLot> findByCityIgnoreCase(String city);
    List<ParkingLot> findByLocationIgnoreCase(String location);

    // Modified to return Optional for single result to align with controller
    @Query("SELECT p FROM ParkingLot p WHERE LOWER(p.city) = LOWER(:city) AND LOWER(p.location) = LOWER(:location)")
    Optional<ParkingLot> findFirstByCityIgnoreCaseAndLocationIgnoreCase(@Param("city") String city, @Param("location") String location);

    /**
     * Get all unique cities (sorted, case-insensitive)
     */
    @Query("SELECT DISTINCT LOWER(p.city) FROM ParkingLot p WHERE p.city IS NOT NULL ORDER BY LOWER(p.city) ASC")
    List<String> findDistinctCities();

    /**
     * Get unique locations for a city (sorted, case-insensitive)
     */
    @Query("SELECT DISTINCT LOWER(p.location) FROM ParkingLot p WHERE LOWER(p.city) = LOWER(:city) AND p.location IS NOT NULL ORDER BY LOWER(p.location) ASC")
    List<String> findDistinctLocationsByCity(@Param("city") String city);
}