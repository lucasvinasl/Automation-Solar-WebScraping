package com.automation.webscraping.solar.monitor.controller;

import com.automation.webscraping.solar.monitor.dto.ExcelClientCredentialsImportDTO;
import com.automation.webscraping.solar.monitor.service.AutomationSerivce;
import com.automation.webscraping.solar.monitor.spreadsheet.exceptions.ClientNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/automation")
public class AutomationController {

    @Autowired
    private AutomationSerivce automationSerivce;

    @PostMapping("/initializer")
    public ResponseEntity<Map<String, String>> executeScrapers(){
        try {
            automationSerivce.executeScraping();
            Map<String, String> response = new HashMap<>();
            response.put("status", "Scraping iniciado.");
            return ResponseEntity.ok().body(response);
        } catch (ClientNotFoundException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            log.error("Erro ao iniciar o processo de scraping: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @PostMapping("/update-spreadsheet")
    public ResponseEntity<ExcelClientCredentialsImportDTO> consumeSpreadsheetClient(){
        return ResponseEntity.ok().body(automationSerivce.consumeSpreadsheetClient());
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, String>> cancelScraping() {
        boolean cancelled = automationSerivce.cancelScraping();
        Map<String, String> response = new HashMap<>();
        if (cancelled) {
            response.put("status", "Scraping cancelado.");
        } else {
            response.put("status", "Não há scrapign em andamento para cancelar.");
        }
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/pause")
    public ResponseEntity<Map<String, String>> pauseScraping() {
        boolean paused = automationSerivce.pauseScraping();
        Map<String, String> response = new HashMap<>();
        if (paused) {
            response.put("status", "Scraping pausado.");
        } else {
            response.put("status", "Não há scrapign em andamento para pausar.");
        }
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/resume")
    public ResponseEntity<Map<String, String>> resumeScraping() {
        boolean resumed = automationSerivce.resumeScraping();
        Map<String, String> response = new HashMap<>();
        if (resumed) {
            response.put("status", "Scraping reiniciado.");
        } else {
            response.put("status", "Não há scrapign em andamento para reiniciar.");
        }
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getScrapingStatus() {
        String status = automationSerivce.getScrapingStatus();
        Map<String, String> response = new HashMap<>();
        response.put("status", status);
        return ResponseEntity.ok().body(response);
    }
}
