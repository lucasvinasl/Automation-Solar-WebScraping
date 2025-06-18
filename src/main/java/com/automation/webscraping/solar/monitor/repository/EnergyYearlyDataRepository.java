package com.automation.webscraping.solar.monitor.repository;

import com.automation.webscraping.solar.monitor.model.EnergyYearlyData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnergyYearlyDataRepository extends JpaRepository<EnergyYearlyData, Long> {


    List<EnergyYearlyData> findByYearAndClient_Id(Integer year, Long clientId);
}
