package com.dg.ServerRebornFarmguard.controller;


import com.dg.ServerRebornFarmguard.entity.MovementsElcEntity;
import com.dg.ServerRebornFarmguard.entity.MovementsObshEntity;
import com.dg.ServerRebornFarmguard.exception.ExceptionHttp;
import com.dg.ServerRebornFarmguard.service.MovementsElcService;
import com.dg.ServerRebornFarmguard.service.MovementsObshService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/move_elc")
public class MovementsElcController {

    @Autowired
    MovementsElcService movementsElcService;

    //http://localhost:7778/move_obsh/logic
    @PostMapping("/logic")
    public ResponseEntity<String> movingObsh(@RequestBody String macIdPrefix) {
        try {
            return ResponseEntity.ok(movementsElcService.logic(macIdPrefix));
        }catch (Exception e){
            e.printStackTrace();
            return ExceptionHttp.MainExeption();
        }
    }


    @PostMapping("/nowOnPlaceElc")
    public  ResponseEntity<List<MovementsElcEntity>> nowOnPlace(){
        try{
            List<MovementsElcEntity> movementsEntites = movementsElcService.nowOnPlace();
            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }

}
