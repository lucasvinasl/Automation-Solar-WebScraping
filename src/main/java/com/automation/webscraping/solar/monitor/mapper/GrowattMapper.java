package com.automation.webscraping.solar.monitor.mapper;

import com.automation.webscraping.solar.monitor.enums.Months;
import com.automation.webscraping.solar.monitor.model.Client;
import com.automation.webscraping.solar.monitor.model.EnergyYearlyData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GrowattMapper {
    public static EnergyYearlyData toYearlyData(String bodyString, Client client, int year) {
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(bodyString);

            JsonNode energyArray = root
                    .path("obj")
                    .get(0)
                    .path("datas")
                    .path("energy");

            if(!energyArray.isArray() || energyArray.size() != 12){
                throw new IllegalArgumentException("JSON inválido: vetor 'energy' precisa ter 12 meses");
            }

            EnergyYearlyData energyYearlyData = new EnergyYearlyData();
            energyYearlyData.setYear(year);
            energyYearlyData.setClient(client);

            for(int i = 0; i < 12; i++){
                double energyValue = energyArray.get(i).asDouble();
                switch (Months.fromNumber(i+1)){
                    case JAN -> energyYearlyData.setJanuary(energyValue);
                    case FEV -> energyYearlyData.setFebruary(energyValue);
//                    case MAR -> energyYearlyData.setMarch(energyValue);
//                    case ABR -> energyYearlyData.setApril(energyValue);
//                    case MAI -> energyYearlyData.setMay(energyValue);
//                    case JUN -> energyYearlyData.setJune(energyValue);
//                    case JUL -> energyYearlyData.setJuly(energyValue);
//                    case AGO -> energyYearlyData.setAugust(energyValue);
//                    case SET -> energyYearlyData.setSeptember(energyValue);
//                    case OUT -> energyYearlyData.setOctober(energyValue);
//                    case NOV -> energyYearlyData.setNovember(energyValue);
//                    case DEZ -> energyYearlyData.setDecember(energyValue);
                }
            }

            return energyYearlyData;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao mapear JSON para EnergyYearlyData: " + e.getMessage(), e);
        }
    }
}
