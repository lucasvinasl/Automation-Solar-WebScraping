package com.automation.webscraping.solar.monitor.spreadsheet.processingqueue;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "processing_queue_entry")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private ZonedDateTime insertedAt;

    private ZonedDateTime processedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingQueueStatus statusQueue;

    public ProcessingQueueEntry(String clientName, String filePath, String fileName) {
        this.clientName = clientName;
        this.filePath = filePath;
        this.fileName = fileName;
        this.insertedAt = ZonedDateTime.now(ZoneId.of("America/Fortaleza"));
        this.statusQueue = ProcessingQueueStatus.PENDING;
    }
}
