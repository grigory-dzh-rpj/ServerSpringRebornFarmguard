package com.dg.ServerRebornFarmguard.repository;

import com.dg.ServerRebornFarmguard.entity.MovementsEntity;
import com.dg.ServerRebornFarmguard.entity.MovementsObshEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovementsObshRepo extends JpaRepository<MovementsObshEntity, Long> {

    List<MovementsObshEntity> findAll();

    MovementsObshEntity findByIdUserAndExitTimeIsNull(Long idUser);

    List<MovementsObshEntity> findByDateAndPlaceAndExitTimeIsNull(String date, String place);
    List<MovementsObshEntity> findByPlaceAndExitTimeIsNull(String place);


    MovementsObshEntity findByIdUserAndPlaceAndExitTimeIsNull(Long idUser, String place);
    /*Для UI индивидуальные отчеты*/
    // Где пункт, диапазон дат и имя
    List<MovementsObshEntity> findByDateBetweenAndPlaceAndNameUser(String dateFrom, String dateTo, String place, String nameUser);
    //Где все пункты и диапазон дат и имя
    List<MovementsObshEntity> findByDateBetweenAndNameUser(String dateFrom, String dateTo, String nameUser);


    /*Для UI общие отчеты*/
    //Где конкретный пункт, диапазон дат и все сотрудники
    List<MovementsObshEntity> findByDateBetweenAndPlace(String dateFrom, String dateTo, String place);
    //Все пункты, Все сотрудники
    List<MovementsObshEntity> findByDateBetween(String dateFrom, String dateTo);

}