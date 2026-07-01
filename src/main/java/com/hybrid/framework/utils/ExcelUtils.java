package com.hybrid.framework.utils;

import com.hybrid.framework.config.FrameworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Utility for reading test data from Excel (.xlsx) files.
 * <p>
 * Returns data as a list of maps ({@code List<Map<String, String>>})
 * where keys are column headers and values are cell values.
 * This format integrates cleanly with TestNG {@code @DataProvider}.
 * </p>
 * <p>
 * For large files, use {@link ExcelStreamingUtils} which streams rows via
 * POI's SAX event model instead of loading the full workbook DOM.
 * </p>
 */
public final class ExcelUtils {

    private static final Logger LOG = LogManager.getLogger(ExcelUtils.class);

    private ExcelUtils() {
        // Utility class — no instantiation
    }

    /**
     * Reads all rows from a named sheet in an Excel file.
     *
     * @param fileName  file name relative to the testdata directory
     * @param sheetName the sheet to read
     * @return list of row maps (header → value)
     */
    public static List<Map<String, String>> readSheet(String fileName, String sheetName) {
        Path filePath = FrameworkConstants.TESTDATA_DIR.resolve(fileName);
        List<Map<String, String>> data = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet '" + sheetName + "' not found in " + fileName);
            }

            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValue(cell));
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, String> rowData = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowData.put(headers.get(j), getCellValue(cell));
                }
                data.add(rowData);
            }

            LOG.info("Read {} rows from sheet '{}' in file '{}'", data.size(), sheetName, fileName);

        } catch (IOException e) {
            LOG.error("Failed to read Excel file: {}", e.getMessage());
            throw new RuntimeException("Excel read failure: " + filePath, e);
        }

        return data;
    }

    /**
     * Returns the first row in a sheet where the given column matches the expected value.
     *
     * @param fileName     file name relative to the testdata directory
     * @param sheetName    the sheet to read
     * @param columnName   header name of the column to filter on
     * @param columnValue  expected cell value in that column
     * @return row map (header → value) for the matching row
     * @throws IllegalArgumentException if the sheet, column, or matching row is not found
     */
    public static Map<String, String> readRowByColumnValue(
            String fileName, String sheetName, String columnName, String columnValue) {
        Path filePath = FrameworkConstants.TESTDATA_DIR.resolve(fileName);

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet '" + sheetName + "' not found in " + fileName);
            }

            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValue(cell));
            }

            int columnIndex = headers.indexOf(columnName);
            if (columnIndex < 0) {
                throw new IllegalArgumentException(
                        "Column '" + columnName + "' not found in sheet '" + sheetName + "' of " + fileName);
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell matchCell = row.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                if (!columnValue.equals(getCellValue(matchCell))) {
                    continue;
                }

                Map<String, String> rowData = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowData.put(headers.get(j), getCellValue(cell));
                }

                LOG.info("Found row {} in sheet '{}' where '{}' = '{}'",
                        i, sheetName, columnName, columnValue);
                return rowData;
            }

            throw new IllegalArgumentException(
                    "No row found in sheet '" + sheetName + "' of " + fileName
                            + " where '" + columnName + "' = '" + columnValue + "'");

        } catch (IOException e) {
            LOG.error("Failed to read Excel file: {}", e.getMessage());
            throw new RuntimeException("Excel read failure: " + filePath, e);
        }
    }

    /**
     * Converts Excel data to a 2D Object array for TestNG @DataProvider.
     */
    public static Object[][] toDataProviderArray(String fileName, String sheetName) {
        List<Map<String, String>> data = readSheet(fileName, sheetName);
        Object[][] result = new Object[data.size()][1];
        for (int i = 0; i < data.size(); i++) {
            result[i][0] = data.get(i);
        }
        return result;
    }

    // ──────────────────────────────────────────────────────────────

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                double numVal = cell.getNumericCellValue();
                yield (numVal == Math.floor(numVal)) ? String.valueOf((long) numVal) : String.valueOf(numVal);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
