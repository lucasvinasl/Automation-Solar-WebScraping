package com.automation.webscraping.solar.monitor.spreadsheet.exceptions;

public class InvalidSpreadsheetCellValue extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidSpreadsheetCellValue(String message) {
        super(message);
    }
}
