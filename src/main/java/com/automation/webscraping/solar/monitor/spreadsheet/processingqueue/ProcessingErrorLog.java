package com.automation.webscraping.solar.monitor.spreadsheet.processingqueue;

import jakarta.persistence.*; // Ou javax.persistence
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "processing_error_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processing_queue_entry_id", nullable = false)
    private ProcessingQueueEntry queueEntry;

    @Column(nullable = false)
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    @Column(nullable = false)
    private ZonedDateTime errorTimestamp;

    public ProcessingErrorLog(ProcessingQueueEntry queueEntry, String errorMessage, String stackTrace) {
        this.queueEntry = queueEntry;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.errorTimestamp = ZonedDateTime.now(ZoneId.of("America/Fortaleza"));
    }
}