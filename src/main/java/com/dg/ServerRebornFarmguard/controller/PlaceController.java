package com.dg.ServerRebornFarmguard.controller;

import com.dg.ServerRebornFarmguard.entity.PlaceEntity;
import com.dg.ServerRebornFarmguard.entity.UserEntity;
import com.dg.ServerRebornFarmguard.exception.ExceptionHttp;
import com.dg.ServerRebornFarmguard.service.PlaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/place")
@CrossOrigin(origins = "http://localhost:5173")
public class PlaceController {


    @Autowired
    private PlaceService placeService;


    @PostMapping("/all")
    public ResponseEntity<List<String>> findAllPlace(){
        try{
            List<String> placeEntitiesName = placeService.placesName();
            return ResponseEntity.ok(placeEntitiesName);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();

        }
    }

    @PostMapping("/status_place")
    public ResponseEntity<String> statusPlace(){

        return ResponseEntity.ok("good");
    }


    @PostMapping("/mac")
    public ResponseEntity<String> macPlace(@RequestBody String name){
        String mac = placeService.findMacPlaceByNamePlace(name);
        return ResponseEntity.ok(mac);
    }

    @PostMapping("/create")
    public ResponseEntity<PlaceEntity> createPlace(@RequestBody PlaceEntity place) {
        PlaceEntity createdPlace = placeService.createPlace(place);
        return new ResponseEntity<>(createdPlace, HttpStatus.CREATED);
    }


    @PostMapping("/itsHub")
    public ResponseEntity<Boolean> itsHub(@RequestBody String placeName){
        Boolean itsHubFindByName = placeService.itsHubFindByName(placeName);
        return ResponseEntity.ok(itsHubFindByName);
    }








}
