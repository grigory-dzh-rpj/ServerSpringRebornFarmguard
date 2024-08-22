package com.dg.ServerRebornFarmguard.service.reports.excel;

import com.dg.ServerRebornFarmguard.service.PlaceService;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.stereotype.Component;



@Component
public class MainReports {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Autowired
    private PlaceService placeService;




    //Вся база
    public byte[] generateExcel() throws IOException {
        DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

        String OPEN = null;
        String CLOSE = null;
        String currentUser = null;

        // Получение данных из базы данных
        List<Map<String, Object>> movements = jdbcTemplate.queryForList("SELECT * FROM movements");

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Перемещения");
        /*Стили*/

        CellStyle openStyle = workbook.createCellStyle();
        openStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        openStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        openStyle.setAlignment(HorizontalAlignment.CENTER);
        openStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle itogStyle = workbook.createCellStyle();
        Font itogFont = workbook.createFont();
        itogFont.setBold(true);
        itogStyle.setFont(itogFont);
        itogStyle.setAlignment(HorizontalAlignment.CENTER);
        itogStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        itogStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        itogStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle closeStyle = workbook.createCellStyle();
        closeStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        closeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        closeStyle.setAlignment(HorizontalAlignment.CENTER);
        closeStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.TURQUOISE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Заголовки столбцов
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ШК", "ФИО", "Должность", "Время прихода", "Время ухода", "Итого на пункте", "Дата", "Пункт", "Статус", "Итого за смену", "Эффективное время", "Неэффективное время", "Пункт закрытия смены", "Время открытия/закрытия смены", "Опоздание"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));

        // Сортировка данных по именам
        movements.sort(Comparator.comparing(movement -> (String) movement.get("name_user")));

        int rowNum = 1;
        Duration totalEffectiveDuration = Duration.ZERO;
        int startMergeRow = -1;

        for (Map<String, Object> movement : movements) {
            Row row = sheet.createRow(rowNum);

            Cell idCell = row.createCell(0);
            idCell.setCellValue((Long) movement.get("id_user"));
            idCell.setCellStyle(dataStyle);

            Cell nameCell = row.createCell(1);
            nameCell.setCellValue((String) movement.get("name_user"));
            nameCell.setCellStyle(dataStyle);

            Cell positionCell = row.createCell(2);
            positionCell.setCellValue((String) movement.get("position_user"));
            positionCell.setCellStyle(dataStyle);

            Cell arrivalTimeCell = row.createCell(3);
            String arrivalTimeString = (String) movement.get("coming_time");
            LocalTime arrivalTime = null;
            if (arrivalTimeString != null) {
                arrivalTime = LocalTime.parse(arrivalTimeString, TIME_FORMATTER);
                arrivalTimeCell.setCellValue(arrivalTime.toString());
            }
            arrivalTimeCell.setCellStyle(dataStyle);

            Cell exitTimeCell = row.createCell(4);
            String exitTimeString = (String) movement.get("exit_time");
            LocalTime exitTime = null;
            if (exitTimeString != null) {
                exitTime = LocalTime.parse(exitTimeString, TIME_FORMATTER);
                exitTimeCell.setCellValue(exitTime.toString());
            }
            exitTimeCell.setCellStyle(dataStyle);

            Cell durationCell = row.createCell(5);
            if (arrivalTime != null && exitTime != null) {
                Duration duration = Duration.between(arrivalTime, exitTime);
                durationCell.setCellValue(formatDuration(duration));
                durationCell.setCellStyle(dataStyle);
                totalEffectiveDuration = totalEffectiveDuration.plus(duration);
            }

            Cell dateCell = row.createCell(6);
            dateCell.setCellValue(movement.get("date").toString());
            dateCell.setCellStyle(dataStyle);

            Cell placeCell = row.createCell(7);
            placeCell.setCellValue((String) movement.get("place"));
            placeCell.setCellStyle(dataStyle);

            Cell statusCell = row.createCell(8);
            statusCell.setCellValue((String) movement.get("open_close"));
            statusCell.setCellStyle(dataStyle);



            String userName = (String) movement.get("name_user");

            if ("open".equals(movement.get("open_close")) ) {
                if (!userName.equals(currentUser)) {
                    // Новая смена для нового пользователя
                    startMergeRow = rowNum;
                    OPEN = (String) movement.get("coming_time");
                    currentUser = userName;
                    totalEffectiveDuration = Duration.ZERO; // Сброс для новой смены

                }else {
                    startMergeRow = rowNum;
                    OPEN = (String) movement.get("coming_time");
                }

                for (int i = 0; i < headers.length-6; i++) {
                    row.getCell(i).setCellStyle(openStyle);
                }
            } else if ("close".equals(movement.get("open_close"))) {
                CLOSE = (String) movement.get("exit_time");
                for (int i = 0; i < headers.length-6; i++) {
                    row.getCell(i).setCellStyle(closeStyle);
                }
                if (startMergeRow != -1 && userName.equals(currentUser)) {

                    Cell shiftTotalCell = sheet.getRow(startMergeRow).createCell(9);
                    // это может вызвать ошибку - если кто-то закрыл но карту не приложил

                    String itogString = (String) movement.get("open_close_time");
                    LocalTime itogTime = null;
                    if (exitTimeString != null) {
                        itogTime = LocalTime.parse(itogString, TIME_FORMATTER);
                        shiftTotalCell.setCellValue(itogTime.toString());
                    }else{
                        shiftTotalCell.setCellValue("Смена еще не закрыта!");
                    }


                    Cell effectiveCell = sheet.getRow(startMergeRow).createCell(10);
                    effectiveCell.setCellValue(formatDuration(totalEffectiveDuration));

                    LocalTime zero = LocalTime.parse("00:00", TIME_FORMATTER);
                    Duration shiftDuration = Duration.between(zero, itogTime);
                    Duration nonEffectiveDuration = shiftDuration.minus(totalEffectiveDuration);
                    Cell nonEffectiveCell = sheet.getRow(startMergeRow).createCell(11);
                    nonEffectiveCell.setCellValue(formatDuration(nonEffectiveDuration));

                    Cell closePlaceCell = sheet.getRow(startMergeRow).createCell(12);
                    closePlaceCell.setCellValue((String) movement.get("place"));

                    Cell openCloseTimesCell = sheet.getRow(startMergeRow).createCell(13);
                    if(OPEN != null && CLOSE != null){
                        openCloseTimesCell.setCellValue(String.format("%s | %s", OPEN, CLOSE));
                    }

                    Cell delay =  sheet.getRow(startMergeRow).createCell(14);



                    delay.setCellComment(comment("Если смена открыта позже 08:45", workbook, sheet, delay));

                    if (LocalTime.parse(OPEN, TIME_FORMATTER).isAfter(LocalTime.parse("08:45", TIME_FORMATTER))) {
                        delay.setCellValue("ЕСТЬ");
                    }else{
                        delay.setCellValue("НЕТ");
                    }

                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 9,  itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 10, itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 11, itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 12, itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 13, itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 14, itogStyle);

                    startMergeRow = -1;
                    totalEffectiveDuration = Duration.ZERO; // Reset for next shift
                }
            }
            rowNum++;
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }
    //Общий за период
    public byte[] generateExcelForDateBetween(String dateRange) throws IOException {
        DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

        String OPEN = null;
        String CLOSE = null;
        String currentUser = null;
        System.out.println(dateRange);


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Разбиваем строку на две даты
        String[] dates = dateRange.split("/");
        LocalDate startDate = LocalDate.parse(dates[0], formatter);
        LocalDate endDate = LocalDate.parse(dates[1], formatter);

        // Получение данных из базы данных за указанный период
        List<Map<String, Object>> movements = jdbcTemplate.queryForList("SELECT * FROM movements WHERE date BETWEEN ? AND ?", startDate, endDate);

        if(movements.isEmpty()){
            System.out.println("Лист пустой");
            return new byte[1];
        }
        // Создание книги и листа с использованием XSSF для .xlsx формата
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Перемещения");
        /*Стили*/

        CellStyle openStyle = workbook.createCellStyle();
        openStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        openStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        openStyle.setAlignment(HorizontalAlignment.CENTER);
        openStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle itogStyle = workbook.createCellStyle();
        Font itogFont = workbook.createFont();
        itogFont.setBold(true);
        itogStyle.setFont(itogFont);
        itogStyle.setAlignment(HorizontalAlignment.CENTER);
        itogStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        itogStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        itogStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle closeStyle = workbook.createCellStyle();
        closeStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        closeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        closeStyle.setAlignment(HorizontalAlignment.CENTER);
        closeStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.TURQUOISE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);



