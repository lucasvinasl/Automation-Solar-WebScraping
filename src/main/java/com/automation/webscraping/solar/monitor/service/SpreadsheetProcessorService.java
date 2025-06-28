package com.automation.webscraping.solar.monitor.service;


import com.automation.webscraping.solar.monitor.enums.Manufacturers;
import com.automation.webscraping.solar.monitor.model.EnergyYearlyData;
import com.automation.webscraping.solar.monitor.model.InverterData;
import com.automation.webscraping.solar.monitor.model.InverterManufacturer;
import com.automation.webscraping.solar.monitor.repository.EnergyYearlyDataRepository;
import com.automation.webscraping.solar.monitor.spreadsheet.processingqueue.*;
import com.automation.webscraping.solar.monitor.spreadsheet.reader.GrowattSpreadsheetReader;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@EnableScheduling
public class SpreadsheetProcessorService {

    @Autowired
    private ProcessingQueueEntryRepository processingQueueEntryRepository;

    @Autowired
    private ProcessingErrorLogRepository processingErrorLogRepository;

    @Autowired
    private GrowattSpreadsheetReader growattSpreadsheetReader;

    @Autowired
    private EnergyYearlyDataRepository energyYearlyDataRepository;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void scheduleSpreadsheetProcessing() {
        log.debug("Verificando planilhas PENDING para processamento...");

        List<ProcessingQueueEntry> pendingEntries = processingQueueEntryRepository.findTop10ByStatusQueueOrderByInsertedAtAsc(ProcessingQueueStatus.PENDING);

        if (pendingEntries.isEmpty()) {
            log.debug("Nenhuma planilha PENDING encontrada no momento.");
            return;
        }
        log.info("Encontradas {} planilhas PENDING para processar.", pendingEntries.size());

        for (ProcessingQueueEntry entry : pendingEntries) {
            try {
                Optional<ProcessingQueueEntry> currentEntryOpt = processingQueueEntryRepository.findById(entry.getId());
                if (currentEntryOpt.isPresent() && currentEntryOpt.get().getStatusQueue() == ProcessingQueueStatus.PENDING) {
                    ProcessingQueueEntry currentEntry = currentEntryOpt.get();
                    currentEntry.setStatusQueue(ProcessingQueueStatus.IN_PROGRESS);
                    processingQueueEntryRepository.save(currentEntry);

                    processSpreadsheetAsync(currentEntry.getId());
                } else {
                    log.warn("Planilha (ID: {}) já está sendo processada ou tem outro status. Ignorando no ciclo atual.", entry.getId());
                }
            } catch (Exception e) {
                log.error("Erro ao tentar mudar o status da planilha (ID: {}) para IN_PROGRESS: {}", entry.getId(), e.getMessage(), e);
            }
        }
    }

