package com.automation.webscraping.solar.monitor.service;

import com.automation.webscraping.solar.monitor.dto.ExcelClientCredentialsImportDTO;
import com.automation.webscraping.solar.monitor.model.Client;
import com.automation.webscraping.solar.monitor.spreadsheet.exceptions.ClientNotFoundException;
import com.automation.webscraping.solar.monitor.scraper.PortalScraper;
import com.automation.webscraping.solar.monitor.enums.Manufacturers;
import com.automation.webscraping.solar.monitor.spreadsheet.reader.ExcelClientCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class AutomationSerivce {

    private final ClientService clientService;
    private final List<PortalScraper> scrapers;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicInteger lastProcessedClientIndex = new AtomicInteger(-1);

    @Autowired
    private ExcelClientCredentials excelClientCredentials;

    @Autowired
    public AutomationSerivce(ClientService clientService, List<PortalScraper> scrapers) {
        this.clientService = clientService;
        this.scrapers = scrapers;
    }

    public void executeScraping() {
        if (!isRunning.compareAndSet(false, true)) {
            log.info("Iniciando processo de Scraping.");
            return;
        }

        try {
            isPaused.set(false);

            List<Client> clients;
            try {
                clients = clientService.findAll();
            } catch (ClientNotFoundException e) {
                log.error("Erro ao buscar clientes: {}", e.getMessage());
                isRunning.set(false);
                throw e;
            }

            int startIndex = Math.max(0, lastProcessedClientIndex.get() + 1);

            if (startIndex == 0) {
                lastProcessedClientIndex.set(-1);
                log.info("Consumindo lista completa de clientes.");
            }

            log.info("Consumindo lista de clientes a partir de: {}", startIndex);

            for (int i = startIndex; i < clients.size(); i++) {
                if (!isRunning.get()) {
                    log.info("Solicitação de Cancelamento.");
                    return;
                }

                if (isPaused.get()) {
                    log.info("Scraping pausado no cliente: {}", i - 1);
                    lastProcessedClientIndex.set(i - 1);
                    return;
                }

                Client client = clients.get(i);
                String manufacturerName = client.getInverterManufacturer().getName();

                // Testando só a Growatt
                if (!manufacturerName.equalsIgnoreCase(Manufacturers.GROWATT.name())) {
                    lastProcessedClientIndex.set(i);
                    continue;
                }

                scrapers.stream()
                        .filter(scraper -> scraper.isPortalAvailable(manufacturerName))
                        .findFirst()
                        .ifPresentOrElse(
                                scrapers -> scrapers.webScrapingService(client),
                                () -> log.info("Nenhum Scraper implementado para o Fabricante: {} \n", manufacturerName)
                        );

                if(isRunning.get()){
                    lastProcessedClientIndex.set(i);
                }else{
                    lastProcessedClientIndex.set(-1);
                }

            }

            log.info("Scraping process completed successfully");
        } finally {
            isRunning.set(false);
        }
    }

    public boolean cancelScraping() {
        if (isRunning.get()) {
            isRunning.set(false);
            isPaused.set(false);
            lastProcessedClientIndex.set(-1);
            log.info("Scraping cancelado.");
            return true;
        }
        log.info("Não há Scraping em andamento para cancelar.");
        return false;
    }

    public boolean pauseScraping() {
        if (isRunning.get() && !isPaused.get()) {
            isPaused.set(true);
            log.info("Scraping pausado.");
            return true;
        }
        log.info("Não há Scraping em andamento para pausar.");
        return false;
    }

    public boolean resumeScraping() {
        if (isPaused.get()) {
            isPaused.set(false);
            log.info("Reiniciando Scrapring a partir de: {}", lastProcessedClientIndex.get());
            executeScraping();
            return true;
        }
        log.info("Não há Scraping em andamento para continuar.");
        return false;
    }

    public String getScrapingStatus() {
        if (isRunning.get()) {
            if (isPaused.get()) {
                return "PAUSED";
            }
            return "RUNNING";
        }
        return "IDLE";
    }

    public ExcelClientCredentialsImportDTO consumeSpreadsheetClient(){
        File fileExcel = new File("C:/Users/usuario/Documents/SpreadsheetAutomation/Client_Credentials_By_Manufacturer.xlsx");
        ExcelClientCredentialsImportDTO importDTO = excelClientCredentials.readClientsCredentials(fileExcel);
        return importDTO;
    }

}
