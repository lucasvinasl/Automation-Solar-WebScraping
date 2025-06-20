package com.automation.webscraping.solar.monitor.controller;

import com.automation.webscraping.solar.monitor.service.AutomationSerivce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/automation")
public class AutomationController {

    @Autowired
    private AutomationSerivce automationSerivce;

    @PostMapping("/initializer")
    public void executeScrapers(){
        automationSerivce.executeScraping();
    }

    @PostMapping("/update-spreadsheet")
    public void consumeSpreadsheetClient(){
        automationSerivce.consumeSpreadsheetClient();
    }

}
