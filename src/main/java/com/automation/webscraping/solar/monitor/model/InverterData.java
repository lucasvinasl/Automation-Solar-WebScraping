package com.automation.webscraping.solar.monitor.model;

import lombok.Getter;
import lombok.Setter;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class InverterData {
    private String serialNumber;
    private int year;
    private Map<Integer, Double> monthlyGeneration;
    private Double totalGeneration;

    public InverterData(String serialNumber, int year) {
        this.serialNumber = serialNumber;
        this.year = year;
        this.monthlyGeneration = new HashMap<>();
        for (int i = 1; i <= 12; i++) {
            this.monthlyGeneration.put(i, 0.0);
        }
        this.totalGeneration = 0.0;
    }

    @Override
    public String toString() {
        return "InverterData{" +
                "serialNumber='" + serialNumber + '\'' +
                ", year=" + year +
                ", monthlyGeneration=" + monthlyGeneration +
                ", totalGeneration=" + totalGeneration +
                '}';
    }
}