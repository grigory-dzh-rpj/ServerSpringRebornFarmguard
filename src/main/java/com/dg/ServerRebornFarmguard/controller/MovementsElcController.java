package com.dg.ServerRebornFarmguard.controller;


import com.dg.ServerRebornFarmguard.entity.MovementsElcEntity;
import com.dg.ServerRebornFarmguard.exception.ExceptionHttp;
import com.dg.ServerRebornFarmguard.service.MovementsElcService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/move_elc")
@Slf4j
@Tag(name = "НЕ ИСПОЛЬЗУЕТСЯ", description = "Создавалось как альтернатива хаба, если будет какая-то новая логика для определенных пунктов ")

public class MovementsElcController {

    @Autowired
    MovementsElcService movementsElcService;

    //http://localhost:7778/move_obsh/logic
    @PostMapping("/logic")
    public ResponseEntity<String> movingObsh(@RequestBody String macIdPrefix) {
        try {
            return ResponseEntity.ok(movementsElcService.logic(macIdPrefix));
        }catch (Exception e){
            log.error("Ошибка /logic:",e);
            return ExceptionHttp.MainExeption();
        }
    }


    @PostMapping("/nowOnPlaceElc")
    public  ResponseEntity<List<MovementsElcEntity>> nowOnPlace(){
        try{
            List<MovementsElcEntity> movementsEntites = movementsElcService.nowOnPlace();
            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            log.error("Ошибка /nowOnPlaceElc",e);
            return ExceptionHttp.MainExeption();
        }
    }

}
