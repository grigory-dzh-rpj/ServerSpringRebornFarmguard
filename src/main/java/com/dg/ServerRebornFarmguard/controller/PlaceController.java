package com.dg.ServerRebornFarmguard.controller;

import com.dg.ServerRebornFarmguard.entity.PlaceEntity;
import com.dg.ServerRebornFarmguard.exception.ExceptionHttp;
import com.dg.ServerRebornFarmguard.service.PlaceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/place")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Place Controller", description = "API для управления создания/удаления/.. пунктов")
public class PlaceController {


    @Autowired
    private PlaceService placeService;


    @PostMapping("/all")
    @Operation(summary = "Получить имена всех пунктов пункты",
            description = "Возвращает список всех пунктов.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получен список мест",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Ошибка на сервере")
    })
    public ResponseEntity<List<String>> findAllPlace(){
        try{
            List<String> placeEntitiesName = placeService.placesName();
            return ResponseEntity.ok(placeEntitiesName);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();

        }
    }

//    @PostMapping("/status_place")
//
//    public ResponseEntity<String> statusPlace(){
//
//        return ResponseEntity.ok("good");
//    }


    @PostMapping("/mac")
    @Operation(summary = "Получить MAC-адрес по имени пункта",
            description = "Возвращает MAC-адрес для указанного имени пункта.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получен MAC-адрес"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос"),
            @ApiResponse(responseCode = "500", description = "Ошибка на сервере")
    })
    public ResponseEntity<String> macPlace(
            @RequestBody @Schema(description = "Имя места для поиска MAC-адреса", example = "Ферма 1") String name){
        String mac = placeService.findMacPlaceByNamePlace(name);
        return ResponseEntity.ok(mac);
    }

    @PostMapping("/create")
    @Operation(summary = "Создать пункт",
            description = "Создает новое место и возвращает информацию о нем.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Место успешно создано",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlaceEntity.class))),
            @ApiResponse(responseCode = "400", description = "Неверный формат данных"),
            @ApiResponse(responseCode = "500", description = "Ошибка на сервере")
    })
    public ResponseEntity<PlaceEntity> createPlace(@RequestBody @Schema(description = "Данные для создания нового места") PlaceEntity place) {
        PlaceEntity createdPlace = placeService.createPlace(place);
        return new ResponseEntity<>(createdPlace, HttpStatus.CREATED);
    }


    @PostMapping("/itsHub")
    @Operation(summary = "Проверить, является ли пункт хабом",
            description = "Хабы это такие пункты - где я не открываю и не закрываю смены(Энергентики), там мы следим только за перемещеинями" +
                    ", често говоря не помню использую я эндпоинт для такой проверки, мне кажется я уже давно переделал под внутренню логику" +
                    ", но возможно будет полезно в каких-то ситуацияъ .")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный ответ с информацией, является ли место хабом",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))),
            @ApiResponse(responseCode = "400", description = "Неверный формат данных"),
            @ApiResponse(responseCode = "500", description = "Ошибка на сервере")
    })
    public ResponseEntity<Boolean> itsHub(@RequestBody String placeName){
        Boolean itsHubFindByName = placeService.itsHubFindByName(placeName);
        return ResponseEntity.ok(itsHubFindByName);
    }








}
