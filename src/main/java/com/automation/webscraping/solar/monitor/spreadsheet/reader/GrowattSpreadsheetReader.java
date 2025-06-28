package com.automation.webscraping.solar.monitor.spreadsheet.reader;

import com.automation.webscraping.solar.monitor.enums.Manufacturers;
import com.automation.webscraping.solar.monitor.model.EnergyYearlyData;
import com.automation.webscraping.solar.monitor.model.InverterData;
import com.automation.webscraping.solar.monitor.model.InverterManufacturer;
import com.automation.webscraping.solar.monitor.repository.EnergyYearlyDataRepository;
import com.automation.webscraping.solar.monitor.repository.InverterManufacturerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class GrowattSpreadsheetReader {
    @Autowired
    private EnergyYearlyDataRepository energyYearlyDataRepository;

    @Autowired
    private InverterManufacturerRepository inverterManufacturerRepository;

    private static final String START_CELL_NAME = "Número de série do inversor";
    private static final String NEXT_SECTION_START_CELL_NAME = "Dados do storage : Carga de bateria hoje(kWh)";

    /**
     * Tenta obter o valor numérico de uma célula, independentemente do seu tipo (NUMERIC ou STRING).
     * Lida com números formatados com vírgula como separador decimal.
     * @param cell A célula do Excel.
     * @return Um Optional contendo o valor Double se a conversão for bem-sucedida, ou um Optional vazio caso contrário.
     */
    private Optional<Double> getNumericCellValue(Cell cell) {
        if (cell == null) {
            return Optional.empty();
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return Optional.of(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String stringValue = cell.getStringCellValue().trim();
                stringValue = stringValue.replace(",", ".");
                if (!stringValue.isEmpty()) {
                    return Optional.of(Double.parseDouble(stringValue));
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Falha ao converter o valor da célula '{}' para número. Linha: {}, Coluna: {}. Erro: {}",
                    cell.toString(), (cell.getRowIndex() + 1), (cell.getColumnIndex() + 1), e.getMessage());
        }
        return Optional.empty();
    }


    public List<InverterData> extractEnergyDataFromSpreadsheet(File fileExcel) {
        List<InverterData> allInverterData = new ArrayList<>();
        Workbook workbook = null;

        try (FileInputStream fileInputStream = new FileInputStream(fileExcel)) {
            if (fileExcel.getName().toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fileInputStream);
            } else if (fileExcel.getName().toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(fileInputStream);
            } else {
                throw new IllegalArgumentException("Formato de arquivo não suportado, deve ser .xls ou .xlsx");
            }

            int reportYear = -1;
            Sheet firstSheet = workbook.getSheetAt(0);
            if (firstSheet != null) {
                Row yearRow = firstSheet.getRow(3);
                if (yearRow != null) {
                    Cell yearCell = yearRow.getCell(0);
                    if (yearCell != null) {
                        try {
                            if (yearCell.getCellType() == CellType.NUMERIC) {
                                reportYear = (int) yearCell.getNumericCellValue();
                            } else if (yearCell.getCellType() == CellType.STRING) {
                                String yearString = yearCell.getStringCellValue().trim();
                                if (!yearString.isEmpty()) {
                                    reportYear = Integer.parseInt(yearString);
                                }
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Não foi possível extrair o ano da célula A1. Verifique o formato. Erro: {}", e.getMessage());
                        }
                    }
                }
            }
            if (reportYear == -1) {
                log.warn("Não foi possível determinar o ano do relatório. Usando o ano atual como padrão ({}).", java.time.Year.now().getValue());
                reportYear = java.time.Year.now().getValue();
            }

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                log.info("Processando planilha: {}", sheetName);

                int targetRowIndex = -1;

                for (Row row : sheet) {
                    Cell cell = row.getCell(0);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String value = cell.getStringCellValue().trim();
                        if (START_CELL_NAME.equalsIgnoreCase(value)) {
                            targetRowIndex = row.getRowNum();
                            log.info("Cabeçalho '{}' encontrado na linha {}", START_CELL_NAME, (targetRowIndex + 1));
                            break;
                        }
                    }
                }

                if (targetRowIndex == -1) {
                    log.warn("Cabeçalho '{}' não encontrado na planilha '{}'. Pulando esta planilha.", START_CELL_NAME, sheetName);
                    continue;
                }

                int currentRowIndex = targetRowIndex + 1;
                while (true) {
                    Row dataRow = sheet.getRow(currentRowIndex);

                    if (dataRow == null) {
                        log.info("Fim dos dados do inversor ou final da planilha alcançado na linha {}.", (currentRowIndex + 1));
                        break;
                    }

                    Cell firstCell = dataRow.getCell(0);
                    if (firstCell == null || firstCell.getCellType() == CellType.BLANK ||
                            (firstCell.getCellType() == CellType.STRING && firstCell.getStringCellValue().trim().isEmpty())) {
                        log.info("Linha vazia ou célula vazia na primeira coluna. {}.", (currentRowIndex + 1));
                        break;
                    }

                    // Verifica se é o cabeçalho da próxima seção para parar a leitura de inversores
                    if (firstCell.getCellType() == CellType.STRING &&
                            firstCell.getStringCellValue().trim().equalsIgnoreCase(NEXT_SECTION_START_CELL_NAME)) {
                        log.info("Cabeçalho da próxima seção '{}' encontrado na linha {}. Parando a leitura de dados do inversor.", NEXT_SECTION_START_CELL_NAME, (currentRowIndex + 1));
                        break;
                    }

                    String serialNumber = "";
                    InverterData currentInverterData;

                    Cell serialCell = dataRow.getCell(0);
                    if (serialCell != null && serialCell.getCellType() == CellType.STRING && !serialCell.getStringCellValue().trim().isEmpty()) {
                        serialNumber = serialCell.getStringCellValue().trim();
                        currentInverterData = new InverterData(serialNumber, reportYear);
                        log.info("Processando inversor: '{}' na linha {}.", serialNumber, (currentRowIndex + 1));
                    } else {
                        currentInverterData = null;
                        log.warn("Linha {} não possui um número de série válido na coluna A. Pulando esta linha.", (currentRowIndex + 1));
                        currentRowIndex++;
                        continue;
                    }

                    int monthColumnStartIdx = 3;
                    int totalColumnIdx = 15;

                    for (int month = 1; month <= 12; month++) {
                        Cell monthCell = dataRow.getCell(monthColumnStartIdx + (month - 1));
                        Optional<Double> monthlyValue = getNumericCellValue(monthCell);
                        int finalMonth = month;
                        monthlyValue.ifPresent(val -> currentInverterData.getMonthlyGeneration().put(finalMonth, val));
                        if (monthlyValue.isEmpty()) {
                            log.warn("Mês {} (Coluna {}): Valor não numérico ou vazio na Linha {}. Usando 0.0.",
                                    month, (monthColumnStartIdx + (month - 1) + 1), (currentRowIndex + 1));
                        }
                    }

                    Cell totalCell = dataRow.getCell(totalColumnIdx);
                    Optional<Double> totalValue = getNumericCellValue(totalCell);
                    totalValue.ifPresent(currentInverterData::setTotalGeneration);
                    if (totalValue.isEmpty()) {
                        log.warn("Total (Coluna {}): Valor não numérico ou vazio na Linha {}. Usando 0.0.",
                                (totalColumnIdx + 1), (currentRowIndex + 1));
                    }

                    allInverterData.add(currentInverterData);
                    log.info("Dados do inversor '{}' processados: {}", serialNumber, currentInverterData);

                    currentRowIndex++;
                }
            }

        } catch (IOException e) {
            log.error("Erro de I/O ao ler o arquivo Excel: {}", e.getMessage());
            throw new RuntimeException("Erro de I/O ao ler o arquivo Excel.", e);
        } catch (Exception e) {
            log.error("Ocorreu um erro inesperado ao processar o arquivo Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Erro inesperado ao processar o arquivo Excel.", e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.error("Erro ao fechar o workbook: {}", e.getMessage());
                }
            }
        }
        return allInverterData;
    }
}