    @Async("spreadsheetTaskExecutor")
    @Transactional
    public void processSpreadsheetAsync(Long entryId) {
        ProcessingQueueEntry entry = null;
        try {
            Optional<ProcessingQueueEntry> optionalEntry = processingQueueEntryRepository.findById(entryId);
            if (optionalEntry.isEmpty()) {
                log.warn("Entrada da fila com ID {} não encontrada ou já removida. Pode ter sido processada por outra thread.", entryId);
                return;
            }
            entry = optionalEntry.get();

            if (entry.getStatusQueue() != ProcessingQueueStatus.IN_PROGRESS) {
                log.warn("Planilha (ID: {}) não está no status IN_PROGRESS como esperado. Status atual: {}. Ignorando processamento real.", entryId, entry.getStatusQueue());
                return;
            }

            log.info("Iniciando processamento REAL da planilha '{}' (ID: {}) para o cliente '{}'.",
                    entry.getFileName(), entry.getId(), entry.getClient().getName());

            File excelFile = new File(entry.getFilePath());
            if(excelFile.exists()){
                if(optionalEntry.get().getClient().getInverterManufacturer().getName().equalsIgnoreCase(Manufacturers.GROWATT.getName())){
                    log.info("Lendo a planilha Growatt.");
                    List<InverterData> inverterDataGrowatt = growattSpreadsheetReader.extractEnergyDataFromSpreadsheet(excelFile);
                    if (inverterDataGrowatt != null && !inverterDataGrowatt.isEmpty()) {
                        log.info("Salvando os registros da planilha no banco para {} inversores.", inverterDataGrowatt.size());
                        populateEnergyData(inverterDataGrowatt, entry);
                    } else {
                        log.warn("Nenhum dado de energia extraído da planilha para o cliente '{}'.", entry.getClient().getName());
                    }
                }
            }

            entry.setStatusQueue(ProcessingQueueStatus.COMPLETED);
            entry.setProcessedAt(ZonedDateTime.now(ZoneId.of("America/Fortaleza")));
            processingQueueEntryRepository.save(entry);
            log.info("Planilha '{}' (ID: {}) processada com sucesso. Status: COMPLETED",
                    entry.getFileName(), entry.getId());

        } catch (Exception e) {
            log.error("Erro inesperado ao processar planilha (ID: {}): {}", entryId, e.getMessage(), e);
            if (entry != null) {
                handleProcessingError(entry, "Erro no processamento: " + e.getMessage(), getStackTrace(e));
            }
        }
    }

