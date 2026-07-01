package com.hybrid.framework.utils;

import com.hybrid.framework.config.FrameworkConstants;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Utility for reading test data from CSV files.
 * <p>
 * First row is treated as headers. Returns data in the same
 * {@code List<Map<String, String>>} format as {@link ExcelUtils}
 * for consistency.
 * </p>
 */
public final class CsvUtils {

    private static final Logger LOG = LogManager.getLogger(CsvUtils.class);

    private CsvUtils() {
        // Utility class — no instantiation
    }

    /**
     * Reads all rows from a CSV file in the testdata directory.
     *
     * @param fileName CSV file name relative to the testdata directory
     * @return list of row maps (header → value)
     */
    public static List<Map<String, String>> readCsv(String fileName) {
        Path filePath = FrameworkConstants.TESTDATA_DIR.resolve(fileName);
        List<Map<String, String>> data = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath.toFile()))) {
            List<String[]> allRows = reader.readAll();

            if (allRows.isEmpty()) {
                LOG.warn("CSV file is empty: {}", fileName);
                return data;
            }

            String[] headers = allRows.get(0);

            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                Map<String, String> rowData = new LinkedHashMap<>();
                for (int j = 0; j < headers.length; j++) {
                    String value = (j < row.length) ? row[j].trim() : "";
                    rowData.put(headers[j].trim(), value);
                }
                data.add(rowData);
            }

            LOG.info("Read {} rows from CSV file '{}'", data.size(), fileName);

        } catch (IOException | CsvException e) {
            LOG.error("Failed to read CSV file: {}", e.getMessage());
            throw new RuntimeException("CSV read failure: " + filePath, e);
        }

        return data;
    }

    /**
     * Converts CSV data to a 2D Object array for TestNG @DataProvider.
     */
    public static Object[][] toDataProviderArray(String fileName) {
        List<Map<String, String>> data = readCsv(fileName);
        Object[][] result = new Object[data.size()][1];
        for (int i = 0; i < data.size(); i++) {
            result[i][0] = data.get(i);
        }
        return result;
    }
}
