package com.automation.webscraping.solar.monitor.spreadsheet.reader;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

@Slf4j
@Service
public class GrowattSpreadsheetReader {

    private static final String START_CELL_NAME = "Número de série do inversor";
    private static final int CELLS_TO_ITERATE = 13;



    public void extractEnergyDataFromSpreadsheet(File fileExcel){
        try(
                FileInputStream fileInputStream = new FileInputStream(fileExcel);
                Workbook workbook = new HSSFWorkbook(fileInputStream);
        ){

            int targetRowIndex = -1;
            Sheet targetSheet = null;
            for(int i = 0; i < workbook.getNumberOfSheets(); i++){
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                for (Row row : sheet) {
                    Cell cell = row.getCell(0);
                    if (cell != null) {
                        String value = cell.getStringCellValue().trim();
                        if (START_CELL_NAME.equalsIgnoreCase(value)) {
                            targetRowIndex = row.getRowNum();
                            targetSheet = sheet;
                            log.info("Conteúdo encontrado na célula: {}", targetRowIndex);
                            break;
                        }
                    }
                }
                if (targetRowIndex == -1) {
                    log.warn("Cabeçalho '{}' não encontrado na planilha '{}'", START_CELL_NAME, sheetName);
                    continue;
                }
                int dataRowIndex = targetRowIndex + 1;
                Row dataRow = targetSheet.getRow(dataRowIndex);
                if (dataRow == null) {
                    log.warn("Nenhuma linha de dados encontrada após o cabeçalho na linha {}. Verifique o arquivo Excel.", (targetRowIndex + 1));
                    return;
                }

                Iterator<Cell> cellIterator = dataRow.cellIterator();
                int cellsProcessed = 0;
                boolean foundFirstNonEmpty = false;
                int startColumnIndex = -1;
                HashMap<String, Double> annualSolarGeneration = new HashMap<>();

                while (cellIterator.hasNext() && cellsProcessed < CELLS_TO_ITERATE) {
                    Cell cell = cellIterator.next();
                    String cellValue = cell.getStringCellValue().trim();

                    // Encontra a primeira célula não vazia - o nome do inversor
                    if (!foundFirstNonEmpty) {
                        if (!cellValue.isEmpty()) {
                            foundFirstNonEmpty = true;
                            startColumnIndex = cell.getColumnIndex();
                            log.info("Primeira célula não vazia encontrada na coluna {}. Valor: {}", (startColumnIndex + 1), cellValue);
                            cellsProcessed++;
                        }
                    } else {
                        log.info("Processando célula (Linha: {}, Coluna: {}): {}",
                                (cell.getRowIndex() + 1), (cell.getColumnIndex() + 1), cellValue);
                        cellsProcessed++;
                    }

                }

                if (!foundFirstNonEmpty) {
                    log.warn("Nenhuma célula não vazia encontrada na linha de dados {} após o cabeçalho para iniciar a iteração de 12 células.", (dataRowIndex + 1));
                } else if (cellsProcessed < CELLS_TO_ITERATE) {
                    log.warn("Atingiu o final da linha antes de processar {} células a partir da primeira não vazia. Processadas: {}", CELLS_TO_ITERATE, cellsProcessed);
                }
                // --- Fim da lógica de iteração ---

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
