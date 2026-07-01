package com.hybrid.framework.utils;

import com.hybrid.framework.config.FrameworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Low-memory utility for reading large Excel (.xlsx) files.
 * <p>
 * Uses POI's SAX event model to stream rows one at a time instead of loading
 * the full workbook DOM via {@link org.apache.poi.xssf.usermodel.XSSFWorkbook}.
 * Returns the same {@code List<Map<String, String>>} format as {@link ExcelUtils}.
 * </p>
 */
public final class ExcelStreamingUtils {

    private static final Logger LOG = LogManager.getLogger(ExcelStreamingUtils.class);

    private ExcelStreamingUtils() {
        // Utility class - no instantiation
    }

    /**
     * Streams data rows one at a time from a named sheet without loading the full workbook DOM.
     * <p>
     * Only the current row is retained in the heap (plus POI's shared-strings/styles tables).
     * Suitable for large {@code .xlsx} files or when processing rows incrementally.
     * </p>
     *
     * @param fileName    file name relative to the testdata directory
     * @param sheetName   the sheet to read
     * @param rowConsumer called once per data row (row 0 is treated as headers, not passed to the consumer)
     */
    public static void forEachRow(String fileName, String sheetName, Consumer<Map<String, String>> rowConsumer) {
        Path filePath = FrameworkConstants.TESTDATA_DIR.resolve(fileName);

        try (OPCPackage pkg = OPCPackage.open(filePath.toFile())) {
            int rowCount = streamSheet(pkg, fileName, sheetName, rowConsumer);
            LOG.info("Streamed {} rows from sheet '{}' in file '{}'", rowCount, sheetName, fileName);
        } catch (IOException | OpenXML4JException | SAXException | ParserConfigurationException e) {
            LOG.error("Failed to stream Excel file: {}", e.getMessage());
            throw new RuntimeException("Excel stream failure: " + filePath, e);
        }
    }

    /**
     * Reads all rows using the streaming parser. Avoids the workbook DOM but still
     * materializes every row in a list - use {@link #forEachRow} when you do not need all rows at once.
     *
     * @param fileName  file name relative to the testdata directory
     * @param sheetName the sheet to read
     * @return list of row maps (header to value)
     */
    public static List<Map<String, String>> readSheet(String fileName, String sheetName) {
        List<Map<String, String>> data = new ArrayList<>();
        forEachRow(fileName, sheetName, data::add);
        return data;
    }

    /**
     * Converts Excel data to a 2D Object array for TestNG @DataProvider using the streaming parser.
     * Rows are still collected into memory for the array; use {@link #forEachRow} to avoid that.
     */
    public static Object[][] toDataProviderArray(String fileName, String sheetName) {
        List<Map<String, String>> data = readSheet(fileName, sheetName);
        Object[][] result = new Object[data.size()][1];
        for (int i = 0; i < data.size(); i++) {
            result[i][0] = data.get(i);
        }
        return result;
    }

    // ----------------------------------------------------------------

    private static int streamSheet(OPCPackage pkg, String fileName, String sheetName,
                                   Consumer<Map<String, String>> rowConsumer)
            throws IOException, OpenXML4JException, SAXException, ParserConfigurationException {

        XSSFReader reader = new XSSFReader(pkg);
        SharedStrings strings = reader.getSharedStringsTable();
        StylesTable styles = reader.getStylesTable();
        StreamingSheetHandler sheetHandler = new StreamingSheetHandler(rowConsumer);

        Iterator<InputStream> sheets = reader.getSheetsData();
        while (sheets.hasNext()) {
            try (InputStream sheetStream = sheets.next()) {
                if (!matchesSheetName(sheets, sheetName)) {
                    continue;
                }
                parseSheetStream(sheetStream, styles, strings, sheetHandler);
                return sheetHandler.getRowCount();
            }
        }

        throw new IllegalArgumentException("Sheet '" + sheetName + "' not found in " + fileName);
    }

    private static boolean matchesSheetName(Iterator<InputStream> sheets, String sheetName) {
        if (sheets instanceof XSSFReader.SheetIterator sheetIterator) {
            return sheetName.equals(sheetIterator.getSheetName());
        }
        throw new IllegalStateException("Unexpected sheet iterator type: " + sheets.getClass().getName());
    }

    private static void parseSheetStream(InputStream sheetStream, StylesTable styles, SharedStrings strings,
                                         StreamingSheetHandler sheetHandler)
            throws SAXException, IOException, ParserConfigurationException {
        DataFormatter formatter = new DataFormatter();
        XSSFSheetXMLHandler handler = new XSSFSheetXMLHandler(
                styles, strings, sheetHandler, formatter, true);

        XMLReader parser = XMLHelper.newXMLReader();
        parser.setContentHandler(handler);
        parser.parse(new InputSource(sheetStream));
    }

    private static final class StreamingSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        private final Consumer<Map<String, String>> rowConsumer;
        private List<String> headers = List.of();
        private final Map<Integer, String> currentCells = new TreeMap<>();
        private int rowCount;

        private StreamingSheetHandler(Consumer<Map<String, String>> rowConsumer) {
            this.rowConsumer = rowConsumer;
        }

        @Override
        public void startRow(int rowNum) {
            currentCells.clear();
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum == 0) {
                headers = buildHeaders(currentCells);
                return;
            }

            if (headers.isEmpty()) {
                return;
            }

            Map<String, String> rowData = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                String value = currentCells.getOrDefault(j, "");
                rowData.put(headers.get(j), value.trim());
            }
            rowConsumer.accept(rowData);
            rowCount++;
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            if (cellReference == null) {
                return;
            }
            int col = new CellReference(cellReference).getCol();
            currentCells.put(col, formattedValue != null ? formattedValue : "");
        }

        private int getRowCount() {
            return rowCount;
        }

        private static List<String> buildHeaders(Map<Integer, String> headerCells) {
            if (headerCells.isEmpty()) {
                return List.of();
            }
            int columnCount = headerCells.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
            List<String> headerList = new ArrayList<>(columnCount);
            for (int i = 0; i < columnCount; i++) {
                headerList.add(headerCells.getOrDefault(i, "").trim());
            }
            return headerList;
        }
    }
}
