/**
 * Copyright (c) 2018, biezhi 王爵 (biezhi.me@gmail.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.biezhi.excel.plus.writer;

import io.github.biezhi.excel.plus.Constant;
import io.github.biezhi.excel.plus.enums.ExcelType;
import io.github.biezhi.excel.plus.exception.ExcelException;
import io.github.biezhi.excel.plus.utils.ExcelUtils;
import io.github.biezhi.excel.plus.utils.Pair;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Exporter interface
 *
 * @author biezhi
 * @date 2018/2/4
 */
public interface ExcelWriter extends Constant {

    /**
     * Default Export method
     *
     * @param exporter     Exporter Object
     * @param outputStream OutputStream
     * @param <T>          Java Type
     * @throws ExcelException thrown when exporting Excel to an exception
     */
    default <T> void export(Exporter<T> exporter, OutputStream outputStream) throws ExcelException {
        Collection<T> data = exporter.getData();
        if (null == data || data.size() == 0) {
            throw new ExcelException("Export excel data is empty.");
        }
        try {

            Sheet     sheet;
            Workbook  workbook;
            CellStyle headerStyle;
            CellStyle columnStyle = null;
            CellStyle titleStyle;
            List<CellStyle> specialColumnStyles = new ArrayList<>();
            List<Predicate<String>> specialConditions = new ArrayList<>();

            T data0 = data.iterator().next();
            // Set Excel header
            Iterator<T> iterator = data.iterator();

            List<Pair<Integer, String>> writeFieldNames = ExcelUtils.getWriteFieldNames(data0.getClass());

            List<Integer> columnIndexes = writeFieldNames.stream().map(Pair::getK).collect(Collectors.toList());

            int startRow = exporter.startRow();

            if (null != exporter.getTemplatePath()) {
                InputStream in = ExcelWriter.class.getClassLoader().getResourceAsStream(exporter.getTemplatePath());
                workbook = WorkbookFactory.create(in);
                sheet = workbook.getSheetAt(0);

            } else {
                workbook = exporter.getExcelType().equals(ExcelType.XLSX) ? new XSSFWorkbook() : new HSSFWorkbook();
                if (null != exporter.getSheetName()) {
                    sheet = workbook.createSheet(exporter.getSheetName());
                } else {
                    sheet = workbook.createSheet(DEFAULT_SHEET_NAME);
                }

                if (null != exporter.getTitleStyle()) {
                    titleStyle = exporter.getTitleStyle().apply(workbook);
                } else {
                    titleStyle = this.defaultTitleStyle(workbook);
                }

                if (null != exporter.getHeaderStyle()) {
                    headerStyle = exporter.getHeaderStyle().apply(workbook);
                } else {
                    headerStyle = this.defaultHeaderStyle(workbook);
                }

                if (null != exporter.getColumnStyle()) {
                    columnStyle = exporter.getColumnStyle().apply(workbook);
                } else {
                    columnStyle = this.defaultColumnStyle(workbook);
                }

                if (null != exporter.getSpecialColumn()) {
                    exporter.getSpecialColumn().values().forEach(var -> specialColumnStyles.add(var.apply(workbook)));
                    specialConditions = new ArrayList<>(exporter.getSpecialColumn().keySet());
                }

                String headerTitle = exporter.getHeaderTitle();
                int    colIndex    = 0;
                if (null != headerTitle) {
                    colIndex = 1;
                    columnIndexes.stream()
                            .max(Comparator.comparingInt(Integer::intValue))
                            .ifPresent(maxColIndex -> this.writeTitleRow(titleStyle, sheet, headerTitle, maxColIndex));
                }
                this.writeColumnNames(colIndex, headerStyle, sheet, writeFieldNames);
                startRow += colIndex;

            }

            this.writeRows(sheet, columnStyle, specialColumnStyles, null, specialConditions, iterator, startRow, columnIndexes);

            workbook.write(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            throw new ExcelException(e);
        }
    }

    default void writeTitleRow(CellStyle cellStyle, Sheet sheet, String title, int maxColIndex) {
        Row titleRow = sheet.createRow(0);
        for (int i = 0; i <= maxColIndex; i++) {
            Cell cell = titleRow.createCell(i);
            if (i == 0) {
                cell.setCellValue(title);
            }
            cell.setCellStyle(cellStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, maxColIndex));
    }

    /**
     * Write the header row to Sheet.
     *
     * @param rowIndex    start row index
     * @param headerStyle header row cell style
     * @param sheet       work sheet
     * @param columnNames column names
     */
    default void writeColumnNames(int rowIndex, CellStyle headerStyle, Sheet sheet, List<Pair<Integer, String>> columnNames) {
        Row rowHead = sheet.createRow(rowIndex);
        columnNames.forEach(pair -> {
            Integer colIndex   = pair.getK();
            String  columnName = pair.getV();
            Cell    cell       = rowHead.createCell(colIndex);
            if (null != headerStyle) {
                cell.setCellStyle(headerStyle);
            }
            cell.setCellValue(columnName);
        });
    }

    /**
     * Write line data
     *
     * @param sheet       work sheet
     * @param columnStyle each column style in the row.
     * @param specialColumnStyles each special column style in the row.
     * @param rowStyle    row style
     * @param specialConditions    the judgment conditions for a special column
     * @param iterator    row data iterator
     * @param startRow    from the beginning of the line, the default is 1
     * @param <T>         Java Type
     */
    default <T> void writeRows(Sheet sheet, CellStyle columnStyle, List<CellStyle> specialColumnStyles, CellStyle rowStyle, List<Predicate<String>> specialConditions, Iterator<T> iterator, int startRow, List<Integer> columnIndexes) {
        for (int rowNum = startRow; iterator.hasNext(); rowNum++) {
            T   item = iterator.next();
            Row row  = sheet.createRow(rowNum);
            if (null != rowStyle) {
                row.setRowStyle(rowStyle);
            }

            boolean isSpecialColumn = false;
            int index = -1;
            if (null != specialColumnStyles && null != specialConditions) {
                here:
                for (Integer col : columnIndexes) {
                    String value = ExcelUtils.getColumnValue(item, col);
                    for (int i = 0, length = specialConditions.size(); i < length; i++) {
                        index = i;
                        isSpecialColumn = specialConditions.get(i).test(value);
                        if (isSpecialColumn) {
                            break here;
                        }
                    }
                }
            }

            Iterator<Integer> colIt = columnIndexes.iterator();
            while (colIt.hasNext()) {
                int    col   = colIt.next();
                Cell   cell  = row.createCell(col);
                String value = ExcelUtils.getColumnValue(item, col);
                if (isSpecialColumn) {
                    cell.setCellStyle(specialColumnStyles.get(index));
                } else if (null != columnStyle) {
                    cell.setCellStyle(columnStyle);
                }
                if (null != value) {
                    if (ExcelUtils.isNumber(value)) {
                        cell.setCellValue(Double.valueOf(value));
                    } else {
                        cell.setCellValue(value);
                    }
                } else {
                    cell.setCellValue(value);
                }
                sheet.autoSizeColumn(col);
            }
        }
    }

    /**
     * Export excel
     *
     * @param exporter Exporter Object
     * @param <T>      Java Type
     * @throws ExcelException thrown when exporting Excel to an exception
     */
    <T> void export(Exporter<T> exporter) throws ExcelException;

}
