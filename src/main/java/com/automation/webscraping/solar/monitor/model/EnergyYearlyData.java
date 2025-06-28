package com.automation.webscraping.solar.monitor.model;

import com.automation.webscraping.solar.monitor.spreadsheet.processingqueue.ProcessingQueueEntry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class EnergyYearlyData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "january", nullable = false)
    private Double january;

    @Column(name = "february", nullable = false)
    private Double february;

    @Column(name = "march", nullable = false)
    private Double march;

    @Column(name = "april", nullable = false)
    private Double april;

    @Column(name = "may", nullable = false)
    private Double may;

    @Column(name = "june", nullable = false)
    private Double june;

    @Column(name = "july", nullable = false)
    private Double july;

    @Column(name = "august", nullable = false)
    private Double august;

    @Column(name = "september", nullable = false)
    private Double september;

    @Column(name = "october", nullable = false)
    private Double october;

    @Column(name = "november", nullable = false)
    private Double november;

    @Column(name = "december", nullable = false)
    private Double december;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "inverter_manufacturer_id", nullable = false)
    private InverterManufacturer inverterManufacturer;

    @ManyToOne
    @JoinColumn(name = "processing_queue_entry_id")
    private ProcessingQueueEntry processingQueueEntry;

    private String inverterSerialNumber;


}
