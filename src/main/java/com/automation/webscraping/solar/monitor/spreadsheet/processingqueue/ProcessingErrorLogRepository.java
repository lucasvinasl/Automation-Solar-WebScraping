package com.automation.webscraping.solar.monitor.spreadsheet.processingqueue;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessingErrorLogRepository extends JpaRepository<ProcessingErrorLog, Long> {
}