    @Transactional
    protected void handleProcessingError(ProcessingQueueEntry entry, String errorMessage, String stackTrace) {
        entry.setStatusQueue(ProcessingQueueStatus.ERROR);
        processingQueueEntryRepository.save(entry);
        ProcessingErrorLog errorLog = new ProcessingErrorLog(entry, errorMessage, stackTrace);
        processingErrorLogRepository.save(errorLog);
        log.error("Planilha '{}' (ID: {}) marcada como ERRO. Mensagem: {}",
                entry.getFileName(), entry.getId(), errorMessage);
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private void populateEnergyData(List<InverterData> allInverters, ProcessingQueueEntry entry) {

        for (InverterData inverterData : allInverters) {

            Optional<EnergyYearlyData> energyYearlyData = energyYearlyDataRepository
                    .findOneByYearAndClientIdAndInverterSerial(inverterData.getYear(),
                            entry.getClient().getId(),inverterData.getSerialNumber());

            if(energyYearlyData.isPresent()){
                log.info("Registro do inversor {} já existe, atualizando... ", inverterData.getSerialNumber());
                for (Map.Entry<Integer, Double> monthEntry : inverterData.getMonthlyGeneration().entrySet()) {
                    int month = monthEntry.getKey();
                    double value = monthEntry.getValue();
                    if (month >= 1 && month <= 12) {
                        switch (month) {
                            case 1 ->{
                                if(!energyYearlyData.get().getJanuary().equals(value)){
                                    log.info("Janeiro está diferente, atualizando.");
                                    energyYearlyData.get().setJanuary(value);
                                }
                            }
                            case 2 -> {
                                if(!energyYearlyData.get().getFebruary().equals(value)){
                                    log.info("Fevereiro está diferente, atualizando.");
                                    energyYearlyData.get().setFebruary(value);
                                }
                            }
                            case 3 -> {
                                if(!energyYearlyData.get().getMarch().equals(value)){
                                    log.info("Março está diferente, atualizando.");
                                    energyYearlyData.get().setMarch(value);
                                }
                            }
                            case 4 -> {
                                if(!energyYearlyData.get().getApril().equals(value)){
                                    log.info("Abril está diferente, atualizando.");
                                    energyYearlyData.get().setApril(value);
                                }
                            }
                            case 5 -> {
                                if(!energyYearlyData.get().getMay().equals(value)){
                                    log.info("Maio está diferente, atualizando.");
                                    energyYearlyData.get().setMay(value);
                                }
                            }
                            case 6 -> {
                                if(!energyYearlyData.get().getJune().equals(value)){
                                    log.info("Junho está diferente, atualizando.");
                                    energyYearlyData.get().setJuly(value);
                                }
                            }
                            case 7 -> {
                                if(!energyYearlyData.get().getJuly().equals(value)){
                                    log.info("Julho está diferente, atualizando.");
                                    energyYearlyData.get().setJuly(value);
                                }
                            }
                            case 8 -> {
                                if(!energyYearlyData.get().getAugust().equals(value)){
                                    log.info("Agosto está diferente, atualizando.");
                                    energyYearlyData.get().setAugust(value);
                                }
                            }
                            case 9 -> {
                                if(!energyYearlyData.get().getSeptember().equals(value)){
                                    log.info("Setembro está diferente, atualizando.");
                                    energyYearlyData.get().setSeptember(value);
                                }
                            }
                            case 10 -> {
                                if(!energyYearlyData.get().getOctober().equals(value)){
                                    log.info("Outubro está diferente, atualizando.");
                                    energyYearlyData.get().setOctober(value);
                                }
                            }
                            case 11 -> {
                                if(!energyYearlyData.get().getNovember().equals(value)){
                                    log.info("Novembro está diferente, atualizando.");
                                    energyYearlyData.get().setNovember(value);
                                }
                            }
                            case 12 -> {
                                if(!energyYearlyData.get().getDecember().equals(value)){
                                    log.info("Dezembro está diferente, atualizando.");
                                    energyYearlyData.get().setDecember(value);
                                }
                            }
                        }
                    }
                }
                InverterManufacturer manufacturer = entry.getClient().getInverterManufacturer();
                energyYearlyData.get().setInverterManufacturer(manufacturer);
                energyYearlyData.get().setProcessingQueueEntry(entry);
                energyYearlyDataRepository.save(energyYearlyData.get());
                log.info("Saved energy data for year {} for client {}",
                        energyYearlyData.get().getYear(), entry.getClient().getName());
            }else {
                EnergyYearlyData newEnergyYearlyData = new EnergyYearlyData();
                newEnergyYearlyData.setYear(inverterData.getYear());
                newEnergyYearlyData.setClient(entry.getClient());
                newEnergyYearlyData.setInverterManufacturer(entry.getClient().getInverterManufacturer());
                for (Map.Entry<Integer, Double> monthEntry : inverterData.getMonthlyGeneration().entrySet()) {
                    int month = monthEntry.getKey();
                    double value = monthEntry.getValue();
                    if (month >= 1 && month <= 12) {
                        switch (month) {
                            case 1 -> newEnergyYearlyData.setJanuary(value);
                            case 2 -> newEnergyYearlyData.setFebruary(value);
                            case 3 -> newEnergyYearlyData.setMarch(value);
                            case 4 -> newEnergyYearlyData.setApril(value);
                            case 5 -> newEnergyYearlyData.setMay(value);
                            case 6 -> newEnergyYearlyData.setJune(value);
                            case 7 -> newEnergyYearlyData.setJuly(value);
                            case 8 -> newEnergyYearlyData.setAugust(value);
                            case 9 -> newEnergyYearlyData.setSeptember(value);
                            case 10 -> newEnergyYearlyData.setOctober(value);
                            case 11 -> newEnergyYearlyData.setNovember(value);
                            case 12 -> newEnergyYearlyData.setDecember(value);
                        }
                    }
                }
                InverterManufacturer manufacturer = entry.getClient().getInverterManufacturer();
                newEnergyYearlyData.setInverterManufacturer(manufacturer);
                newEnergyYearlyData.setProcessingQueueEntry(entry);
                newEnergyYearlyData.setInverterSerialNumber(inverterData.getSerialNumber());
                energyYearlyDataRepository.save(newEnergyYearlyData);
                log.info("Saved energy data for year {} for client {}",
                        newEnergyYearlyData.getYear(), entry.getClient().getName());
            }
        }
    }
}
