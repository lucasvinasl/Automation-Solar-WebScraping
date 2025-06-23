package com.automation.webscraping.solar.monitor.spreadsheet.processingqueue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessingQueueEntryRepository extends JpaRepository<ProcessingQueueEntry, Long> {

    List<ProcessingQueueEntry> findTop10ByStatusQueueOrderByInsertedAtAsc(ProcessingQueueStatus statusQueue);

    List<ProcessingQueueEntry> findByStatusQueue(ProcessingQueueStatus statusQueue);
}
