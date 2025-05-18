package com.automation.webscraping.solar.monitor.scraper.solis;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class SolisElementMap {

    public WebElement usernameInput;
    public WebElement passwordInput;
    public WebElement checkBox;
    public WebElement loginButton;

    public void waitAndMap(WebDriver driver, Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);

        this.usernameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@placeholder='Preencha o e-mail ou nome de utilizador']")));
        this.passwordInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[contains(@placeholder, 'Inserir')]")));
        this.checkBox = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[contains(text(), 'concordei')]")));
        this.loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='Login']")));
    }
}
