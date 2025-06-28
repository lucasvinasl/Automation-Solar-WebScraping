package com.automation.webscraping.solar.monitor.repository;

import com.automation.webscraping.solar.monitor.model.InverterManufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InverterManufacturerRepository extends JpaRepository<InverterManufacturer, Long> {

    @Query("""
    SELECT inverter FROM InverterManufacturer inverter
    WHERE inverter.name ILIKE :name
    """)
    Optional<InverterManufacturer> findByName(@Param("name") String name);
}
