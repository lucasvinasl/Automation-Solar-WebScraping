package com.automation.webscraping.solar.monitor.scraper.growatt;

import com.automation.webscraping.solar.monitor.enums.Months;
import com.automation.webscraping.solar.monitor.mapper.GrowattMapper;
import com.automation.webscraping.solar.monitor.model.Client;
import com.automation.webscraping.solar.monitor.model.EnergyYearlyData;
import com.automation.webscraping.solar.monitor.repository.EnergyYearlyDataRepository;
import com.automation.webscraping.solar.monitor.scraper.PortalScraper;
import com.automation.webscraping.solar.monitor.enums.Manufacturers;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v85.network.Network;
import org.openqa.selenium.devtools.v85.network.model.RequestId;
import org.openqa.selenium.devtools.v85.network.model.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class GrowattScraper implements PortalScraper {

    @Autowired
    private EnergyYearlyDataRepository energyYearlyDataRepository;

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

            clickOnEnergy(driver, webElementMapped);
            Thread.sleep(1000);
            setEnergyMonthly(driver, webElementMapped, client);
            //driver.quit();
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

    /* Vai habilitar a devtools, verificar todas as respostas de endpoints recebidas, encontrar a que eu preciso e
    mapear o json especificado.
     */
    public void setEnergyMonthly(WebDriver driver, GrowattElementMap webElementMapped, Client client) {
        DevTools devTools = ((ChromeDriver) driver).getDevTools();
        devTools.createSession();
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        AtomicBoolean processed = new AtomicBoolean(false);

        devTools.addListener(Network.responseReceived(), response -> {
            if (processed.get()) return;
            Response res = response.getResponse();
            String url = res.getUrl();

            if (url.contains("getDevicesYearChart")) {
                processed.set(true);

                RequestId requestId = response.getRequestId();
                Network.GetResponseBodyResponse body = devTools.send(Network.getResponseBody(requestId));
                if (body != null) {
                    String bodyString = body.getBody();
                    log.info("📦 JSON capturado: {}", bodyString);

                    int currentYear = Integer.parseInt(Objects.requireNonNull(driver.findElement(
                                    By.xpath("//input[@id='val_energy_compare_Time' and @data-max]"))
                            .getAttribute("data-max")));
                    log.info("Ano atual: {}", currentYear);
                    EnergyYearlyData energyYearlyData = GrowattMapper.toYearlyData(bodyString, client, currentYear);
                    energyYearlyData.setInverterManufacturer(client.getInverterManufacturer());
                    energyYearlyDataRepository.save(energyYearlyData);
                }
            }
        });

        clickOnEnergyMonth(driver, webElementMapped);
    }

    private void clickOnEnergy(WebDriver driver, GrowattElementMap webElementMapped){
        webElementMapped.waitAndMapEnergyButton(driver, Duration.ofSeconds(5));
        webElementMapped.energyButton.click();
    }

    private void clickOnEnergyMonth(WebDriver driver, GrowattElementMap webElementMapped){
        webElementMapped.waitAndMapEnergyButtonMonth(driver, Duration.ofSeconds(5));
        webElementMapped.energyButtonMonth.click();
    }
}
