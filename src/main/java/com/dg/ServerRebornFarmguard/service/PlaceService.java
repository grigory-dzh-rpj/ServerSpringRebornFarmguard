package com.dg.ServerRebornFarmguard.service;

import com.dg.ServerRebornFarmguard.entity.PlaceEntity;
import com.dg.ServerRebornFarmguard.repository.PlaceRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlaceService {

    @Autowired
    PlaceRepo placeRepo;

    public String namePlace(String macPlace){
        PlaceEntity placeEntity = placeRepo.findNamePlaceByMacPlace(macPlace);
            return placeEntity.getNamePlace();
    }

    public List<PlaceEntity> returnAllPlaceEntity(){
        List<PlaceEntity> list = placeRepo.findAll();
        return list;
    }


    public List<String> placesName(){
       List<String> list = new ArrayList<>();

     for(PlaceEntity placeEntity : placeRepo.findAll()){
         list.add(placeEntity.getNamePlace());
     }
     return list;

    }

    public String findMacPlaceByNamePlace(String name){
        PlaceEntity placeEntity = placeRepo.findMacPlaceByNamePlace(name);
        if(placeEntity != null){
            return placeEntity.getMacPlace();
        }else{
            return "0";
        }
    }

    public PlaceEntity createPlace(PlaceEntity place) {
        return placeRepo.save(place);
    }


    public List<PlaceEntity> hubZone(String itsHub){
        return placeRepo.findAllByItsHub("yes");
    }

    public boolean itsHub(String macPlace){
        String y = "yes";
        PlaceEntity place = placeRepo.findByMacPlaceAndItsHub(macPlace, y);

        return place != null;
    }

    public boolean itsHubFindByName(String namePlace){

        String y = "yes";
        PlaceEntity place = placeRepo.findByNamePlaceAndItsHub(namePlace,y);
        return place != null;
    }

}
