package com.automation.webscraping.solar.monitor.scraper.growatt;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class GrowattElementMap {

    public WebElement usernameInput;
    public WebElement passwordInput;
    public WebElement loginButton;
    public WebElement selectPlant;
    public WebElement energyButton;
    public WebElement energyButtonMonth;
    public WebElement currentEnergyYear;
    public WebElement exportButton;
    public WebElement annualReportOption;

    public void waitAndMapLoginElements(WebDriver driver, Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);

        this.usernameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@placeholder='Usuário']")));
        this.passwordInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@placeholder='Senha']")));
        this.loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class, 'loginB')]")));

    }

    //caso de Ethan
    public void waitAndMapPlantListVerify(WebDriver driver, Duration timeout){
        WebDriverWait wait = new WebDriverWait(driver, timeout);

        this.selectPlant = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='selectPlant-con']")));
    }

    public void waitAndMapEnergyButton(WebDriver driver, Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        this.energyButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='Energia']")));
    }

    public void waitAndMapEnergyButtonMonth(WebDriver driver, Duration timeout){
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        this.energyButtonMonth = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//i[@data-val='2' and contains(@class, 'btn_energy_compare_timeType') and text()='MES']")));

        this.currentEnergyYear = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@id='val_energy_compare_Time' and @data-max]")));
    }

    public void waitAndMapExportButton(WebDriver driver, Duration timeout){
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        this.exportButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@value='Exportar']")));
    }

    public void waitAndMapAnnualReport(WebDriver driver, Duration timeout){
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        this.annualReportOption = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//dd[@title='Relatório anual']")));
    }

}
