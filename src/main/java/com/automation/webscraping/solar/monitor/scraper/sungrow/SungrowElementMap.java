package com.automation.webscraping.solar.monitor.scraper.sungrow;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class SungrowElementMap {

    public WebElement usernameInput;
    public WebElement passwordInput;
    public WebElement loginButton;
    public WebElement checkBox;

    public void waitAndMap(WebDriver driver, Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);

        this.usernameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@placeholder='Conta']")));
        this.passwordInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@placeholder='Senha']")));
        this.checkBox = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='Li e concordo com ']")));
        this.loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='Entrar']")));
    }

}
