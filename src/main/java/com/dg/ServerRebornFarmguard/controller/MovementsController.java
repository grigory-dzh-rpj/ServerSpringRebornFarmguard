package com.dg.ServerRebornFarmguard.controller;

import com.dg.ServerRebornFarmguard.entity.MovementsEntity;
import com.dg.ServerRebornFarmguard.exception.ExceptionHttp;
import com.dg.ServerRebornFarmguard.model.ReqDateAndNameUser;
import com.dg.ServerRebornFarmguard.model.ReqDateAndPlace;
import com.dg.ServerRebornFarmguard.model.ReqDateAndPlaceAndUserName;
import com.dg.ServerRebornFarmguard.model.ReqDateBetweenAndNameUserAndPlace;
import com.dg.ServerRebornFarmguard.service.MovementsService;
import com.dg.ServerRebornFarmguard.service.TelegramBotService;
import com.dg.ServerRebornFarmguard.service.reports.excel.MainReports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/move")
public class MovementsController {

    @Autowired
    MovementsService movementsService;




    @PostMapping("/logic")
    public ResponseEntity<String> moving(@RequestBody String macIdPrefix) {
        try {
            return ResponseEntity.ok(movementsService.logic(macIdPrefix));
        }catch (Exception e){
            e.printStackTrace();
            return ExceptionHttp.MainExeption();
        }
    }


    /*for sockets*/

//    @Autowired
//    private SimpMessagingTemplate messagingTemplate;
//
//    @MessageMapping("/logic")
//    @SendTo("/topic/responses")
//    public String handleLogic(String macIdPrefix) {
//        String response = movementsService.logic(macIdPrefix);
//        messagingTemplate.convertAndSend("/topic/responses", response);
//        return "OK";
//    }

    /**/

    @PostMapping("/close")
    public ResponseEntity closing(@RequestBody Long id){
        try {
          String s =  movementsService.closeStatus(id);
            return ResponseEntity.ok(s);
        }catch (Exception e){
            e.printStackTrace();
            return ExceptionHttp.MainExeption();
        }
    }

    @PostMapping("/closeByName")
    public ResponseEntity closingByName(@RequestBody String name){
        try {
            String s =  movementsService.closeStatusByName(name);
            return ResponseEntity.ok(s);
        }catch (Exception e){
            e.printStackTrace();
            return ExceptionHttp.MainExeption();
        }
    }

    @PostMapping("/nowOnPlace")
    public  ResponseEntity<List<MovementsEntity>> nowOnPlace(@RequestBody String place){
        try{
            List<MovementsEntity> movementsEntites = movementsService.nowOnPlace(place);
            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();

        }
    }



    @GetMapping("/startBot")
    public ResponseEntity<String> startBot(){
        TelegramBotService telegramBotService = new TelegramBotService();
        telegramBotService.createBot("6784964838:AAFjhqGhE9LrO-QhWvjCOVp_TO_43oeribg");
        return ResponseEntity.ok("Бот запущен");
    }



