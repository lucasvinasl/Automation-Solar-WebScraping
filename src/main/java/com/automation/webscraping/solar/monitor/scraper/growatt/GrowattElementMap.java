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

    public void waitAndMaoLoginElements(WebDriver driver, Duration timeout) {
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


}
