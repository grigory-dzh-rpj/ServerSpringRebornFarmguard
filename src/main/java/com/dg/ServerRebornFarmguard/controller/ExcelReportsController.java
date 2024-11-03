package com.dg.ServerRebornFarmguard.controller;

import com.dg.ServerRebornFarmguard.exception.ExceptionHttp;
import com.dg.ServerRebornFarmguard.model.ReqDateBetweenAndNameUserAndPlace;
import com.dg.ServerRebornFarmguard.service.reports.excel.CreateDiagrams;
import com.dg.ServerRebornFarmguard.service.reports.excel.MainReports;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;

@RestController
@RequestMapping("/excel")
@Slf4j
@Tag(name = "ExcelReport Controller", description = "API для генерации Excel отчетов, какие-то методы возможно есть в MovementsController")

public class ExcelReportsController {



    @Autowired
    MainReports mainReports;

    @Autowired
    CreateDiagrams createDiagrams;

    @PostMapping("/excelDateBetweenAndUserName")
    @Operation(summary = "Экспорт данных по диапазону дат и имени пользователя в Excel",
            description = "Генерирует Excel-файл со всеми данными в указанном диапазоне дат для определенного пользователя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Excel-файл успешно сгенерирован",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "500", description = "Ошибка при генерации файла")
    })
    public ResponseEntity<byte[]> generateExcelForDateBetweenAndUserName(@RequestBody ReqDateBetweenAndNameUserAndPlace req) {
        try {
            String dateRange = req.getDateFrom()+"/"+req.getDateTo();
            byte[] bytes = mainReports.generateExcelForDateBetweenAndUserName(dateRange, req.getNameUser());
            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(bytes);
        }catch (Exception e){
            log.error("Ошибка в /excelDateBetweenAndUserName",e);
            return ExceptionHttp.MainExeption();
        }
    }

    @PostMapping("/excelDateBetweenAndUserNameAndPlace")
    @Operation(summary = "Экспорт данных по диапазону дат, имени пользователя и месту в Excel",
            description = "Генерирует Excel-файл со всеми данными для указанного диапазона дат, имени пользователя и места.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Excel-файл успешно сгенерирован",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "500", description = "Ошибка при генерации файла")
    })
    public ResponseEntity<byte[]> generateExcelForDateBetweenAndUserNameAndPlace(@RequestBody ReqDateBetweenAndNameUserAndPlace req) {
        try {
            String dateRange = req.getDateFrom()+"/"+req.getDateTo();
            byte[] bytes = mainReports.generateExcelForDateBetweenAndUserNameAndPlace(dateRange, req.getNameUser(), req.getPlace());
            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(bytes);
        }catch (Exception e){
            log.error("Ошибка в /excelDateBetweenAndUserName",e);
            return ExceptionHttp.MainExeption();
        }
    }

    /*Общий отчет*/
    @PostMapping("/excelDateBetween")
    @Operation(summary = "Экспорт данных по диапазону дат в Excel",
            description = "Генерирует Excel-файл со всеми данными за указанный диапазон дат.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Excel-файл успешно сгенерирован",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "500", description = "Ошибка при генерации файла")
    })
    public ResponseEntity<byte[]> generateExcelForDateBetween(@RequestBody ReqDateBetweenAndNameUserAndPlace req) {
        try {
            String dateRange = req.getDateFrom()+"/"+req.getDateTo();
            byte[] bytes = mainReports.generateExcelForDateBetween(dateRange);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(bytes);
        }catch (Exception e){
            log.error("Ошибка в /excelDateBetween",e);
            return ExceptionHttp.MainExeption();
        }
    }

    /*Диаграмма*/
    @PostMapping("/createDiagram")
    @Operation(summary = "Создание диаграммы эффективности",
            description = "Создает диаграмму эффективности времени для определенного пользователя и диапазона дат.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Диаграмма успешно создана",
                    content = @Content(mediaType = "image/png")),
            @ApiResponse(responseCode = "500", description = "Ошибка при создании диаграммы")
    })
    public ResponseEntity<ByteArrayResource> diagramaEff(@RequestBody ReqDateBetweenAndNameUserAndPlace req) {
        try {
            File file = createDiagrams.createEffTimeDiagramOnDifferentPlaceForUI(req.getNameUser(), req.getDateFrom(), req.getDateTo());
            byte[] bytes = new byte[(int) file.length()];
            FileInputStream in = new FileInputStream(file);
            in.read(bytes);
            in.close();
            ByteArrayResource resource = new ByteArrayResource(bytes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Ошибка в /createDiagram",e);
            return ExceptionHttp.MainExeption();
        }
    }


    /*Эффективное время*/
    @PostMapping("/effTime")
    @Operation(summary = "Получить эффективное время",
            description = "Возвращает эффективное время пользователя за указанный диапазон дат.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получено эффективное время",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Ошибка при получении данных")
    })
    public ResponseEntity<String> effTime(@RequestBody ReqDateBetweenAndNameUserAndPlace req){
        try{
            String effTime = createDiagrams.effectiveTimeByName(req.getNameUser(), req.getDateFrom(), req.getDateTo());
            return ResponseEntity.ok(effTime);
        }catch (Exception e){
            log.error("Ошибка в /effTime",e);
            return ExceptionHttp.MainExeption();
        }
    }

    @PostMapping("/totalTime")
    @Operation(summary = "Получить общее время",
            description = "Возвращает общее время пользователя за указанный диапазон дат.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получено общее время",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Ошибка при получении данных")
    })
    public ResponseEntity<String> totalTime(@RequestBody ReqDateBetweenAndNameUserAndPlace req){
        try{
            String totalTimeByName = createDiagrams.totalTimeByName(req.getNameUser(), req.getDateFrom(), req.getDateTo());
            return ResponseEntity.ok(totalTimeByName);
        }catch (Exception e){
            log.error("Ошибка в /totalTim",e);
            return ExceptionHttp.MainExeption();
        }
    }


}