    @Autowired
    private MainReports excelGenerator;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMovements() throws IOException {
        byte[] excelData = excelGenerator.generateExcel();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "move2.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    //http://localhost:7778/move/exportWithDateRange?dateRange=2024-05-31/2024-05-31
    @GetMapping("/exportWithDateRange")
    public ResponseEntity<byte[]> exportMovementsWithDateRange(@RequestParam("dateRange") String dateRange) throws IOException {
        byte[] excelData = excelGenerator.generateExcelForDateBetween(dateRange);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "move_with_date.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    @GetMapping("/exportWithDateRangeAndUserName")
    public ResponseEntity<byte[]> exportMovementsWithDateRangeAndUserName(@RequestParam("dateRange") String dateRange, @RequestParam("userName") String userName) throws IOException {
        byte[] excelData = excelGenerator.generateExcelForDateBetweenAndUserName(dateRange, userName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        headers.setContentDispositionFormData("attachment", "move_"+userName+"of"+dateRange+".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    //http://localhost:7778/move/exportWithDateRangeAndPlace?dateRange=2024-05-31/2024-05-31&place=Ферма 1
    @GetMapping("/exportWithDateRangeAndPlace")
    public ResponseEntity<byte[]> exportMovementsWithDateRangeAndPlace(@RequestParam("dateRange") String dateRange, @RequestParam("place") String place) throws IOException {
        byte[] excelData = excelGenerator.generateExcelForDateBetweenAndPlace(dateRange, place);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "move_with_date.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    @GetMapping("/exportWithDateRangeAndDepartment")
    public ResponseEntity<byte[]> exportMovementsWithDateRangeAndDepartment(@RequestParam("dateRange") String dateRange, @RequestParam("department_user") String department_user) throws IOException {
        byte[] excelData = excelGenerator.generateExcelForDateBetweenAndPlace(dateRange, department_user);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "move_with_date.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }



    /*******/

    @PostMapping("/findMovementsByDate")
    public  ResponseEntity<List<MovementsEntity>> findMovementsByDate(@RequestBody String date){
        try{
            List<MovementsEntity> movementsEntites = movementsService.findMovementsByDate(date);
            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();

        }
    }


    @PostMapping("/findMovementsByDateAndPlace")
    public  ResponseEntity<List<MovementsEntity>> findMovementsByDateAndPlace(@RequestBody ReqDateAndPlace req){
        try{
            List<MovementsEntity> movementsEntites = movementsService.findMovementsByDateAndPlace(req.getDate(), req.getPlace());
            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();

        }
    }

    @PostMapping("/findMovementsByDateAndPlaceAndNameUser")
    public  ResponseEntity<List<MovementsEntity>> findMovementsByDateAndPlaceAndNameUser(@RequestBody ReqDateAndPlaceAndUserName req){
        try{
            List<MovementsEntity> movementsEntites = movementsService.findMovementsByDateAndPlaceAndNameUser(req.getDate(), req.getPlace(), req.getNameUser());
            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }


    @PostMapping("/findMovementsByDateAndNameUser")
    public  ResponseEntity<List<MovementsEntity>> findMovementsByDateAndUserName(@RequestBody ReqDateAndNameUser req){
        try{
            List<MovementsEntity> movementsEntites = movementsService.findMovementsByDateAndUserName(req.getDate(), req.getNameUser());
            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();

        }
    }


    /*Отчеты UI*/
    //Индивидуальные
    //Конкретный пункт
    @PostMapping("/findMovementsByDateBetweenNameUserAndPlace")
    public  ResponseEntity<List<MovementsEntity>> findMovementsByDateBetweenNameUserAndPlace(@RequestBody ReqDateBetweenAndNameUserAndPlace req){
        try{
            List<MovementsEntity> movementsEntites = movementsService.findMovementsByDateBetweenAndUserNameAndPlace(req.getDateFrom(),
                    req.getDateTo(), req.getNameUser(), req.getPlace());

            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }

    //Все пункты
    @PostMapping("/findMovementsByDateBetweenNameUser")
    public  ResponseEntity<List<MovementsEntity>> findMovementsByDateBetweenNameUser(@RequestBody ReqDateBetweenAndNameUserAndPlace req){
        try{
            List<MovementsEntity> movementsEntites = movementsService.findMovementsByDateBetweenAndUserName(req.getDateFrom(),
                    req.getDateTo(), req.getNameUser());

            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }

    //Общие
    //Конкретный пункт
    @PostMapping("/findMovementsByDateBetweenPlace")
    public  ResponseEntity<List<MovementsEntity>> findMovementsByDateBetweenPlace(@RequestBody ReqDateBetweenAndNameUserAndPlace req){
        try{
            List<MovementsEntity> movementsEntites = movementsService.findMovementsByDateBetweenAndPlace(req.getDateFrom(),
                    req.getDateTo(), req.getPlace());

            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }
    @PostMapping("/findMovementsByDateBetween")
    public  ResponseEntity<List<MovementsEntity>> findMovementsByDateBetween(@RequestBody ReqDateBetweenAndNameUserAndPlace req){
        try{
            List<MovementsEntity> movementsEntites = movementsService.findMovementsByDateBetween(req.getDateFrom(),
                    req.getDateTo());

            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }









}
