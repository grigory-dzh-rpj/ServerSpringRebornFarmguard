package com.dg.ServerRebornFarmguard.controller;


import com.dg.ServerRebornFarmguard.entity.MovementsEntity;
import com.dg.ServerRebornFarmguard.entity.MovementsObshEntity;
import com.dg.ServerRebornFarmguard.exception.ExceptionHttp;
import com.dg.ServerRebornFarmguard.model.ReqDateBetweenAndNameUserAndPlace;
import com.dg.ServerRebornFarmguard.service.MovementsObshService;
import com.dg.ServerRebornFarmguard.service.MovementsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/move_obsh")
@Tag(name = "MovementsObshController - это контроллер для хабов", description = "API для пунктов типа HUB")
public class MovementsObshController {

    @Autowired
    MovementsObshService movementsOService;



    @PostMapping("/nowOnPlace")
    @Operation(summary = "Получить перемещения на указанном хабе",
            description = "Возвращает список всех перемещений, связанных с указанным хабом.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получен список перемещений",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MovementsObshEntity.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос"),
            @ApiResponse(responseCode = "500", description = "Ошибка на сервере")
    })
    public  ResponseEntity<List<MovementsObshEntity>> nowOnPlace(@RequestBody String place){
        try{
            List<MovementsObshEntity> movementsEntites = movementsOService.nowOnPlace(place);
            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();

        }
    }



    @PostMapping("/findMovementsByDateBetweenNameUserAndPlace")
    @Operation(summary = "Найти перемещения по дате, имени пользователя и Хабу",
            description = "Возвращает список перемещений в указанном диапазоне дат, по указанному имени пользователя и месту.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получен список перемещений",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MovementsObshEntity.class))),
            @ApiResponse(responseCode = "400", description = "Неверный формат параметров"),
            @ApiResponse(responseCode = "500", description = "Ошибка на сервере")
    })
    public  ResponseEntity<List<MovementsObshEntity>> findMovementsByDateBetweenNameUserAndPlace(@RequestBody ReqDateBetweenAndNameUserAndPlace req){
        try{
            List<MovementsObshEntity> movementsEntites = movementsOService.findMovementsByDateBetweenAndUserNameAndPlace(req.getDateFrom(),
                    req.getDateTo(), req.getNameUser(), req.getPlace());

            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }

    //Все пункты
    @PostMapping("/findMovementsByDateBetweenNameUser")
    @Operation(summary = "Найти перемещения по дате и имени пользователя",
            description = "Возвращает список перемещений за указанный диапазон дат и имя пользователя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получен список перемещений",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MovementsObshEntity.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос"),
            @ApiResponse(responseCode = "500", description = "Ошибка на сервере")
    })
    public  ResponseEntity<List<MovementsObshEntity>> findMovementsByDateBetweenNameUser(@RequestBody ReqDateBetweenAndNameUserAndPlace req){
        try{
            List<MovementsObshEntity> movementsEntites = movementsOService.findMovementsByDateBetweenAndUserName(req.getDateFrom(),
                    req.getDateTo(), req.getNameUser());

            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }

    //Общие
    //Конкретный пункт
    @PostMapping("/findMovementsByDateBetweenPlace")
    @Operation(summary = "Найти перемещения по дате и месту",
            description = "Возвращает список перемещений за указанный диапазон дат и место.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получен список перемещений",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MovementsObshEntity.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос"),
    })
            public  ResponseEntity<List<MovementsObshEntity>> findMovementsByDateBetweenPlace(@RequestBody ReqDateBetweenAndNameUserAndPlace req){
        try{
            List<MovementsObshEntity> movementsEntites = movementsOService.findMovementsByDateBetweenAndPlace(req.getDateFrom(),
                    req.getDateTo(), req.getPlace());

            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }
    @PostMapping("/findMovementsByDateBetween")
    @Operation(summary = "Найти перемещения по диапазону дат",
            description = "Возвращает список перемещений за указанный диапазон дат.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получен список перемещений",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MovementsObshEntity.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос"),
            @ApiResponse(responseCode = "500", description = "Ошибка на сервере")
    })
    public  ResponseEntity<List<MovementsObshEntity>> findMovementsByDateBetween(@RequestBody ReqDateBetweenAndNameUserAndPlace req){
        try{
            List<MovementsObshEntity> movementsEntites = movementsOService.findMovementsByDateBetween(req.getDateFrom(),
                    req.getDateTo());

            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }




}
