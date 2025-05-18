package com.automation.webscraping.solar.monitor.repository;

import com.automation.webscraping.solar.monitor.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
}
