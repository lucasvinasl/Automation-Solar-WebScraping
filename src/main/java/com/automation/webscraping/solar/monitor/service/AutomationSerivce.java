package com.automation.webscraping.solar.monitor.service;

import com.automation.webscraping.solar.monitor.model.Client;
import com.automation.webscraping.solar.monitor.repository.ClientRepository;
import com.automation.webscraping.solar.monitor.scraper.PortalScraper;
import com.automation.webscraping.solar.monitor.enums.Manufacturers;
import com.automation.webscraping.solar.monitor.spreadsheet.reader.ExcelClientCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

import java.util.List;

@Slf4j
@Service
public class AutomationSerivce {

    private final ClientService clientService;
    private final List<PortalScraper> scrapers;

    @Autowired
    private ExcelClientCredentials excelClientCredentials;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    public AutomationSerivce(ClientService clientService, List<PortalScraper> scrapers) {
        this.clientService = clientService;
        this.scrapers = scrapers;
    }

    public void executeScraping() {
        List<Client> clients = clientService.findAll();
        for(Client client : clients){

            String manufacturerName = client.getInverterManufacturer().getName();

            // Testando só a Growatt
            if(!manufacturerName.equalsIgnoreCase(Manufacturers.GROWATT.name())){
                continue;
            }

            scrapers.stream()
                    .filter(scraper -> scraper.isPortalAvailable(manufacturerName))
                    .findFirst()
                    .ifPresentOrElse(
                            scrapers -> scrapers.webScrapingService(client),
                            () -> log.info("Nenhum Scraper implementado para o Fabricante: {} \n", manufacturerName)
                    );
        }
    }

    public void consumeSpreadsheetClient(){
        File fileExcel = new File("C:/Users/usuario/Documents/SpreadsheetAutomation/Client_Credentials_By_Manufacturer.xlsx");
        List<Client> clientList = excelClientCredentials.readClientsCredentials(fileExcel);
        clientRepository.saveAll(clientList);
    }

}
