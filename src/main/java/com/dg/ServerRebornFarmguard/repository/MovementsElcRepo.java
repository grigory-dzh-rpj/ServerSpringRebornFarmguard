package com.dg.ServerRebornFarmguard.repository;

import com.dg.ServerRebornFarmguard.entity.MovementsElcEntity;
import com.dg.ServerRebornFarmguard.entity.MovementsObshEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovementsElcRepo extends JpaRepository<MovementsElcEntity, Long> {

    List<MovementsElcEntity> findAll();

    MovementsElcEntity findByIdUserAndExitTimeIsNull(Long idUser);

    List<MovementsElcEntity> findByDateAndPlaceAndExitTimeIsNull(String date, String place);
}