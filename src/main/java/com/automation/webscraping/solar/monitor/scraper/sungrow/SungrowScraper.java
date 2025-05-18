package com.automation.webscraping.solar.monitor.scraper.sungrow;

import com.automation.webscraping.solar.monitor.model.Client;
import com.automation.webscraping.solar.monitor.scraper.PortalScraper;
import com.automation.webscraping.solar.monitor.scraper.growatt.GrowattElementMap;
import com.automation.webscraping.solar.monitor.spreadsheet.enums.Manufacturers;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class SungrowScraper implements PortalScraper {

    @Override
    public boolean isPortalAvailable(String manufacturerName) {
        return Manufacturers.SUNGROW.name().equalsIgnoreCase(manufacturerName);
    }

    @Override
    public void webScrapingService(Client client) {
        WebDriver driver = new ChromeDriver();
        String url = client.getInverterManufacturer().getPortalUrl();
        try {
            driver.get(url);
            driver.manage().window().maximize();
            SungrowElementMap webElementMapped = new SungrowElementMap();
            webElementMapped.waitAndMap(driver, Duration.ofSeconds(5));

            webElementMapped.usernameInput.sendKeys(client.getUsername());
            webElementMapped.passwordInput.sendKeys(client.getPassword());

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", webElementMapped.loginButton);
            Thread.sleep(2000);
            webElementMapped.checkBox.click();
            Thread.sleep(1000);
            webElementMapped.loginButton.click();
            Thread.sleep(2000);

            log.info("Login efetuado com sucesso! \n Cliente: {} - Portal: {}", client.getName(), client.getInverterManufacturer().getName());
            //driver.quit();
        } catch (Exception e) {
            log.error("Erro ao tentar acessar portal da {}: {}", client.getInverterManufacturer().getName(), e.getMessage());
            throw new RuntimeException(
                    String.format("Erro ao tentar acessar portal da %s : %s", client.getInverterManufacturer().getName(), e.getMessage()));
        }
    }
}
