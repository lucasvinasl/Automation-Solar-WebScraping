package com.automation.webscraping.solar.monitor.repository;

import com.automation.webscraping.solar.monitor.model.EnergyYearlyData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnergyYearlyDataRepository extends JpaRepository<EnergyYearlyData, Long> {

    @Query("""
        SELECT energy
        FROM EnergyYearlyData energy
        WHERE energy.year = :year
        AND energy.client.id = :clientId
        AND energy.inverterSerialNumber = :inverterSerial
    """)
    Optional<EnergyYearlyData> findOneByYearAndClientIdAndInverterSerial(@Param("year") Integer year,
                                                     @Param("clientId") Long clientId,
                                                     @Param("inverterSerial") String inverterSerial);
}
