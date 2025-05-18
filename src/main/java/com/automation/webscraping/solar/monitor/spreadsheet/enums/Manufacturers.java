package com.automation.webscraping.solar.monitor.spreadsheet.enums;

public enum Manufacturers {
    GROWATT("Growatt", "https://server.growatt.com/?lang=pt"),
    SOLIS("Solis", "https://soliscloud.com/#/homepage"),
    SUNGROW("Sungrow", "https://isolarcloud.com/#/login");


    private String name;
    private String portalUrl;

    Manufacturers(String name, String portalUrl) {
        this.name = name;
        this.portalUrl = portalUrl;
    }

    public String getName() {
        return name;
    }

    public String getPortalUrl() {
        return portalUrl;
    }
}
