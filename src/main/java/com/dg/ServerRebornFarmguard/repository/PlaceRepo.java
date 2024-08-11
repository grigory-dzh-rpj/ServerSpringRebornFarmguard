package com.dg.ServerRebornFarmguard.repository;

import com.dg.ServerRebornFarmguard.entity.PlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceRepo extends JpaRepository<PlaceEntity, Long> {


    List<PlaceEntity> findAll();
    PlaceEntity findNamePlaceByMacPlace(String macPlace);
    PlaceEntity findNamePlaceByIdplace(Long id);
    PlaceEntity findMacPlaceByNamePlace(String namePlace);
    List<PlaceEntity> findAllByItsHub(String itsHub);
    PlaceEntity findByMacPlaceAndItsHub(String macPlace, String itsHub);
    PlaceEntity findByNamePlaceAndItsHub(String namePlace, String itsHub);


}