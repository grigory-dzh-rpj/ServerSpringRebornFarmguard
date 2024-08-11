package com.dg.ServerRebornFarmguard.controller;


import com.dg.ServerRebornFarmguard.entity.MovementsEntity;
import com.dg.ServerRebornFarmguard.entity.MovementsObshEntity;
import com.dg.ServerRebornFarmguard.exception.ExceptionHttp;
import com.dg.ServerRebornFarmguard.model.ReqDateBetweenAndNameUserAndPlace;
import com.dg.ServerRebornFarmguard.service.MovementsObshService;
import com.dg.ServerRebornFarmguard.service.MovementsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/move_obsh")
public class MovementsObshController {

    @Autowired
    MovementsObshService movementsOService;



    @PostMapping("/nowOnPlace")
    public  ResponseEntity<List<MovementsObshEntity>> nowOnPlace(@RequestBody String place){
        try{
            List<MovementsObshEntity> movementsEntites = movementsOService.nowOnPlace(place);
            return ResponseEntity.ok(movementsEntites);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();

        }
    }



    @PostMapping("/findMovementsByDateBetweenNameUserAndPlace")
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
