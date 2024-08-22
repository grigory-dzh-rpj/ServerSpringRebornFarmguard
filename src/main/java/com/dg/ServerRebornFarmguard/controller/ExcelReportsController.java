package com.dg.ServerRebornFarmguard.controller;

import com.dg.ServerRebornFarmguard.exception.ExceptionHttp;
import com.dg.ServerRebornFarmguard.model.ReqDateBetweenAndNameUserAndPlace;
import com.dg.ServerRebornFarmguard.service.reports.excel.CreateDiagrams;
import com.dg.ServerRebornFarmguard.service.reports.excel.MainReports;
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
public class ExcelReportsController {



    @Autowired
    MainReports mainReports;

    @Autowired
    CreateDiagrams createDiagrams;

    /*Excel отчет */


    @PostMapping("/excelDateBetweenAndUserName")
    public ResponseEntity<byte[]> generateExcelForDateBetweenAndUserName(@RequestBody ReqDateBetweenAndNameUserAndPlace req) {
        try {
            String dateRange = req.getDateFrom()+"/"+req.getDateTo();
            byte[] bytes = mainReports.generateExcelForDateBetweenAndUserName(dateRange, req.getNameUser());
            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(bytes);
        }catch (Exception e){
            log.error("Ошибка при генерации Excel: ", e);
            return ExceptionHttp.MainExeption();
        }
    }

    @PostMapping("/excelDateBetweenAndUserNameAndPlace")
    public ResponseEntity<byte[]> generateExcelForDateBetweenAndUserNameAndPlace(@RequestBody ReqDateBetweenAndNameUserAndPlace req) {
        try {
            String dateRange = req.getDateFrom()+"/"+req.getDateTo();
            byte[] bytes = mainReports.generateExcelForDateBetweenAndUserNameAndPlace(dateRange, req.getNameUser(), req.getPlace());
            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(bytes);
        }catch (Exception e){
            log.error("Ошибка при генерации Excel: ", e);
            return ExceptionHttp.MainExeption();
        }
    }

    /*Общий отчет*/

    @PostMapping("/excelDateBetween")
    public ResponseEntity<byte[]> generateExcelForDateBetween(@RequestBody ReqDateBetweenAndNameUserAndPlace req) {
        try {
            String dateRange = req.getDateFrom()+"/"+req.getDateTo();
            byte[] bytes = mainReports.generateExcelForDateBetween(dateRange);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(bytes);
        }catch (Exception e){
            log.error("Ошибка при генерации Excel: ", e);
            return ExceptionHttp.MainExeption();
        }
    }

    /*Диаграмма*/
    @PostMapping("/createDiagram")
    public ResponseEntity<ByteArrayResource> diagramaEff(@RequestBody ReqDateBetweenAndNameUserAndPlace req) {
        try {
            File file = createDiagrams.createEffTimeDiagramOnDifferentPlaceForUI(req.getNameUser(), req.getDateFrom(), req.getDateTo());
            byte[] bytes = new byte[(int) file.length()];
            FileInputStream in = new FileInputStream(file);
            in.read(bytes);
            in.close();
            ByteArrayResource resource = new ByteArrayResource(bytes); // Создайте ByteArrayResource

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Ошибка при генерации Excel: ", e);
            return ExceptionHttp.MainExeption();
        }
    }


    /*Эффективное время*/
    @PostMapping("/effTime")
    public ResponseEntity<String> effTime(@RequestBody ReqDateBetweenAndNameUserAndPlace req){
        try{
            String effTime = createDiagrams.effectiveTimeByName(req.getNameUser(), req.getDateFrom(), req.getDateTo());
            return ResponseEntity.ok(effTime);
        }catch (Exception e){
            log.error("Ошибка при расчете EffTime: ", e);
            return ExceptionHttp.MainExeption();
        }
    }

    @PostMapping("/totalTime")
    public ResponseEntity<String> totalTime(@RequestBody ReqDateBetweenAndNameUserAndPlace req){
        try{
            String totalTimeByName = createDiagrams.totalTimeByName(req.getNameUser(), req.getDateFrom(), req.getDateTo());
            return ResponseEntity.ok(totalTimeByName);
        }catch (Exception e){
            log.error("Ошибка при расчете TotalTime: ", e);
            return ExceptionHttp.MainExeption();
        }
    }


}
