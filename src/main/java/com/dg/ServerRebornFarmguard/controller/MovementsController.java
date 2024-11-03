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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/move")
@Slf4j
@Tag(name = "Movement Controller", description = "API для управления перемещениями сотрудников")
public class MovementsController {

    @Autowired
    MovementsService movementsService;


    @PostMapping("/logic")
    @Operation(summary = "Приход/Уход",
            description = "Записывает перемещение на основе префикса(2222 приход/3333 уход) и MAC-адреса контрольки на пункте")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная обработка перемещения"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<String> moving(@RequestBody @Schema(description = "Пример 2222{idUser}//macAdr") String macIdPrefix) {
        try {
            return ResponseEntity.ok(movementsService.logic(macIdPrefix));
        }catch (Exception e){
            log.error("Ошибка в /logic", e);
            return ExceptionHttp.MainExeption();
        }
    }




    @PostMapping("/close")
    @Operation(summary = "Закрыть смену по ID",
            description = "Закрывает перемещение по указанному идентификатору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Перемещение успешно закрыто"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<String> closing(@RequestBody Long id){
        try {
          String s =  movementsService.closeStatus(id);
            return ResponseEntity.ok(s);
        }catch (Exception e){

            return ExceptionHttp.MainExeption();
        }
    }


    @PostMapping("/closeByName")
    @Operation(summary = "Закрыть смену по имени",
            description = "Закрывает перемещение по имени пользователя, в таблице устанавливается" +
                    "флаг close и подсчитывается время общее время за смену" )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Перемещение успешно закрыто"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<String> closingByName(@RequestBody String name){
        try {
            String s =  movementsService.closeStatusByName(name);
            return ResponseEntity.ok(s);
        }catch (Exception e){
            log.error("Ошибка в /closeByName()", e);
            return ExceptionHttp.MainExeption();
        }
    }

    @PostMapping("/nowOnPlace")
    @Operation(summary = "Получить список сотрудников на конкретном пункте",
            description = "Передаем пункт, получаем список сотрудников на пункте.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получен список перемещений",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MovementsEntity.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос"),
            @ApiResponse(responseCode = "500", description = "Ошибка на сервере")
    })
    public  ResponseEntity<List<MovementsEntity>> nowOnPlace(@RequestBody String place){
        try{
            List<MovementsEntity> movementsEntities = movementsService.nowOnPlace(place);
            return ResponseEntity.ok(movementsEntities);
        }catch (Exception e){
            log.error("Ошибка в nowOnPlace", e);
            return ExceptionHttp.MainExeption();

        }
    }




    @GetMapping("/startBot")
    public ResponseEntity<String> startBot(){
        TelegramBotService telegramBotService = new TelegramBotService();
        //<>
        return ResponseEntity.ok("Бот запущен");
    }



    @Autowired
    private MainReports excelGenerator;

    @GetMapping("/export")
    @Operation(summary = "Экспорт всех перемещений",
            description = "Экспортирует все перемещения в Excel файл/ грубо говоря дамп базы")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Файл успешно сгенерирован"),
            @ApiResponse(responseCode = "500", description = "Ошибка при генерации файла")
    })
    public ResponseEntity<byte[]> exportMovements() throws IOException {
        byte[] excelData = excelGenerator.generateExcel();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "move2.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    @GetMapping("/exportWithDateRange")
    @Operation(summary = "Экспорт перемещений за период",
            description = "Экспортирует перемещения за указанный период в Excel файл")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Файл успешно сгенерирован"),
            @ApiResponse(responseCode = "500", description = "Ошибка при генерации файла")
    })
    public ResponseEntity<byte[]> exportMovementsWithDateRange(@RequestParam("dateRange")
                                                                   @Schema(description = "Диапазон дат в формате yyyy-MM-dd/yyyy-MM-dd")
                                                                           String dateRange) throws IOException {
        byte[] excelData = excelGenerator.generateExcelForDateBetween(dateRange);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "move_with_date.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    @GetMapping("/exportWithDateRangeAndUserName")
    @Operation(summary = "Экспорт перемещений по периоду и пользователю",
            description = "Экспортирует в Excel файл все перемещения указанного пользователя за заданный период времени")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Excel файл успешно сгенерирован",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "400", description = "Неверный формат параметров"),
            @ApiResponse(responseCode = "500", description = "Ошибка при генерации файла")
    })
    public ResponseEntity<byte[]> exportMovementsWithDateRangeAndUserName(@RequestParam("dateRange")
                                                                              @Schema(description = "Диапазон дат в формате yyyy-MM-dd/yyyy-MM-dd",
                                                                                      example = "2024-01-01/2024-12-31") String dateRange,
                                                                          @RequestParam("userName") @Schema(description = "Имя пользователя, чьи перемещения нужно экспортировать",
                                                                                  example = "Иван Иванов") String userName) throws IOException {
        byte[] excelData = excelGenerator.generateExcelForDateBetweenAndUserName(dateRange, userName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "move_"+userName+"of"+dateRange+".xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    @GetMapping("/exportWithDateRangeAndPlace")
    @Operation(summary = "Экспорт перемещений по периоду и месту",
            description = "Экспортирует в Excel файл все перемещения для указанного места за заданный период времени")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Excel файл успешно сгенерирован",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "400", description = "Неверный формат параметров"),
            @ApiResponse(responseCode = "500", description = "Ошибка при генерации файла")
    })
    public ResponseEntity<byte[]> exportMovementsWithDateRangeAndPlace(
            @RequestParam("dateRange")@Schema(description = "Диапазон дат в формате yyyy-MM-dd/yyyy-MM-dd",
                    example = "2024-05-31/2024-05-31")  String dateRange,
            @RequestParam("place") @Schema(description = "Место, для которого нужно экспортировать перемещения",
                    example = "Ферма 1") String place) throws IOException {
        byte[] excelData = excelGenerator.generateExcelForDateBetweenAndPlace(dateRange, place);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "move_with_date.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    @GetMapping("/exportWithDateRangeAndDepartment")
    @Operation(summary = "Экспорт перемещений по периоду и отделу",
            description = "Экспортирует в Excel файл все перемещения для указанного отдела за заданный период времени.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Excel файл успешно сгенерирован",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "400", description = "Неверный формат параметров"),
            @ApiResponse(responseCode = "500", description = "Ошибка при генерации файла")
    })
    public ResponseEntity<byte[]> exportMovementsWithDateRangeAndDepartment(
            @RequestParam("dateRange") @Schema(description = "Диапазон дат в формате yyyy-MM-dd/yyyy-MM-dd",
                    example = "2024-05-31/2024-06-30") String dateRange,
            @RequestParam("department_user") @Schema(description = "Отдел, для которого нужно экспортировать перемещения",
                    example = "Отдел ремонта") String department_user) throws IOException {
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
    @Operation(summary = "Поиск перемещений по дате",
            description = "Возвращает список перемещений, связанных с указанной датой.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно найден список перемещений",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MovementsEntity.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос"),
            @ApiResponse(responseCode = "500", description = "Ошибка на сервере")
    })
    public  ResponseEntity<List<MovementsEntity>> findMovementsByDate(
            @RequestBody @Schema(description = "Дата для поиска перемещений в формате yyyy-MM-dd",
                    example = "2024-05-31")  String date){
        try{
            List<MovementsEntity> movementsEntites = movementsService.findMovementsByDate(date);
            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();

        }
    }



    @PostMapping("/findMovementsByDateAndPlace")
    @Operation(summary = "Поиск перемещений по дате и месту",
            description = "Возвращает список перемещений за указанную дату в конкретном месте")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список успешно получен",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = MovementsEntity.class)))),
            @ApiResponse(responseCode = "500", description = "Ошибка при поиске")
    })
    public  ResponseEntity<List<MovementsEntity>> findMovementsByDateAndPlace(@RequestBody ReqDateAndPlace req){
        try{
            List<MovementsEntity> movementsEntites = movementsService.findMovementsByDateAndPlace(req.getDate(), req.getPlace());
            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();

        }
    }

    @PostMapping("/findMovementsByDateAndPlaceAndNameUser")
    @Operation(summary = "Поиск перемещений по дате, месту и имени пользователя",
            description = "Возвращает список перемещений, связанных с указанной датой, местом и именем пользователя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно найден список перемещений",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MovementsEntity.class))),
            @ApiResponse(responseCode = "400", description = "Неверный формат запроса"),
            @ApiResponse(responseCode = "500", description = "Ошибка на сервере")
    })
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
    @Operation(summary = "Поиск перемещений по периоду, пользователю и месту",
            description = "Возвращает список перемещений за указанный период для конкретного пользователя в определенном месте")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список успешно получен",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = MovementsEntity.class)))),
            @ApiResponse(responseCode = "500", description = "Ошибка при поиске")
    })
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
    @Operation(summary = "Поиск перемещений по диапазону дат, имени пользователя и месту",
            description = "Возвращает список перемещений для указанного пользователя в заданный период времени.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно найден список перемещений",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MovementsEntity.class))),
            @ApiResponse(responseCode = "400", description = "Неверный формат запроса"),
            @ApiResponse(responseCode = "500", description = "Ошибка на сервере")
    })
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
    @Operation(summary = "Поиск перемещений по диапазону дат",
            description = "Возвращает список перемещений для указанного диапазона дат.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно найден список перемещений",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MovementsEntity.class))),
            @ApiResponse(responseCode = "400", description = "Неверный формат запроса"),
            @ApiResponse(responseCode = "500", description = "Ошибка на сервере")
    })
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
