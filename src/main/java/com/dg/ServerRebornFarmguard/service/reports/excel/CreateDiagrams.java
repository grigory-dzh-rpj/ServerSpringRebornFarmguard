package com.dg.ServerRebornFarmguard.service.reports.excel;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendPhoto;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;

import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
//import java.awt.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.AttributedString;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class CreateDiagrams {


    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String getFirstDayOfMonth() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        String formatter = firstDayOfMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return formatter;
    }

    private String getLastDayOfMonth() {
        LocalDate today = LocalDate.now();
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        String formatter = lastDayOfMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));


        return formatter;
    }

    public void createEffTimeDiagramOnDifferentPlaceAndSendBot(long chatId, String id, TelegramBot bot) {
        String query = "SELECT place, TIME_FORMAT(SEC_TO_TIME(SUM(TIME_TO_SEC(time_at_place))), '%H:%i:%s') AS eff_time " +
                "FROM movements " +
                "WHERE time_at_place IS NOT NULL AND date IS NOT NULL AND date BETWEEN ? AND ? AND id_user = ?" +
                "GROUP BY place " +
                "ORDER BY place";

        Map<String, Integer> placeEffTime = new HashMap<>();
        Map<String, String> placeEffTimeString = new HashMap<>();
        File tempFile = null;

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query, getFirstDayOfMonth(), getLastDayOfMonth(), id);
            int fullTime = 0;
            for (Map<String, Object> row : results) {
                String place = (String) row.get("place");
                String effTime = (String) row.get("eff_time"); // Извлечение значения eff_time как строки

                String[] strings = effTime.split(":");
                String onMapTime = strings[0] + ":" + strings[1];

                int totalSeconds = parseStringAsSeconds(effTime);
                placeEffTime.put(place, totalSeconds / 3600);
                placeEffTimeString.put(place, onMapTime);
                fullTime += totalSeconds;
            }

            DefaultPieDataset datasetPie = new DefaultPieDataset();
            for (Map.Entry<String, Integer> entry : placeEffTime.entrySet()) {
                datasetPie.setValue(entry.getKey(), entry.getValue());
            }



            BarRenderer renderer = new BarRenderer();
            renderer.setDefaultItemLabelsVisible(true);
            Paint violetColor = new Color(128, 0, 128);
            renderer.setSeriesPaint(0, violetColor);

            JFreeChart pieChart = ChartFactory.createPieChart(
                    "Время на пунктах: " + formatSecondsAsHHmm(fullTime),
                    datasetPie,
                    true,
                    true,
                    false
            );

            // Настройка фона
            pieChart.setBackgroundPaint(new GradientPaint(
                    new Point(0, 0), new Color(147, 112, 219),
                    new Point(400, 200), new Color(0, 0, 0)
            ));

            // Настройка заголовка
            TextTitle title = pieChart.getTitle();
            title.setFont(new Font("Arial", Font.BOLD, 24));
            title.setPaint(Color.WHITE);
            title.setHorizontalAlignment(HorizontalAlignment.CENTER);

            PiePlot plot = (PiePlot) pieChart.getPlot();

            // Создание градиентов для секторов
            GradientPaint[] gradients = {
                    new GradientPaint(0f, 00f, new Color(255, 99, 71), 400f, 200f, new Color(255, 200, 200)),
                    new GradientPaint(0f, 00f, new Color(100, 149, 237), 400f, 200f, new Color(200, 220, 255)),
                    new GradientPaint(0f, 00f, new Color(50, 205, 50), 400f, 200f, new Color(200, 255, 200)),
                    new GradientPaint(0f, 00f, new Color(255, 215, 0), 400f, 200f, new Color(255, 255, 200)),
                    new GradientPaint(0f, 00f, new Color(147, 112, 219), 400f, 200f, new Color(220, 200, 255)),
                    new GradientPaint(0f, 00f, new Color(255, 127, 80), 400f, 200f, new Color(255, 220, 200)),
                    new GradientPaint(0f, 00f, new Color(19, 100, 255), 400f, 200f, new Color(200, 240, 255)),
                    new GradientPaint(0f, 00f, new Color(0, 191, 255), 400f, 200f, new Color(177, 40, 95)),
                    new GradientPaint(0f, 00f, new Color(66, 11, 25), 400f, 200f, new Color(200, 240, 255)),
                    new GradientPaint(0f, 00f, new Color(0, 151, 155), 400f, 200f, new Color(200, 240, 255)),
            };

            // Установка градиентов для секторов
            List<Comparable> keys = datasetPie.getKeys();
            for (int i = 0; i < keys.size(); i++) {
                plot.setSectionPaint(keys.get(i), gradients[i % gradients.length]);
            }

            // Настройка меток секторов
            plot.setLabelGenerator(new PieSectionLabelGenerator() {
                @Override
                public String generateSectionLabel(PieDataset dataset, Comparable key) {
                    String value = placeEffTimeString.get(key);
                    return key + ": " + value;
                }

                @Override
                public AttributedString generateAttributedSectionLabel(PieDataset dataset, Comparable key) {
                    return null;
                }
            });
            plot.setBackgroundPaint(new GradientPaint(
                    new Point(0, 0), new Color(147, 112, 219),
                    new Point(400, 200), new Color(255, 255, 255)));
            plot.setLabelFont(new Font("Arial", Font.PLAIN, 12));


            plot.setLabelPaint( new Color(255, 255, 255));
            plot.setLabelBackgroundPaint(new GradientPaint(
                    new Point(0, 0), new Color(147, 112, 219),
                    new Point(400, 200), new Color(0, 0, 0)
            ));
            plot.setLabelOutlinePaint(null);
            plot.setLabelShadowPaint(null);

            // Добавление градиента и тени
            plot.setSectionOutlinesVisible(false);
            plot.setShadowPaint(new GradientPaint(
                    new Point(0, 0), new Color(147, 112, 219),
                    new Point(400, 200), new Color(255, 255, 255)));
            plot.setShadowXOffset(4.0);
            plot.setShadowYOffset(4.0);

            // Настройка легенды
            LegendTitle legend = pieChart.getLegend();
            legend.setItemFont(new Font("Arial", Font.PLAIN, 12));
            legend.setBackgroundPaint(new Color(147, 112, 219, 100));
            legend.setItemPaint(Color.WHITE);

            int width = 600; // Увеличим размер для лучшей детализации
            int height = 400;

            BufferedImage chartImage = pieChart.createBufferedImage(width, height);
            tempFile = File.createTempFile("diagrams", ".png");
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                ImageIO.write(chartImage, "png", outputStream);
            }

            bot.execute(new SendPhoto(chatId, tempFile));
            tempFile.deleteOnExit();

        } catch (Exception e) {
            e.printStackTrace();
            if(tempFile != null) {
                tempFile.deleteOnExit();
            }
        }finally {

        tempFile.deleteOnExit();
     }
    }


    public File createEffTimeDiagramOnDifferentPlaceForUI(String name, String dateFrom, String dateTo) {
        String query = "SELECT place, TIME_FORMAT(SEC_TO_TIME(SUM(TIME_TO_SEC(time_at_place))), '%H:%i:%s') AS eff_time " +
                "FROM movements " +
                "WHERE time_at_place IS NOT NULL AND date IS NOT NULL AND date BETWEEN ? AND ? AND name_user = ?" +
                "GROUP BY place " +
                "ORDER BY place";

        Map<String, Integer> placeEffTime = new HashMap<>();
        Map<String, String> placeEffTimeString = new HashMap<>();
        File tempFile = null;

        try {
            tempFile = File.createTempFile("diagrams", ".png");
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query, dateFrom,dateTo, name);
            int fullTime = 0;
            for (Map<String, Object> row : results) {
                String place = (String) row.get("place");
                String effTime = (String) row.get("eff_time"); // Извлечение значения eff_time как строки

                String[] strings = effTime.split(":");
                String onMapTime = strings[0] + ":" + strings[1];

                int totalSeconds = parseStringAsSeconds(effTime);
                placeEffTime.put(place, totalSeconds / 3600);
                placeEffTimeString.put(place, onMapTime);
                fullTime += totalSeconds;
            }

            DefaultPieDataset datasetPie = new DefaultPieDataset();
            for (Map.Entry<String, Integer> entry : placeEffTime.entrySet()) {
                datasetPie.setValue(entry.getKey(), entry.getValue());
            }



            BarRenderer renderer = new BarRenderer();
            renderer.setDefaultItemLabelsVisible(true);
            Paint violetColor = new Color(128, 0, 128);
            renderer.setSeriesPaint(0, violetColor);



            JFreeChart pieChart = ChartFactory.createPieChart(
                    "Время на пунктах: " + formatSecondsAsHHmm(fullTime),
                    datasetPie,
                    true,
                    true,
                    false
            );

            // Настройка фона
            pieChart.setBackgroundPaint(new GradientPaint(
                    new Point(0, 0), new Color(147, 112, 219),
                    new Point(400, 200), new Color(0, 0, 0)
            ));

            // Настройка заголовка
            TextTitle title = pieChart.getTitle();
            title.setFont(new Font("Arial", Font.BOLD, 24));
            title.setPaint(Color.WHITE);
            title.setHorizontalAlignment(HorizontalAlignment.CENTER);

            PiePlot plot = (PiePlot) pieChart.getPlot();

            // Создание градиентов для секторов
            GradientPaint[] gradients = {
                    new GradientPaint(0f, 00f, new Color(66, 11, 25), 400f, 200f, new Color(200, 240, 255)),
                    new GradientPaint(0f, 00f, new Color(0, 151, 155), 400f, 200f, new Color(200, 240, 255)),
                    new GradientPaint(0f, 00f, new Color(255, 99, 71), 400f, 200f, new Color(255, 200, 200)),
                    new GradientPaint(0f, 00f, new Color(100, 149, 237), 400f, 200f, new Color(200, 220, 255)),
                    new GradientPaint(0f, 00f, new Color(50, 205, 50), 400f, 200f, new Color(200, 255, 200)),
                    new GradientPaint(0f, 00f, new Color(255, 215, 0), 400f, 200f, new Color(255, 255, 200)),
                    new GradientPaint(0f, 00f, new Color(147, 112, 219), 400f, 200f, new Color(220, 200, 255)),
                    new GradientPaint(0f, 00f, new Color(255, 127, 80), 400f, 200f, new Color(255, 220, 200)),
                    new GradientPaint(0f, 00f, new Color(19, 100, 255), 400f, 200f, new Color(200, 240, 255)),
                    new GradientPaint(0f, 00f, new Color(0, 191, 255), 400f, 200f, new Color(177, 40, 95)),

            };

            // Установка градиентов для секторов
            List<Comparable> keys = datasetPie.getKeys();
            for (int i = 0; i < keys.size(); i++) {
                plot.setSectionPaint(keys.get(i), gradients[i % gradients.length]);
            }

            // Настройка меток секторов
            plot.setLabelGenerator(new PieSectionLabelGenerator() {
                @Override
                public String generateSectionLabel(PieDataset dataset, Comparable key) {
                    String value = placeEffTimeString.get(key);
                    return key + ": " + value;
                }

                @Override
                public AttributedString generateAttributedSectionLabel(PieDataset dataset, Comparable key) {
                    return null;
                }
            });
            plot.setBackgroundPaint(new GradientPaint(
                    new Point(0, 0), new Color(147, 112, 219),
                    new Point(400, 200), new Color(255, 255, 255)));
            plot.setLabelPaint( new Color(255, 255, 255));
            plot.setLabelBackgroundPaint(new GradientPaint(
                    new Point(0, 0), new Color(147, 112, 219),
                    new Point(400, 200), new Color(0, 0, 0)
            ));
            plot.setLabelFont(new Font("Arial", Font.PLAIN, 12));
            plot.setLabelOutlinePaint(null);
            plot.setLabelShadowPaint(null);

            // Добавление градиента и тени
            plot.setSectionOutlinesVisible(false);
            plot.setShadowPaint(new GradientPaint(
                    new Point(0, 0), new Color(147, 112, 219),
                    new Point(400, 200), new Color(255, 255, 255)));
            plot.setShadowXOffset(4.0);
            plot.setShadowYOffset(4.0);

            // Настройка легенды
            LegendTitle legend = pieChart.getLegend();
            legend.setItemFont(new Font("Arial", Font.PLAIN, 12));
            legend.setBackgroundPaint(new Color(147, 112, 219, 100));
            legend.setItemPaint(Color.WHITE);

            int width = 600; // Увеличим размер для лучшей детализации
            int height = 400;

            BufferedImage chartImage = pieChart.createBufferedImage(width, height);


            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                ImageIO.write(chartImage, "png", outputStream);
            }

            return tempFile;




        } catch (Exception e) {
            e.printStackTrace();
            if(tempFile != null) {
                tempFile.deleteOnExit();
            }
            return null;
        }finally {

            tempFile.deleteOnExit();
        }
    }

    private int parseStringAsSeconds(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return hours * 3600 + minutes * 60 + seconds;
    }

    private String formatSecondsAsHHmm(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    public String effectiveTime(String id, String dateFirst, String dateLast){
        String query = "SELECT SEC_TO_TIME(SUM(TIME_TO_SEC(TIME_FORMAT(time_at_place, '%H:%i')))) AS total_time FROM movements WHERE id_user = ? AND date BETWEEN ? AND ? AND time_at_place IS NOT NULL ";
        return jdbcTemplate.queryForObject(query, new Object[]{id, dateFirst, dateLast}, (rs, rowNum) -> {

            String totalTime = rs.getString("total_time");
            if(totalTime != null) {
                return formatSecondsAsHHmm(parseStringAsSeconds(totalTime));
            }else{
                return "00:00";
            }
        });
    }

    public String totalTime(String id, String dateFirst, String dateLast){
        String query = "SELECT SEC_TO_TIME(SUM(TIME_TO_SEC(TIME_FORMAT(open_close_time, '%H:%i')))) AS total_time FROM movements WHERE id_user = ? AND date BETWEEN ? AND ? AND open_close_time IS NOT NULL ";
        return jdbcTemplate.queryForObject(query, new Object[]{id, dateFirst, dateLast}, (rs, rowNum) -> {
            String totalTime = rs.getString("total_time");
            if(totalTime != null) {
                return formatSecondsAsHHmm(parseStringAsSeconds(totalTime));
            }else{
                return "00:00";
            }
        });
    }


    public String effectiveTimeByName(String name, String dateFirst, String dateLast){
        String query = "SELECT SEC_TO_TIME(SUM(TIME_TO_SEC(TIME_FORMAT(time_at_place, '%H:%i')))) AS total_time FROM movements WHERE name_user = ? AND date BETWEEN ? AND ? AND time_at_place IS NOT NULL ";
        return jdbcTemplate.queryForObject(query, new Object[]{name, dateFirst, dateLast}, (rs, rowNum) -> {

            String totalTime = rs.getString("total_time");
            if(totalTime != null) {
                return formatSecondsAsHHmm(parseStringAsSeconds(totalTime));
            }else{
                return "00:00";
            }
        });
    }

    public String totalTimeByName(String name, String dateFirst, String dateLast){
        String query = "SELECT SEC_TO_TIME(SUM(TIME_TO_SEC(TIME_FORMAT(open_close_time, '%H:%i')))) AS total_time FROM movements WHERE name_user = ? AND date BETWEEN ? AND ? AND open_close_time IS NOT NULL ";
        return jdbcTemplate.queryForObject(query, new Object[]{name, dateFirst, dateLast}, (rs, rowNum) -> {
            String totalTime = rs.getString("total_time");
            if(totalTime != null) {
                return formatSecondsAsHHmm(parseStringAsSeconds(totalTime));
            }else{
                return "00:00";
            }
        });
    }

}