            // Заголовки столбцов
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ШК", "ФИО", "Должность", "Дата", "Время прихода", "Время ухода", "Итого на пункте", "Пункт", "Статус", "Итого за смену", "Эффективное время", "Неэффективное время", "Пункт закрытия смены", "Время открытия/закрытия смены", "Опоздание"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));


            // Сортировка данных по именам (второй столбец)
            movements.sort(Comparator.comparing(movement -> (String) movement.get("name_user")));

            int rowNum = 1;
            Duration totalEffectiveDuration = Duration.ZERO;
            int startMergeRow = -1;

            for (Map<String, Object> movement : movements) {
                Row row = sheet.createRow(rowNum);

                Cell idCell = row.createCell(0);
                idCell.setCellValue((Long) movement.get("id_user"));
                idCell.setCellStyle(dataStyle);

                Cell nameCell = row.createCell(1);
                nameCell.setCellValue((String) movement.get("name_user"));
                nameCell.setCellStyle(dataStyle);


                Cell positionCell = row.createCell(2);
                positionCell.setCellValue((String) movement.get("position_user"));
                positionCell.setCellStyle(dataStyle);

                Cell arrivalTimeCell = row.createCell(4);
                String arrivalTimeString = (String) movement.get("coming_time");
                LocalTime arrivalTime = null;
                if (arrivalTimeString != null) {
                    arrivalTime = LocalTime.parse(arrivalTimeString, TIME_FORMATTER);
                    arrivalTimeCell.setCellValue(arrivalTime.toString());
                }
                arrivalTimeCell.setCellStyle(dataStyle);

                Cell exitTimeCell = row.createCell(5);
                String exitTimeString = (String) movement.get("exit_time");
                LocalTime exitTime = null;
                if (exitTimeString != null) {
                    exitTime = LocalTime.parse(exitTimeString, TIME_FORMATTER);
                    exitTimeCell.setCellValue(exitTime.toString());
                }
                exitTimeCell.setCellStyle(dataStyle);


                Cell durationCell = row.createCell(6);


                if (arrivalTime != null && exitTime != null) {
                    Duration duration = Duration.between(arrivalTime, exitTime);
                    durationCell.setCellValue(formatDuration(duration));
                    durationCell.setCellStyle(dataStyle);
                    totalEffectiveDuration = totalEffectiveDuration.plus(duration);
                }


                Cell dateCell = row.createCell(3);
                dateCell.setCellValue(movement.get("date").toString());
                dateCell.setCellStyle(dataStyle);

                Cell placeCell = row.createCell(7);
                placeCell.setCellValue((String) movement.get("place"));
                placeCell.setCellStyle(dataStyle);

                Cell statusCell = row.createCell(8);
                statusCell.setCellValue((String) movement.get("open_close"));
                statusCell.setCellStyle(dataStyle);

                String userName = (String) movement.get("name_user");

                if ("open".equals(movement.get("open_close"))) {
                    if (!userName.equals(currentUser)) {
                        // Новая смена для нового пользователя
                        startMergeRow = rowNum;
                        OPEN = (String) movement.get("coming_time");
                        currentUser = userName;
                        totalEffectiveDuration = Duration.ZERO; // Сброс для новой смены

                    } else {
                        startMergeRow = rowNum;
                        OPEN = (String) movement.get("coming_time");
                    }

                    for (int i = 0; i < headers.length - 6; i++) {
                        row.getCell(i).setCellStyle(openStyle);
                    }


                } else if ("close".equals(movement.get("open_close"))) {
                    CLOSE = (String) movement.get("exit_time");
                    for (int i = 0; i < headers.length - 6; i++) {
                        row.getCell(i).setCellStyle(closeStyle);
                    }
                    if (startMergeRow != -1 && userName.equals(currentUser)) {

                        Cell shiftTotalCell = sheet.getRow(startMergeRow).createCell(9);

                        String itogString = (String) movement.get("open_close_time");
                        LocalTime itogTime = null;
                        if (exitTimeString != null) {
                            itogTime = LocalTime.parse(itogString, TIME_FORMATTER);
                            shiftTotalCell.setCellValue(itogTime.toString());
                        } else {
                            shiftTotalCell.setCellValue("Смена еще не закрыта!");
                        }


                        Cell effectiveCell = sheet.getRow(startMergeRow).createCell(10);
                        effectiveCell.setCellValue(formatDuration(totalEffectiveDuration));

                        LocalTime zero = LocalTime.parse("00:00", TIME_FORMATTER);
                        Duration shiftDuration = Duration.between(zero, itogTime);
                        Duration nonEffectiveDuration = shiftDuration.minus(totalEffectiveDuration);
                        Cell nonEffectiveCell = sheet.getRow(startMergeRow).createCell(11);
                        nonEffectiveCell.setCellValue(formatDuration(nonEffectiveDuration));

                        Cell closePlaceCell = sheet.getRow(startMergeRow).createCell(12);
                        closePlaceCell.setCellValue((String) movement.get("place"));

                        Cell openCloseTimesCell = sheet.getRow(startMergeRow).createCell(13);
                        if (OPEN != null && CLOSE != null) {
                            openCloseTimesCell.setCellValue(String.format("%s | %s", OPEN, CLOSE));
                        }

                        Cell delay = sheet.getRow(startMergeRow).createCell(14);


                        delay.setCellComment(comment("Если смена открыта позже 08:45", workbook, sheet, delay));

                        if (LocalTime.parse(OPEN, TIME_FORMATTER).isAfter(LocalTime.parse("08:45", TIME_FORMATTER))) {
                            delay.setCellValue("ЕСТЬ");
                        } else {
                            delay.setCellValue("НЕТ");
                        }

                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 9, itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 10, itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 11, itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 12, itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 13, itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 14, itogStyle);


                        for (int i = 0; i < headers.length; i++) {
                            CellRangeAddress region = new CellRangeAddress(startMergeRow, rowNum, i, i);
                            RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
                            RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
                            RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
                            RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
                        }

                        CellRangeAddress entireShift = new CellRangeAddress(startMergeRow, rowNum, 0, 14);
                        RegionUtil.setBorderTop(BorderStyle.THICK, entireShift, sheet);
                        RegionUtil.setBorderBottom(BorderStyle.THICK, entireShift, sheet);
                        RegionUtil.setBorderLeft(BorderStyle.THICK, entireShift, sheet);
                        RegionUtil.setBorderRight(BorderStyle.THICK, entireShift, sheet);

                        // Устанавливаем синий цвет для рамки
                        RegionUtil.setTopBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                        RegionUtil.setBottomBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                        RegionUtil.setLeftBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                        RegionUtil.setRightBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);


                        startMergeRow = -1;
                        totalEffectiveDuration = Duration.ZERO;
                        currentUser = null;
                    }
                }
                rowNum++;
            }


            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }


        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }
    //Индивидуальный за период и все пункты
    public byte[] generateExcelForDateBetweenAndUserName(String dateRange, String userName) throws IOException {
        DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
        String OPEN = null;
        String CLOSE = null;
        String currentUser = null;


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        try {
            // Разбиваем строку на две даты
            String[] dates = dateRange.split("/");
            LocalDate startDate = LocalDate.parse(dates[0], formatter);
            LocalDate endDate = LocalDate.parse(dates[1], formatter);


        // Получение данных из базы данных за указанный период
        List<Map<String, Object>> movements = jdbcTemplate.queryForList("SELECT * FROM movements WHERE name_user = ? AND date BETWEEN ? AND ?", userName, startDate, endDate);

            if(movements.isEmpty()){
                System.out.println("Лист пустой");
                return new byte[1];
            }
            // Создание книги и листа с использованием XSSF для .xlsx формата
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Перемещения");
            /*Стили*/

            CellStyle openStyle = workbook.createCellStyle();
            openStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            openStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            openStyle.setAlignment(HorizontalAlignment.CENTER);
            openStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle itogStyle = workbook.createCellStyle();
            Font itogFont = workbook.createFont();
            itogFont.setBold(true);
            itogStyle.setFont(itogFont);
            itogStyle.setAlignment(HorizontalAlignment.CENTER);
            itogStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            itogStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
            itogStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle closeStyle = workbook.createCellStyle();
            closeStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            closeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            closeStyle.setAlignment(HorizontalAlignment.CENTER);
            closeStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.TURQUOISE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);


            // Заголовки столбцов
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ШК", "ФИО", "Должность", "Дата", "Время прихода", "Время ухода", "Итого на пункте",  "Пункт", "Статус", "Итого за смену", "Эффективное время", "Неэффективное время", "Пункт закрытия смены", "Время открытия/закрытия смены", "Опоздание"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));


            // Сортировка данных по именам (второй столбец)
            movements.sort(Comparator.comparing(movement -> (String) movement.get("name_user")));

            int rowNum = 1;
            Duration totalEffectiveDuration = Duration.ZERO;
            int startMergeRow = -1;

            for (Map<String, Object> movement : movements) {
                Row row = sheet.createRow(rowNum);

                Cell idCell = row.createCell(0);
                idCell.setCellValue((Long) movement.get("id_user"));
                idCell.setCellStyle(dataStyle);

                Cell nameCell = row.createCell(1);
                nameCell.setCellValue((String) movement.get("name_user"));
                nameCell.setCellStyle(dataStyle);


                Cell positionCell = row.createCell(2);
                positionCell.setCellValue((String) movement.get("position_user"));
                positionCell.setCellStyle(dataStyle);

                Cell arrivalTimeCell = row.createCell(4);
                String arrivalTimeString = (String) movement.get("coming_time");
                LocalTime arrivalTime = null;
                if (arrivalTimeString != null) {
                    arrivalTime = LocalTime.parse(arrivalTimeString, TIME_FORMATTER);
                    arrivalTimeCell.setCellValue(arrivalTime.toString());
                }
                arrivalTimeCell.setCellStyle(dataStyle);

                Cell exitTimeCell = row.createCell(5);
                String exitTimeString = (String) movement.get("exit_time");
                LocalTime exitTime = null;
                if (exitTimeString != null) {
                    exitTime = LocalTime.parse(exitTimeString, TIME_FORMATTER);
                    exitTimeCell.setCellValue(exitTime.toString());
                }
                exitTimeCell.setCellStyle(dataStyle);


                Cell durationCell = row.createCell(6);


                if (arrivalTime != null && exitTime != null) {
                    Duration duration = Duration.between(arrivalTime, exitTime);
                    durationCell.setCellValue(formatDuration(duration));
                    durationCell.setCellStyle(dataStyle);
                    totalEffectiveDuration = totalEffectiveDuration.plus(duration);
                }


                Cell dateCell = row.createCell(3);
                dateCell.setCellValue(movement.get("date").toString());
                dateCell.setCellStyle(dataStyle);

                Cell placeCell = row.createCell(7);
                placeCell.setCellValue((String) movement.get("place"));
                placeCell.setCellStyle(dataStyle);

                Cell statusCell = row.createCell(8);
                statusCell.setCellValue((String) movement.get("open_close"));
                statusCell.setCellStyle(dataStyle);

//                String userName = (String) movement.get("name_user");

                if ("open".equals(movement.get("open_close")) ) {
                    if (!userName.equals(currentUser)) {
                        // Новая смена для нового пользователя
                        startMergeRow = rowNum;
                        OPEN = (String) movement.get("coming_time");
                        currentUser = userName;
                        totalEffectiveDuration = Duration.ZERO; // Сброс для новой смены

                    }else {
                        startMergeRow = rowNum;
                        OPEN = (String) movement.get("coming_time");
                    }

                    for (int i = 0; i < headers.length-6; i++) {
                        row.getCell(i).setCellStyle(openStyle);
                    }


                } else if ("close".equals(movement.get("open_close"))) {
                    CLOSE = (String) movement.get("exit_time");
                    for (int i = 0; i < headers.length-6; i++) {
                        row.getCell(i).setCellStyle(closeStyle);
                    }
                    if (startMergeRow != -1 && userName.equals(currentUser)) {

                        Cell shiftTotalCell = sheet.getRow(startMergeRow).createCell(9);
                        // это может вызвать ошибку - если кто-то закрыл но карту не приложил

                        String itogString = (String) movement.get("open_close_time");
                        LocalTime itogTime = null;
                        if (exitTimeString != null) {
                            itogTime = LocalTime.parse(itogString, TIME_FORMATTER);
                            shiftTotalCell.setCellValue(itogTime.toString());
                        }else{
                            shiftTotalCell.setCellValue("Смена еще не закрыта!");
                        }


                        Cell effectiveCell = sheet.getRow(startMergeRow).createCell(10);
                        effectiveCell.setCellValue(formatDuration(totalEffectiveDuration));

                        LocalTime zero = LocalTime.parse("00:00", TIME_FORMATTER);
                        Duration shiftDuration = Duration.between(zero, itogTime);
                        Duration nonEffectiveDuration = shiftDuration.minus(totalEffectiveDuration);
                        Cell nonEffectiveCell = sheet.getRow(startMergeRow).createCell(11);
                        nonEffectiveCell.setCellValue(formatDuration(nonEffectiveDuration));

                        Cell closePlaceCell = sheet.getRow(startMergeRow).createCell(12);
                        closePlaceCell.setCellValue((String) movement.get("place"));

                        Cell openCloseTimesCell = sheet.getRow(startMergeRow).createCell(13);
                        if(OPEN != null && CLOSE != null){
                            openCloseTimesCell.setCellValue(String.format("%s | %s", OPEN, CLOSE));
                        }

                        Cell delay =  sheet.getRow(startMergeRow).createCell(14);



                        delay.setCellComment(comment("Если смена открыта позже 08:45", workbook, sheet, delay));

                        if (LocalTime.parse(OPEN, TIME_FORMATTER).isAfter(LocalTime.parse("08:45", TIME_FORMATTER))) {
                            delay.setCellValue("ЕСТЬ");
                        }else{
                            delay.setCellValue("НЕТ");
                        }

                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 9,  itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 10, itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 11, itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 12, itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 13, itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 14, itogStyle);


                        for (int i = 0; i < headers.length; i++) {
                            CellRangeAddress region = new CellRangeAddress(startMergeRow, rowNum, i, i);
                            RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
                            RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
                            RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
                            RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
                        }

                        CellRangeAddress entireShift = new CellRangeAddress(startMergeRow, rowNum, 0, 14);
                        RegionUtil.setBorderTop(BorderStyle.THICK, entireShift, sheet);
                        RegionUtil.setBorderBottom(BorderStyle.THICK, entireShift, sheet);
                        RegionUtil.setBorderLeft(BorderStyle.THICK, entireShift, sheet);
                        RegionUtil.setBorderRight(BorderStyle.THICK, entireShift, sheet);

                        // Устанавливаем синий цвет для рамки
                        RegionUtil.setTopBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                        RegionUtil.setBottomBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                        RegionUtil.setLeftBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                        RegionUtil.setRightBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);


                        startMergeRow = -1;
                        totalEffectiveDuration = Duration.ZERO; // Reset for next shift
                        currentUser = null;//?
                    }
                }
                rowNum++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            return outputStream.toByteArray();
        }
    //Индивидуальный за период и конкретный пункт
    public byte[] generateExcelForDateBetweenAndUserNameAndPlace(String dateRange, String userName, String place) throws IOException {
        DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
        String OPEN = null;
        String CLOSE = null;
        String currentUser = null;


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        try {
        // Разбиваем строку на две даты
        String[] dates = dateRange.split("/");
        LocalDate startDate = LocalDate.parse(dates[0], formatter);
        LocalDate endDate = LocalDate.parse(dates[1], formatter);

        System.out.println(dateRange +"////"+ userName +"////" + place );
        // Получение данных из базы данных за указанный период
        List<Map<String, Object>> movements = jdbcTemplate.queryForList("SELECT * FROM movements WHERE name_user = ? AND place = ? AND date BETWEEN ? AND ?", userName, place, startDate, endDate);

        if(movements.isEmpty()){
            System.out.println("Лист пустой");
            return new byte[1];
        }
        // Создание книги и листа с использованием XSSF для .xlsx формата
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Перемещения");
        /*Стили*/

        CellStyle openStyle = workbook.createCellStyle();
        openStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        openStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        openStyle.setAlignment(HorizontalAlignment.CENTER);
        openStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle itogStyle = workbook.createCellStyle();
        Font itogFont = workbook.createFont();
        itogFont.setBold(true);
        itogStyle.setFont(itogFont);
        itogStyle.setAlignment(HorizontalAlignment.CENTER);
        itogStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        itogStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        itogStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle closeStyle = workbook.createCellStyle();
        closeStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        closeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        closeStyle.setAlignment(HorizontalAlignment.CENTER);
        closeStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.TURQUOISE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);


        // Заголовки столбцов
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ШК", "ФИО", "Должность", "Дата", "Время прихода", "Время ухода", "Итого на пункте",  "Пункт", "Статус", "Итого за смену", "Эффективное время", "Неэффективное время", "Пункт закрытия смены", "Время открытия/закрытия смены", "Опоздание"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));


        // Сортировка данных по именам (второй столбец)
        movements.sort(Comparator.comparing(movement -> (String) movement.get("name_user")));

        int rowNum = 1;
        Duration totalEffectiveDuration = Duration.ZERO;
        int startMergeRow = -1;

        for (Map<String, Object> movement : movements) {
            Row row = sheet.createRow(rowNum);

            Cell idCell = row.createCell(0);
            idCell.setCellValue((Long) movement.get("id_user"));
            idCell.setCellStyle(dataStyle);

            Cell nameCell = row.createCell(1);
            nameCell.setCellValue((String) movement.get("name_user"));
            nameCell.setCellStyle(dataStyle);


            Cell positionCell = row.createCell(2);
            positionCell.setCellValue((String) movement.get("position_user"));
            positionCell.setCellStyle(dataStyle);

            Cell arrivalTimeCell = row.createCell(4);
            String arrivalTimeString = (String) movement.get("coming_time");
            LocalTime arrivalTime = null;
            if (arrivalTimeString != null) {
                arrivalTime = LocalTime.parse(arrivalTimeString, TIME_FORMATTER);
                arrivalTimeCell.setCellValue(arrivalTime.toString());
            }
            arrivalTimeCell.setCellStyle(dataStyle);

            Cell exitTimeCell = row.createCell(5);
            String exitTimeString = (String) movement.get("exit_time");
            LocalTime exitTime = null;
            if (exitTimeString != null) {
                exitTime = LocalTime.parse(exitTimeString, TIME_FORMATTER);
                exitTimeCell.setCellValue(exitTime.toString());
            }
            exitTimeCell.setCellStyle(dataStyle);


            Cell durationCell = row.createCell(6);


            if (arrivalTime != null && exitTime != null) {
                Duration duration = Duration.between(arrivalTime, exitTime);
                durationCell.setCellValue(formatDuration(duration));
                durationCell.setCellStyle(dataStyle);
                totalEffectiveDuration = totalEffectiveDuration.plus(duration);
            }


            Cell dateCell = row.createCell(3);
            dateCell.setCellValue(movement.get("date").toString());
            dateCell.setCellStyle(dataStyle);

            Cell placeCell = row.createCell(7);
            placeCell.setCellValue((String) movement.get("place"));
            placeCell.setCellStyle(dataStyle);

            Cell statusCell = row.createCell(8);
            statusCell.setCellValue((String) movement.get("open_close"));
            statusCell.setCellStyle(dataStyle);

//                String userName = (String) movement.get("name_user");

            if ("open".equals(movement.get("open_close")) ) {
                if (!userName.equals(currentUser)) {
                    // Новая смена для нового пользователя
                    startMergeRow = rowNum;
                    OPEN = (String) movement.get("coming_time");
                    currentUser = userName;
                    totalEffectiveDuration = Duration.ZERO; // Сброс для новой смены

                }else {
                    startMergeRow = rowNum;
                    OPEN = (String) movement.get("coming_time");
                }

                for (int i = 0; i < headers.length-6; i++) {
                    row.getCell(i).setCellStyle(openStyle);
                }


            } else if ("close".equals(movement.get("open_close"))) {
                CLOSE = (String) movement.get("exit_time");
                for (int i = 0; i < headers.length-6; i++) {
                    row.getCell(i).setCellStyle(closeStyle);
                }
                if (startMergeRow != -1 && userName.equals(currentUser)) {

                    Cell shiftTotalCell = sheet.getRow(startMergeRow).createCell(9);
                    // это может вызвать ошибку - если кто-то закрыл но карту не приложил

                    String itogString = (String) movement.get("open_close_time");
                    LocalTime itogTime = null;
                    if (exitTimeString != null) {
                        itogTime = LocalTime.parse(itogString, TIME_FORMATTER);
                        shiftTotalCell.setCellValue(itogTime.toString());
                    }else{
                        shiftTotalCell.setCellValue("Смена еще не закрыта!");
                    }


                    Cell effectiveCell = sheet.getRow(startMergeRow).createCell(10);
                    effectiveCell.setCellValue(formatDuration(totalEffectiveDuration));

                    LocalTime zero = LocalTime.parse("00:00", TIME_FORMATTER);
                    Duration shiftDuration = Duration.between(zero, itogTime);
                    Duration nonEffectiveDuration = shiftDuration.minus(totalEffectiveDuration);
                    Cell nonEffectiveCell = sheet.getRow(startMergeRow).createCell(11);
                    nonEffectiveCell.setCellValue(formatDuration(nonEffectiveDuration));

                    Cell closePlaceCell = sheet.getRow(startMergeRow).createCell(12);
                    closePlaceCell.setCellValue((String) movement.get("place"));

                    Cell openCloseTimesCell = sheet.getRow(startMergeRow).createCell(13);
                    if(OPEN != null && CLOSE != null){
                        openCloseTimesCell.setCellValue(String.format("%s | %s", OPEN, CLOSE));
                    }

                    Cell delay =  sheet.getRow(startMergeRow).createCell(14);



                    delay.setCellComment(comment("Если смена открыта позже 08:45", workbook, sheet, delay));

                    if (LocalTime.parse(OPEN, TIME_FORMATTER).isAfter(LocalTime.parse("08:45", TIME_FORMATTER))) {
                        delay.setCellValue("ЕСТЬ");
                    }else{
                        delay.setCellValue("НЕТ");
                    }

                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 9,  itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 10, itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 11, itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 12, itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 13, itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 14, itogStyle);


                    for (int i = 0; i < headers.length; i++) {
                        CellRangeAddress region = new CellRangeAddress(startMergeRow, rowNum, i, i);
                        RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
                        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
                        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
                        RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
                    }

                    CellRangeAddress entireShift = new CellRangeAddress(startMergeRow, rowNum, 0, 14);
                    RegionUtil.setBorderTop(BorderStyle.THICK, entireShift, sheet);
                    RegionUtil.setBorderBottom(BorderStyle.THICK, entireShift, sheet);
                    RegionUtil.setBorderLeft(BorderStyle.THICK, entireShift, sheet);
                    RegionUtil.setBorderRight(BorderStyle.THICK, entireShift, sheet);

                    // Устанавливаем синий цвет для рамки
                    RegionUtil.setTopBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                    RegionUtil.setBottomBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                    RegionUtil.setLeftBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                    RegionUtil.setRightBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);


                    startMergeRow = -1;
                    totalEffectiveDuration = Duration.ZERO; // Reset for next shift
                    currentUser = null;//?
                }
            }
            rowNum++;
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }
    //По конкретному пункту за период
    public byte[] generateExcelForDateBetweenAndPlace(String dateRange, String place) throws IOException {
        DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

        String OPEN = null;
        String CLOSE = null;
        String currentUser = null;



        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Разбиваем строку на две даты
        String[] dates = dateRange.split("/");
        LocalDate startDate = LocalDate.parse(dates[0], formatter);
        LocalDate endDate = LocalDate.parse(dates[1], formatter);
        // Создание книги и листа с использованием XSSF для .xlsx формата
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Перемещения");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.TURQUOISE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        if(placeService.itsHubFindByName(place)){

            List<Map<String, Object>> movements = jdbcTemplate.queryForList("SELECT * FROM movements_obsh WHERE place = ? AND date BETWEEN ? AND ?", place, startDate, endDate);

            if (movements.isEmpty()) {
                System.out.println("Лист пустой");
                return new byte[1];
            }

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ШК", "ФИО", "Должность", "Дата", "Время прихода", "Время ухода", "Итого на пункте", "Пункт"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));


            // Сортировка данных по именам (второй столбец)
            movements.sort(Comparator.comparing(movement -> (String) movement.get("name_user")));

            int rowNum = 1;
            Duration totalEffectiveDuration = Duration.ZERO;
            int startMergeRow = -1;

            for (Map<String, Object> movement : movements) {
                Row row = sheet.createRow(rowNum);

                Cell idCell = row.createCell(0);
                idCell.setCellValue((Long) movement.get("id_user"));

                Cell nameCell = row.createCell(1);
                nameCell.setCellValue((String) movement.get("name_user"));


                Cell positionCell = row.createCell(2);
                positionCell.setCellValue((String) movement.get("position_user"));


                Cell dateCell = row.createCell(3);
                dateCell.setCellValue(movement.get("date").toString());

                Cell arrivalTimeCell = row.createCell(4);
                String arrivalTimeString = (String) movement.get("coming_time");
                LocalTime arrivalTime = null;
                if (arrivalTimeString != null) {
                    arrivalTime = LocalTime.parse(arrivalTimeString, TIME_FORMATTER);
                    arrivalTimeCell.setCellValue(arrivalTime.toString());
                }

                Cell exitTimeCell = row.createCell(5);
                String exitTimeString = (String) movement.get("exit_time");
                LocalTime exitTime = null;
                if (exitTimeString != null) {
                    exitTime = LocalTime.parse(exitTimeString, TIME_FORMATTER);
                    exitTimeCell.setCellValue(exitTime.toString());
                }

                Cell durationCell = row.createCell(6);

                if (arrivalTime != null && exitTime != null) {
                    Duration duration = Duration.between(arrivalTime, exitTime);
                    durationCell.setCellValue(formatDuration(duration));
                    totalEffectiveDuration = totalEffectiveDuration.plus(duration);
                }

                Cell placeCell = row.createCell(7);
                placeCell.setCellValue((String) movement.get("place"));





                rowNum++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
//ЕСЛИ НЕ ХАБ
        }else {

            List<Map<String, Object>> movements = jdbcTemplate.queryForList("SELECT * FROM movements WHERE place = ? AND date BETWEEN ? AND ?", place, startDate, endDate);


            if (movements.isEmpty()) {
                System.out.println("Лист пустой");
                return new byte[1];
            }

            /*Стили*/

            CellStyle openStyle = workbook.createCellStyle();
            openStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            openStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            openStyle.setAlignment(HorizontalAlignment.CENTER);
            openStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle itogStyle = workbook.createCellStyle();
            Font itogFont = workbook.createFont();
            itogFont.setBold(true);
            itogStyle.setFont(itogFont);
            itogStyle.setAlignment(HorizontalAlignment.CENTER);
            itogStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            itogStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
            itogStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle closeStyle = workbook.createCellStyle();
            closeStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            closeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            closeStyle.setAlignment(HorizontalAlignment.CENTER);
            closeStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);




            // Заголовки столбцов
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ШК", "ФИО", "Должность", "Дата", "Время прихода", "Время ухода", "Итого на пункте", "Пункт", "Статус", "Итого за смену", "Эффективное время", "Неэффективное время", "Пункт закрытия смены", "Время открытия/закрытия смены", "Опоздание"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));


            // Сортировка данных по именам (второй столбец)
            movements.sort(Comparator.comparing(movement -> (String) movement.get("name_user")));

            int rowNum = 1;
            Duration totalEffectiveDuration = Duration.ZERO;
            int startMergeRow = -1;

            for (Map<String, Object> movement : movements) {
                Row row = sheet.createRow(rowNum);

                Cell idCell = row.createCell(0);
                idCell.setCellValue((Long) movement.get("id_user"));
                idCell.setCellStyle(dataStyle);

                Cell nameCell = row.createCell(1);
                nameCell.setCellValue((String) movement.get("name_user"));
                nameCell.setCellStyle(dataStyle);


                Cell positionCell = row.createCell(2);
                positionCell.setCellValue((String) movement.get("position_user"));
                positionCell.setCellStyle(dataStyle);

                Cell arrivalTimeCell = row.createCell(4);
                String arrivalTimeString = (String) movement.get("coming_time");
                LocalTime arrivalTime = null;
                if (arrivalTimeString != null) {
                    arrivalTime = LocalTime.parse(arrivalTimeString, TIME_FORMATTER);
                    arrivalTimeCell.setCellValue(arrivalTime.toString());
                }
                arrivalTimeCell.setCellStyle(dataStyle);

                Cell exitTimeCell = row.createCell(5);
                String exitTimeString = (String) movement.get("exit_time");
                LocalTime exitTime = null;
                if (exitTimeString != null) {
                    exitTime = LocalTime.parse(exitTimeString, TIME_FORMATTER);
                    exitTimeCell.setCellValue(exitTime.toString());
                }
                exitTimeCell.setCellStyle(dataStyle);


                Cell durationCell = row.createCell(6);


                if (arrivalTime != null && exitTime != null) {
                    Duration duration = Duration.between(arrivalTime, exitTime);
                    durationCell.setCellValue(formatDuration(duration));
                    durationCell.setCellStyle(dataStyle);
                    totalEffectiveDuration = totalEffectiveDuration.plus(duration);
                }


                Cell dateCell = row.createCell(3);
                dateCell.setCellValue(movement.get("date").toString());
                dateCell.setCellStyle(dataStyle);

                Cell placeCell = row.createCell(7);
                placeCell.setCellValue((String) movement.get("place"));
                placeCell.setCellStyle(dataStyle);

                Cell statusCell = row.createCell(8);
                statusCell.setCellValue((String) movement.get("open_close"));
                statusCell.setCellStyle(dataStyle);

                String userName = (String) movement.get("name_user");

                if ("open".equals(movement.get("open_close"))) {
                    if (!userName.equals(currentUser)) {
                        // Новая смена для нового пользователя
                        startMergeRow = rowNum;
                        OPEN = (String) movement.get("coming_time");
                        currentUser = userName;
                        totalEffectiveDuration = Duration.ZERO; // Сброс для новой смены

                    } else {
                        startMergeRow = rowNum;
                        OPEN = (String) movement.get("coming_time");
                    }

                    for (int i = 0; i < headers.length - 6; i++) {
                        row.getCell(i).setCellStyle(openStyle);
                    }


                } else if ("close".equals(movement.get("open_close"))) {
                    CLOSE = (String) movement.get("exit_time");
                    for (int i = 0; i < headers.length - 6; i++) {
                        row.getCell(i).setCellStyle(closeStyle);
                    }
                    if (startMergeRow != -1 && userName.equals(currentUser)) {

                        Cell shiftTotalCell = sheet.getRow(startMergeRow).createCell(9);
                        // это может вызвать ошибку - если кто-то закрыл но карту не приложил

                        String itogString = (String) movement.get("open_close_time");
                        LocalTime itogTime = null;
                        if (exitTimeString != null) {
                            itogTime = LocalTime.parse(itogString, TIME_FORMATTER);
                            shiftTotalCell.setCellValue(itogTime.toString());
                        } else {
                            shiftTotalCell.setCellValue("Смена еще не закрыта!");
                        }


                        Cell effectiveCell = sheet.getRow(startMergeRow).createCell(10);
                        effectiveCell.setCellValue(formatDuration(totalEffectiveDuration));

                        LocalTime zero = LocalTime.parse("00:00", TIME_FORMATTER);
                        Duration shiftDuration = Duration.between(zero, itogTime);
                        Duration nonEffectiveDuration = shiftDuration.minus(totalEffectiveDuration);
                        Cell nonEffectiveCell = sheet.getRow(startMergeRow).createCell(11);
                        nonEffectiveCell.setCellValue(formatDuration(nonEffectiveDuration));

                        Cell closePlaceCell = sheet.getRow(startMergeRow).createCell(12);
                        closePlaceCell.setCellValue((String) movement.get("place"));

                        Cell openCloseTimesCell = sheet.getRow(startMergeRow).createCell(13);
                        if (OPEN != null && CLOSE != null) {
                            openCloseTimesCell.setCellValue(String.format("%s | %s", OPEN, CLOSE));
                        }

                        Cell delay = sheet.getRow(startMergeRow).createCell(14);


                        delay.setCellComment(comment("Если смена открыта позже 08:45", workbook, sheet, delay));

                        if (LocalTime.parse(OPEN, TIME_FORMATTER).isAfter(LocalTime.parse("08:45", TIME_FORMATTER))) {
                            delay.setCellValue("ЕСТЬ");
                        } else {
                            delay.setCellValue("НЕТ");
                        }

                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 9, itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 10, itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 11, itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 12, itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 13, itogStyle);
                        createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 14, itogStyle);


                        for (int i = 0; i < headers.length; i++) {
                            CellRangeAddress region = new CellRangeAddress(startMergeRow, rowNum, i, i);
                            RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
                            RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
                            RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
                            RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
                        }

                        CellRangeAddress entireShift = new CellRangeAddress(startMergeRow, rowNum, 0, 14);
                        RegionUtil.setBorderTop(BorderStyle.THICK, entireShift, sheet);
                        RegionUtil.setBorderBottom(BorderStyle.THICK, entireShift, sheet);
                        RegionUtil.setBorderLeft(BorderStyle.THICK, entireShift, sheet);
                        RegionUtil.setBorderRight(BorderStyle.THICK, entireShift, sheet);

                        // Устанавливаем синий цвет для рамки
                        RegionUtil.setTopBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                        RegionUtil.setBottomBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                        RegionUtil.setLeftBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                        RegionUtil.setRightBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);


                        startMergeRow = -1;
                        totalEffectiveDuration = Duration.ZERO; // Reset for next shift
                        currentUser = null;//?
                    }
                }
                rowNum++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }
    //По конкретному отделу за период
    public byte[] generateExcelForDateBetweenAndDepartment(String dateRange, String department) throws IOException {
        DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

        String OPEN = null;
        String CLOSE = null;
        String currentUser = null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");


        String[] dates = dateRange.split("/");
        LocalDate startDate = LocalDate.parse(dates[0], formatter);
        LocalDate endDate = LocalDate.parse(dates[1], formatter);


        List<Map<String, Object>> movements = jdbcTemplate.queryForList("SELECT * FROM movements WHERE department_user = ? AND date BETWEEN ? AND ?", department, startDate, endDate);


        if(movements.isEmpty()){
            System.out.println("Лист пустой");
            return new byte[1];
        }

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Перемещения");
        /*Стили*/

        CellStyle openStyle = workbook.createCellStyle();
        openStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        openStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        openStyle.setAlignment(HorizontalAlignment.CENTER);
        openStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle itogStyle = workbook.createCellStyle();
        Font itogFont = workbook.createFont();
        itogFont.setBold(true);
        itogStyle.setFont(itogFont);
        itogStyle.setAlignment(HorizontalAlignment.CENTER);
        itogStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        itogStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        itogStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle closeStyle = workbook.createCellStyle();
        closeStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        closeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        closeStyle.setAlignment(HorizontalAlignment.CENTER);
        closeStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.TURQUOISE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);


        // Заголовки столбцов
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ШК", "ФИО", "Должность", "Дата", "Время прихода", "Время ухода", "Итого на пункте",  "Пункт", "Статус", "Итого за смену", "Эффективное время", "Неэффективное время", "Пункт закрытия смены", "Время открытия/закрытия смены", "Опоздание"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));


        // Сортировка данных по именам (второй столбец)
        movements.sort(Comparator.comparing(movement -> (String) movement.get("name_user")));

        int rowNum = 1;
        Duration totalEffectiveDuration = Duration.ZERO;
        int startMergeRow = -1;

        for (Map<String, Object> movement : movements) {
            Row row = sheet.createRow(rowNum);

            Cell idCell = row.createCell(0);
            idCell.setCellValue((Long) movement.get("id_user"));
            idCell.setCellStyle(dataStyle);

            Cell nameCell = row.createCell(1);
            nameCell.setCellValue((String) movement.get("name_user"));
            nameCell.setCellStyle(dataStyle);


            Cell positionCell = row.createCell(2);
            positionCell.setCellValue((String) movement.get("position_user"));
            positionCell.setCellStyle(dataStyle);

            Cell arrivalTimeCell = row.createCell(4);
            String arrivalTimeString = (String) movement.get("coming_time");
            LocalTime arrivalTime = null;
            if (arrivalTimeString != null) {
                arrivalTime = LocalTime.parse(arrivalTimeString, TIME_FORMATTER);
                arrivalTimeCell.setCellValue(arrivalTime.toString());
            }
            arrivalTimeCell.setCellStyle(dataStyle);

            Cell exitTimeCell = row.createCell(5);
            String exitTimeString = (String) movement.get("exit_time");
            LocalTime exitTime = null;
            if (exitTimeString != null) {
                exitTime = LocalTime.parse(exitTimeString, TIME_FORMATTER);
                exitTimeCell.setCellValue(exitTime.toString());
            }
            exitTimeCell.setCellStyle(dataStyle);


            Cell durationCell = row.createCell(6);


            if (arrivalTime != null && exitTime != null) {
                Duration duration = Duration.between(arrivalTime, exitTime);
                durationCell.setCellValue(formatDuration(duration));
                durationCell.setCellStyle(dataStyle);
                totalEffectiveDuration = totalEffectiveDuration.plus(duration);
            }


            Cell dateCell = row.createCell(3);
            dateCell.setCellValue(movement.get("date").toString());
            dateCell.setCellStyle(dataStyle);

            Cell placeCell = row.createCell(7);
            placeCell.setCellValue((String) movement.get("place"));
            placeCell.setCellStyle(dataStyle);

            Cell statusCell = row.createCell(8);
            statusCell.setCellValue((String) movement.get("open_close"));
            statusCell.setCellStyle(dataStyle);

            String userName = (String) movement.get("name_user");

            if ("open".equals(movement.get("open_close")) ) {
                if (!userName.equals(currentUser)) {
                    // Новая смена для нового пользователя
                    startMergeRow = rowNum;
                    OPEN = (String) movement.get("coming_time");
                    currentUser = userName;
                    totalEffectiveDuration = Duration.ZERO; // Сброс для новой смены

                }else {
                    startMergeRow = rowNum;
                    OPEN = (String) movement.get("coming_time");
                }

                for (int i = 0; i < headers.length-6; i++) {
                    row.getCell(i).setCellStyle(openStyle);
                }


            } else if ("close".equals(movement.get("open_close"))) {
                CLOSE = (String) movement.get("exit_time");
                for (int i = 0; i < headers.length-6; i++) {
                    row.getCell(i).setCellStyle(closeStyle);
                }
                if (startMergeRow != -1 && userName.equals(currentUser)) {

                    Cell shiftTotalCell = sheet.getRow(startMergeRow).createCell(9);
                    // это может вызвать ошибку - если кто-то закрыл но карту не приложил

                    String itogString = (String) movement.get("open_close_time");
                    LocalTime itogTime = null;
                    if (exitTimeString != null) {
                        itogTime = LocalTime.parse(itogString, TIME_FORMATTER);
                        shiftTotalCell.setCellValue(itogTime.toString());
                    }else{
                        shiftTotalCell.setCellValue("Смена еще не закрыта!");
                    }


                    Cell effectiveCell = sheet.getRow(startMergeRow).createCell(10);
                    effectiveCell.setCellValue(formatDuration(totalEffectiveDuration));

                    LocalTime zero = LocalTime.parse("00:00", TIME_FORMATTER);
                    Duration shiftDuration = Duration.between(zero, itogTime);
                    Duration nonEffectiveDuration = shiftDuration.minus(totalEffectiveDuration);
                    Cell nonEffectiveCell = sheet.getRow(startMergeRow).createCell(11);
                    nonEffectiveCell.setCellValue(formatDuration(nonEffectiveDuration));

                    Cell closePlaceCell = sheet.getRow(startMergeRow).createCell(12);
                    closePlaceCell.setCellValue((String) movement.get("place"));

                    Cell openCloseTimesCell = sheet.getRow(startMergeRow).createCell(13);
                    if(OPEN != null && CLOSE != null){
                        openCloseTimesCell.setCellValue(String.format("%s | %s", OPEN, CLOSE));
                    }

                    Cell delay =  sheet.getRow(startMergeRow).createCell(14);



                    delay.setCellComment(comment("Если смена открыта позже 08:45", workbook, sheet, delay));

                    if (LocalTime.parse(OPEN, TIME_FORMATTER).isAfter(LocalTime.parse("08:45", TIME_FORMATTER))) {
                        delay.setCellValue("ЕСТЬ");
                    }else{
                        delay.setCellValue("НЕТ");
                    }

                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 9,  itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 10, itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 11, itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 12, itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 13, itogStyle);
                    createAndStyleMergedRegion(sheet, startMergeRow, rowNum, 14, itogStyle);


                    for (int i = 0; i < headers.length; i++) {
                        CellRangeAddress region = new CellRangeAddress(startMergeRow, rowNum, i, i);
                        RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
                        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
                        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
                        RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
                    }

                    CellRangeAddress entireShift = new CellRangeAddress(startMergeRow, rowNum, 0, 14);
                    RegionUtil.setBorderTop(BorderStyle.THICK, entireShift, sheet);
                    RegionUtil.setBorderBottom(BorderStyle.THICK, entireShift, sheet);
                    RegionUtil.setBorderLeft(BorderStyle.THICK, entireShift, sheet);
                    RegionUtil.setBorderRight(BorderStyle.THICK, entireShift, sheet);

                    // Устанавливаем синий цвет для рамки
                    RegionUtil.setTopBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                    RegionUtil.setBottomBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                    RegionUtil.setLeftBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);
                    RegionUtil.setRightBorderColor(IndexedColors.YELLOW.getIndex(), entireShift, sheet);


                    startMergeRow = -1;
                    totalEffectiveDuration = Duration.ZERO; // Reset for next shift
                    currentUser = null;//?
                }
            }
            rowNum++;
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }


    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%02d:%02d", hours, minutes);
    }

    private void createAndStyleMergedRegion(XSSFSheet sheet, int firstRow, int lastRow, int col, CellStyle style) {
        CellRangeAddress mergedRegion = new CellRangeAddress(firstRow, lastRow, col, col);
        sheet.addMergedRegion(mergedRegion);

        // Применяем стиль к объединенному диапазону ячеек
        for (int rowNum = firstRow; rowNum <= lastRow; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                row = sheet.createRow(rowNum);
            }
            Cell cell = row.getCell(col);
            if (cell == null) {
                cell = row.createCell(col);
            }
            cell.setCellStyle(style);
        }
    }


    private  Comment comment(String comm, XSSFWorkbook workbook,XSSFSheet sheet, Cell targetCell){
        CreationHelper creationHelper = workbook.getCreationHelper();
        ClientAnchor anchor = creationHelper.createClientAnchor();


        anchor.setCol1(targetCell.getColumnIndex()); // начальная колонка
        anchor.setCol2(targetCell.getColumnIndex() + 2); // конечная колонка
        anchor.setRow1(targetCell.getRowIndex()); // начальная строка
        anchor.setRow2(targetCell.getRowIndex()+ 2); // конечная строка


        Comment comment = sheet.createDrawingPatriarch().createCellComment(anchor);
        RichTextString commentString = creationHelper.createRichTextString(comm);
        comment.setString(commentString);
        return comment;
    }



    private void sendExcelFile(byte[] bytes) throws IOException {

        File tempFile = File.createTempFile("file", ".xlsx");

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write(bytes);
        }
        tempFile.deleteOnExit();

    }





}
