package com.automation.webscraping.solar.monitor.spreadsheet.reader;

import com.automation.webscraping.solar.monitor.model.Client;
import com.automation.webscraping.solar.monitor.model.InverterManufacturer;
import com.automation.webscraping.solar.monitor.repository.InverterManufacturerRepository;
import com.automation.webscraping.solar.monitor.enums.Manufacturers;
import com.automation.webscraping.solar.monitor.spreadsheet.exceptions.InvalidManufacturer;
import com.automation.webscraping.solar.monitor.spreadsheet.exceptions.InvalidSpreadsheetCellValue;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class ExcelClientCredentials {
    /*
    Aqui será feita a leitura da planilha onde contém os dados de login de usuário.
    Isso aqui provavelmente só vai rodar uma vez, no momento que for para montar o banco de dados
    com base nos clientes que já existem.
 */
    private final List<Client> invalidClients = new ArrayList<>();

    @Autowired
    private InverterManufacturerRepository inverterManufacturerRepository;

    public List<Client> readClientsCredentials(File fileExcel) {
        List<Client> clientList = new ArrayList<>();
        try (
                FileInputStream fileInputStream = new FileInputStream(fileExcel);
                Workbook workbook = new XSSFWorkbook(fileInputStream)
        ) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                Manufacturers manufacturerEnum = Manufacturers.valueOf(sheetName.toUpperCase());
                InverterManufacturer inverterManufacturer = inverterManufacturerRepository.findByName(manufacturerEnum.name())
                            .orElseThrow(()-> new InvalidManufacturer("Fabricante %s não cadastrado.".formatted(sheetName)));

                if (inverterManufacturer != null) {
                    inverterManufacturer.setName(manufacturerEnum.name());
                    inverterManufacturer.setPortalUrl(manufacturerEnum.getPortalUrl());
                }


                Row headers = sheet.getRow(0);
                if (headers == null ||
                        !"Cliente".equalsIgnoreCase(headers.getCell(0).getStringCellValue().trim()) ||
                        !"Usuário".equalsIgnoreCase(headers.getCell(1).getStringCellValue().trim()) ||
                        !"Senha".equalsIgnoreCase(headers.getCell(2).getStringCellValue().trim())) {
                    throw new InvalidSpreadsheetCellValue("Cabeçalhos fora do padrão esperado: Cliente, Usuário, Senha");
                }

                Iterator<Row> rowIterator = sheet.iterator();
                rowIterator.next();
                int countRow = 2;
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    try {
                        String name = verifyCell(row.getCell(0));
                        String username = verifyCell(row.getCell(1));
                        String password = verifyCell(row.getCell(2));

                        if (name.isBlank() || username.isBlank() || password.isBlank()) {
                            Client invalidClient = new Client();
                            invalidClient.setName(name);
                            invalidClient.setUsername(username);
                            invalidClient.setPassword(password);
                            invalidClient.setInverterManufacturer(inverterManufacturer);
                            invalidClients.add(invalidClient);
                            countRow++;
                            continue;
                        }

                        Client client = new Client();
                        client.setName(name);
                        client.setUsername(username);
                        client.setPassword(password);
                        client.setInverterManufacturer(inverterManufacturer);
                        clientList.add(client);
                    } catch (Exception e) {
                        throw new InvalidSpreadsheetCellValue("Erro na linha %d: %s".formatted(countRow, e.getMessage()));
                    }
                    countRow++;
                }
            }
        } catch (RuntimeException | FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return clientList;
    }

    public List<Client> getInvalidClients() {
        return invalidClients;
    }

    private String verifyCell(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue()); // ou `.toString()` se quiser manter decimal
            case FORMULA -> cell.getCellFormula(); // ou `cell.getStringCellValue()` se a fórmula retornar texto
            case BLANK -> "";
            default -> throw new RuntimeException("Tipo de célula não suportado: " + cell.getCellType());
        };
    }

}
