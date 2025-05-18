package com.automation.webscraping.solar.monitor.repository;

import com.automation.webscraping.solar.monitor.model.InverterManufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InverterManufacturerRepository extends JpaRepository<InverterManufacturer, Long> {

    Optional<InverterManufacturer> findByName(String name);
}
