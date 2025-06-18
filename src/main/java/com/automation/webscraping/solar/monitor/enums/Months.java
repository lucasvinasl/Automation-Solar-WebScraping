package com.automation.webscraping.solar.monitor.enums;

public enum Months {
    JAN("Janeiro", "January", 1),
    FEV("Fevereiro", "February", 2),
    MAR("Março", "March", 3),
    ABR("Abril", "April", 4),
    MAI("Maio", "May", 5),
    JUN("Junho", "June", 6),
    JUL("Julho", "July", 7),
    AGO("Agosto", "August", 8),
    SET("Setembro", "September", 9),
    OUT("Outubro", "October", 10),
    NOV("Novembro", "November", 11),
    DEZ("Dezembro", "December", 12);

    private final String namePt;
    private final String nameEn;
    private final int monthNumber;

    Months(String namePt, String nameEn, int monthNumber) {
        this.namePt = namePt;
        this.nameEn = nameEn;
        this.monthNumber = monthNumber;
    }

    public String getNamePt() {
        return namePt;
    }

    public String getNameEn() {
        return nameEn;
    }

    public int getMonthNumber() {
        return monthNumber;
    }

    public static Months fromNumber(int number) {
        for (Months month : values()) {
            if (month.getMonthNumber() == number) {
                return month;
            }
        }
        throw new IllegalArgumentException("Mês inválido: " + number);
    }
}
