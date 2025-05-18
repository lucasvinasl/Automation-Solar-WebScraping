package com.automation.webscraping.solar.monitor.scraper;

import com.automation.webscraping.solar.monitor.model.Client;

public interface PortalScraper {
    boolean isPortalAvailable(String manufacturerName);
    void webScrapingService(Client client);
}
