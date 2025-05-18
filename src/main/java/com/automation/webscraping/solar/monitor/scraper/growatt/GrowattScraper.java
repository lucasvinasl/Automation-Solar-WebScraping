package com.automation.webscraping.solar.monitor.scraper.growatt;

import com.automation.webscraping.solar.monitor.model.Client;
import com.automation.webscraping.solar.monitor.scraper.PortalScraper;
import com.automation.webscraping.solar.monitor.spreadsheet.enums.Manufacturers;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class GrowattScraper implements PortalScraper {

    @Override
    public boolean isPortalAvailable(String manufacturerName) {
        return Manufacturers.GROWATT.name().equalsIgnoreCase(manufacturerName);
    }

    @Override
    public void webScrapingService(Client client) {
        WebDriver driver = new ChromeDriver();
        String url = client.getInverterManufacturer().getPortalUrl();
        try{
            driver.get(url);
            driver.manage().window().maximize();
            GrowattElementMap webElementMapped = new GrowattElementMap();

            login(client, driver, webElementMapped);

            try{
                plantsLits(driver, webElementMapped);
            }catch (RuntimeException e){
                log.info("Cliente não possui condição de mais de uma planta: {}", e.getMessage());
            }


            driver.quit();
        }catch (Exception e){
            log.error("Erro ao tentar acessar portal da {}: {}", client.getInverterManufacturer().getName(), e.getMessage());
            throw new RuntimeException(
                    String.format("Erro ao tentar acessar portal da %s : %s", client.getInverterManufacturer().getName(), e.getMessage()));
        }

    }

    private void login(Client client, WebDriver driver, GrowattElementMap webElementMapped) throws InterruptedException {
        webElementMapped.waitAndMaoLoginElements(driver, Duration.ofSeconds(5));

        webElementMapped.usernameInput.sendKeys(client.getUsername());
        webElementMapped.passwordInput.sendKeys(client.getPassword());

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", webElementMapped.loginButton);
        Thread.sleep(2000);
        webElementMapped.loginButton.click();
        Thread.sleep(2000);

        log.info("Login efetuado com sucesso! \n Cliente: {} - Portal: {}", client.getName(), client.getInverterManufacturer().getName());
    }

    // verificar se tem mais de uma planta ou mais de um inversor no caso de Ethan
    public List<WebElement> plantsLits(WebDriver driver, GrowattElementMap webElementMapped){

        webElementMapped.waitAndMapPlantListVerify(driver, Duration.ofSeconds(5));

        //List<WebElement> plants = driver.findElements(By.xpath("//div[@id='selectPlant-con']/ul/li"));
        List<WebElement> plants = webElementMapped.selectPlant.findElements(By.xpath(".//li"));
        //List<WebElement> plants = webElementMapped.selectPlant.findElements(By.tagName("li"));
        if(plants.size() > 1){
            for(WebElement plant : plants){
                log.info("Planta encontrada: {}", plant.getText());
            }
        }else if(plants.size() == 1){
            log.warn("Apenas uma planta encontrada ou nenhuma.");
        }else{
            return null;
        }

        return plants;
    }
}
