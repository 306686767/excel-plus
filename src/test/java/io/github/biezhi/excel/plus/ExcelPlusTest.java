package io.github.biezhi.excel.plus;

import io.github.biezhi.excel.plus.enums.ExcelType;
import io.github.biezhi.excel.plus.enums.ParseType;
import io.github.biezhi.excel.plus.exception.ExcelException;
import io.github.biezhi.excel.plus.model.CardSecret;
import io.github.biezhi.excel.plus.reader.Reader;
import io.github.biezhi.excel.plus.writer.Exporter;
import io.github.biezhi.excel.plus.writer.ResponseWrapper;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * ExcelPlus Test
 *
 * @author biezhi
 * @date 2018/3/21
 */
public class ExcelPlusTest {

    private ExcelPlus excelPlus = new ExcelPlus();

    private File getCardExcelFile() {
        try {
            return new File(URLDecoder.decode(ExcelPlusTest.class.getResource("/卡密列表.xlsx").getPath(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    @Test
    public void testReadByDOM() throws ExcelException {
        Reader reader = Reader.create().startRowIndex(2).excelFile(getCardExcelFile());

        List<CardSecret> cardSecrets = excelPlus.read(CardSecret.class, reader)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        System.out.println(cardSecrets);
        Assert.assertNotNull(cardSecrets);
        Assert.assertEquals(4, cardSecrets.size());
    }

    @Test
    public void testReadBySAX() throws ExcelException {

        Reader reader = Reader.create().parseType(ParseType.SAX).excelFile(getCardExcelFile());

        List<CardSecret> cardSecrets = excelPlus.read(CardSecret.class, reader).asList();

        System.out.println(cardSecrets);
        Assert.assertNotNull(cardSecrets);
        Assert.assertEquals(4, cardSecrets.size());
    }

    @Test
    public void testReadBySheetName() throws ExcelException {
        Reader reader = Reader.create().parseType(ParseType.DOM)
                .sheetName("工作表 1")
                .excelFile(getCardExcelFile());

        List<CardSecret> cardSecrets = excelPlus.read(CardSecret.class, reader).asList();

        System.out.println(cardSecrets);
        Assert.assertNotNull(cardSecrets);
        Assert.assertEquals(4, cardSecrets.size());
    }

    @Test
    public void testReadAndFilter() throws ExcelException {
        Reader reader = Reader.create().parseType(ParseType.SAX).excelFile(getCardExcelFile());

        List<CardSecret> cardSecrets = excelPlus.read(CardSecret.class, reader)
                .filter(CardSecret::getUsed)
                .collect(Collectors.toList());

        System.out.println(cardSecrets);
        Assert.assertNotNull(cardSecrets);
        Assert.assertEquals(1, cardSecrets.size());
    }

    @Test
    public void testReadAndMap() throws ExcelException {
        Reader reader = Reader.create().parseType(ParseType.SAX).excelFile(getCardExcelFile());

        List<String> cardSecrets = excelPlus.read(CardSecret.class, reader)
                .map(CardSecret::getSecret)
                .collect(Collectors.toList());

        System.out.println(cardSecrets);
        Assert.assertNotNull(cardSecrets);
        Assert.assertEquals(4, cardSecrets.size());
    }

    @Test
    public void testExport() throws ExcelException {
        List<CardSecret> cardSecrets = this.buildCardSecrets();
        excelPlus.export(cardSecrets).writeAsFile(new File("export.xls"));
    }

    @Test
    public void testExportTitle() throws ExcelException {
        List<CardSecret> cardSecrets = this.buildCardSecrets();
        excelPlus.export(Exporter.create(cardSecrets).title("卡密列表第一季数据")).writeAsFile(new File("卡密列表.xls"));
    }

    @Test
    public void testExportStyle() throws ExcelException {
        List<CardSecret> cardSecrets = this.buildCardSecrets();
        excelPlus.export(Exporter.create(cardSecrets).headerStyle(workbook -> {
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setAlignment(HorizontalAlignment.LEFT);

            headerStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Font headerFont = workbook.createFont();
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            return headerStyle;
        })).writeAsFile(new File("卡密列表.xls"));
    }

    @Test
    public void testExportByTpl() throws ExcelException {
        List<CardSecret> cardSecrets = this.buildCardSecrets();
        excelPlus.export(
                Exporter.create(cardSecrets).byTemplate("tpl.xls").startRow(2)
        ).writeAsFile(new File("template_rows.xls"));
    }

    @Test
    public void testExportBySpecials() throws ExcelException {
        List<CardSecret> cardSecrets = this.buildCardSecrets();

        Map<Predicate<String>, Function<Workbook, CellStyle>> specialColumn = new ConcurrentHashMap<>(3);
        specialColumn.put(var -> var.equals("vlfdzepjmlz2y43z7er4"), webHook -> {
            XSSFCellStyle cellStyle = (XSSFCellStyle) webHook.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.LEFT);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            cellStyle.setWrapText(true);
            XSSFColor xssfColor = new XSSFColor();
            xssfColor.setRGB(new byte[]{(byte) 252, (byte) 228, (byte) 236});
            cellStyle.setFillForegroundColor(xssfColor);
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = webHook.createFont();
            font.setFontName("Arial");
            font.setFontHeightInPoints((short) 12);
            cellStyle.setFont(font);
            return cellStyle;
        });
        specialColumn.put(var -> var.equals("rasefq2rzotsmx526z6g"), webHook -> {
            XSSFCellStyle cellStyle = (XSSFCellStyle) webHook.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.LEFT);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            cellStyle.setWrapText(true);
            XSSFColor xssfColor = new XSSFColor();
            xssfColor.setRGB(new byte[]{(byte) 255, (byte) 235, (byte) 238});
            cellStyle.setFillForegroundColor(xssfColor);
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = webHook.createFont();
            font.setFontName("Arial");
            font.setFontHeightInPoints((short) 13);
            cellStyle.setFont(font);
            return cellStyle;
        });

        Exporter<CardSecret> exporter = Exporter.create(cardSecrets)
                .sheetName("卡密列表第一季数据").specialColumn(specialColumn);
        exporter.setExcelType(ExcelType.XLSX);

        excelPlus.export(exporter).writeAsFile(new File("export.xlsx"));
    }

    //    @Test
    public void testDownload() throws ExcelException {
        List<CardSecret> cardSecrets = this.buildCardSecrets();
        excelPlus.export(cardSecrets).writeAsResponse(ResponseWrapper.create(null, "xxx表格.xls"));
    }

    private List<CardSecret> buildCardSecrets() {
        List<CardSecret> cardSecrets = new ArrayList<>();
        cardSecrets.add(new CardSecret(1, "vlfdzepjmlz2y43z7er4", new BigDecimal("20"), true));
        cardSecrets.add(new CardSecret(2, "rasefq2rzotsmx526z6g", new BigDecimal("10"), true));
        cardSecrets.add(new CardSecret(2, "2ru44qut6neykb2380wt", new BigDecimal("50"), false));
        cardSecrets.add(new CardSecret(1, "srcb4c9fdqzuykd6q4zl", new BigDecimal("15"), true));
        return cardSecrets;
    }

}
