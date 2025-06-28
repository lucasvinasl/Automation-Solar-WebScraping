package com.automation.webscraping.solar.monitor.scraper.growatt;

import com.automation.webscraping.solar.monitor.mapper.GrowattMapper;
import com.automation.webscraping.solar.monitor.model.Client;
import com.automation.webscraping.solar.monitor.model.EnergyYearlyData;
import com.automation.webscraping.solar.monitor.repository.EnergyYearlyDataRepository;
import com.automation.webscraping.solar.monitor.scraper.PortalScraper;
import com.automation.webscraping.solar.monitor.enums.Manufacturers;
import com.automation.webscraping.solar.monitor.spreadsheet.processingqueue.ProcessingQueueEntry;
import com.automation.webscraping.solar.monitor.spreadsheet.processingqueue.ProcessingQueueEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v85.network.Network;
import org.openqa.selenium.devtools.v85.network.model.RequestId;
import org.openqa.selenium.devtools.v85.network.model.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class GrowattScraper implements PortalScraper {

    private static final String BASE_DOWNLOAD_PATH = System.getProperty("user.home") + File.separator + "Documents"
            + File.separator + "SpreadsheetAutomation" + File.separator + "growatt_spreadsheets";
    private static final ZoneId LOCAL_ZONE_ID = ZoneId.of("America/Fortaleza");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    @Autowired
    private EnergyYearlyDataRepository energyYearlyDataRepository;

    @Autowired
    private ProcessingQueueEntryRepository processingQueueEntryRepository;

    @Override
    public boolean isPortalAvailable(String manufacturerName) {
        return Manufacturers.GROWATT.name().equalsIgnoreCase(manufacturerName);
    }

    @Override
    public void webScrapingService(Client client) {
        String clientDownloadDirectory =  BASE_DOWNLOAD_PATH + File.separator + sanitizeClientName(client.getName());
        createDirectoryIfNotExists(clientDownloadDirectory);

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", clientDownloadDirectory);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safeBrowse.enabled", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);
        String url = client.getInverterManufacturer().getPortalUrl();
        try{
            driver.get(url);
            driver.manage().window().maximize();
            GrowattElementMap webElementMapped = new GrowattElementMap();

            login(client, driver, webElementMapped);

            try{
                plantsLits(driver, webElementMapped);
            }catch (RuntimeException e){
                log.info("Cliente não possui mais de uma planta: {}", e.getMessage());
            }

            clickOnEnergy(driver, webElementMapped);
            Thread.sleep(1000);
            clickOnEnergyMonth(driver, webElementMapped);
            Thread.sleep(1000);
            clickOnExport(driver, webElementMapped);
            Thread.sleep(1000);
            clickOnAnnualReport(driver, webElementMapped);
            Thread.sleep(1000);
            File downloadedFile = renameDownloadedFileSimple(clientDownloadDirectory, Duration.ofSeconds(60));

            if(downloadedFile != null){
                ProcessingQueueEntry newEntry = new ProcessingQueueEntry(
                        client,
                        downloadedFile.getAbsolutePath(),
                        downloadedFile.getName()
                );
                processingQueueEntryRepository.save(newEntry);
                log.info("Planilha '{}' adicionada à fila de processamento para o cliente '{}'. ID da fila: {}",
                        newEntry.getFileName(), newEntry.getClient().getName(), newEntry.getId());
            } else {
                log.error("Download e renomeação da planilha falharam para o cliente: {}", client.getName());
            }

            //driver.quit();
        }catch (Exception e){
            log.error("Erro ao tentar acessar portal da {}: {}", client.getInverterManufacturer().getName(), e.getMessage());
            throw new RuntimeException(
                    String.format("Erro ao tentar acessar portal da %s : %s", client.getInverterManufacturer().getName(), e.getMessage()));
        }

    }

    private String sanitizeClientName(String name){
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private void createDirectoryIfNotExists(String directoryPath){
        Path path = Paths.get(directoryPath);
        try{
            if(!Files.exists(path)){
                Files.createDirectories(path);
                log.info("Pasta Criada: {} ", directoryPath);
            }else{
                log.info("Pasta já Existe: {} ", directoryPath);
            }

        }catch (IOException e){
            log.error("Erro ao criar o diretório de download {}: {}", directoryPath, e.getMessage());
            throw new RuntimeException("Não foi possível preparar o diretório de download.", e);
        }
    }

    /**
     * Espera pelo arquivo mais recente baixado no diretório de download
     * e adiciona a data/hora atual ao seu nome original (antes da extensão).
     * @param downloadDirectory Caminho do diretório de download.
     * @param timeout Tempo máximo de espera.
     * @return O arquivo renomeado, ou null se não for encontrado dentro do timeout.
     */
    private File renameDownloadedFileSimple(String downloadDirectory, Duration timeout) {
        long endTime = System.currentTimeMillis() + timeout.toMillis();
        File downloadedFile = null;

        while (System.currentTimeMillis() < endTime) {
            File dir = new File(downloadDirectory);
            // Filtra por arquivos que não são downloads incompletos
            File[] files = dir.listFiles((d, name) -> !name.endsWith(".crdownload") && !name.endsWith(".tmp"));

            if (files != null && files.length > 0) {
                // Encontra o arquivo mais recentemente modificado (o que acabamos de baixar)
                downloadedFile = Arrays.stream(files)
                        .max(Comparator.comparingLong(File::lastModified))
                        .orElse(null);

                if (downloadedFile != null) {
                    String originalFullName = downloadedFile.getName();
                    String fileExtension = "";
                    String fileNameWithoutExtension = originalFullName;

                    // Separa nome e extensão
                    int dotIndex = originalFullName.lastIndexOf('.');
                    if (dotIndex > 0 && dotIndex < originalFullName.length() - 1) {
                        fileExtension = originalFullName.substring(dotIndex);
                        fileNameWithoutExtension = originalFullName.substring(0, dotIndex);
                    }

                    // Gera o timestamp atual
                    String timestamp = ZonedDateTime.now(LOCAL_ZONE_ID).format(DATE_TIME_FORMATTER);

                    // Novo nome do arquivo: [NomeOriginalSemExtensao]_[Timestamp].[ExtensaoOriginal]
                    String newFileName = String.format("%s_%s%s",
                            fileNameWithoutExtension,
                            timestamp,
                            fileExtension);

                    Path newFilePath = Paths.get(downloadDirectory, newFileName);

                    try {
                        Files.move(downloadedFile.toPath(), newFilePath, StandardCopyOption.REPLACE_EXISTING);
                        log.info("Arquivo baixado renomeado de '{}' para '{}'", originalFullName, newFileName);
                        return newFilePath.toFile();
                    } catch (IOException e) {
                        log.error("Erro ao renomear o arquivo baixado '{}': {}", originalFullName, e.getMessage());
                        return null;
                    }
                }
            }
            try {
                Thread.sleep(500); // Pequena pausa para o SO atualizar a lista de arquivos
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupção durante a espera pelo download do arquivo.");
                break;
            }
        }
        log.error("Nenhum arquivo baixado encontrado no diretório '{}' após {} segundos.", downloadDirectory, timeout.getSeconds());
        return null;
    }


    private void login(Client client, WebDriver driver, GrowattElementMap webElementMapped) throws InterruptedException {
        webElementMapped.waitAndMapLoginElements(driver, Duration.ofSeconds(5));

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

    private void clickOnEnergy(WebDriver driver, GrowattElementMap webElementMapped){
        webElementMapped.waitAndMapEnergyButton(driver, Duration.ofSeconds(5));
        webElementMapped.energyButton.click();
    }

    private void clickOnEnergyMonth(WebDriver driver, GrowattElementMap webElementMapped){
        webElementMapped.waitAndMapEnergyButtonMonth(driver, Duration.ofSeconds(5));
        webElementMapped.energyButtonMonth.click();
    }

    private void clickOnExport(WebDriver driver, GrowattElementMap webElementMapped){
        webElementMapped.waitAndMapExportButton(driver, Duration.ofSeconds(5));
        webElementMapped.exportButton.click();
    }

    private void clickOnAnnualReport(WebDriver driver, GrowattElementMap webElementMapped){
        webElementMapped.waitAndMapAnnualReport(driver, Duration.ofSeconds(5));
        webElementMapped.annualReportOption.click();
    }
}
