package com.dg.ServerRebornFarmguard.repository;

import com.dg.ServerRebornFarmguard.entity.MovementsEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovementsRepo extends JpaRepository<MovementsEntity, Long> {

    List<MovementsEntity> findAll();

    List<MovementsEntity> findAllByIdUserAndDate(Long idUser,String date);
    List<MovementsEntity> findByExitTimeIsNull();
    MovementsEntity findByIdUserAndExitTimeIsNull(Long idUser);
    MovementsEntity findByIdUserAndPlaceAndExitTimeIsNull(Long idUser,String place);
    Optional<MovementsEntity> findTopByIdUserOrderByIdmovementsDesc(Long idUser);
    Optional<MovementsEntity> findTopByNameUserOrderByIdmovementsDesc(String nameUser);
    Optional<MovementsEntity> findTopByIdUserAndOpenCloseOrderByIdmovementsDesc(Long idUser,String openClose);
    List<MovementsEntity> findByDateAndPlaceAndExitTimeIsNull(String date, String place);
    List<MovementsEntity> findByPlaceAndExitTimeIsNull(String place);
    Optional<MovementsEntity> findByIdUserAndPlaceAndExitTimeIsNullAndComingTimeIsNotNull(Long idUser, String place);



    List<MovementsEntity> findByDateAndPlace(String date, String place);
    List<MovementsEntity> findByDateAndNameUser(String date, String nameUser);
    List<MovementsEntity> findByDateAndPlaceAndNameUser(String date, String place, String nameUser);
    List<MovementsEntity> findByDate(String date);

    /*Для UI индивидуальные отчеты*/
    // Где пункт, диапазон дат и имя
    List<MovementsEntity> findByDateBetweenAndPlaceAndNameUser(String dateFrom, String dateTo, String place, String nameUser);
    //Где все пункты и диапазон дат и имя
    List<MovementsEntity> findByDateBetweenAndNameUser(String dateFrom, String dateTo, String nameUser);


    /*Для UI общие отчеты*/
    //Где конкретный пункт, диапазон дат и все сотрудники
    List<MovementsEntity> findByDateBetweenAndPlace(String dateFrom, String dateTo, String place);
    //Все пункты, Все сотрудники
    List<MovementsEntity> findByDateBetween(String dateFrom, String dateTo);


    @Query(value = "SELECT t1.* " +
            "FROM movements t1 " +
            "JOIN (" +
            "  SELECT MAX(idmovements) AS max_id, id_user " +
            "  FROM movements " +
            "  WHERE date BETWEEN ?1 AND ?2 " +
            "  GROUP BY id_user " +
            ") t2 ON t1.id_user = t2.id_user AND t1.idmovements = t2.max_id " +
            "WHERE t1.exit_time IS NOT NULL AND t1.open_close <> 'close' AND t1.open_close <> 'other'",
            nativeQuery = true)
    List<MovementsEntity> findLastRecordsWithinDateRange(String startDate, String endDate);



    /** Новый метод для проверки на нули*/
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MovementsEntity m WHERE m.idUser = :idUser AND m.place = :place AND m.exitTime IS NULL AND m.comingTime IS NOT NULL")
    Optional<MovementsEntity> findByIdUserAndPlaceAndExitTimeIsNullAndComingTimeIsNotNullForUpdate(@Param("idUser") Long idUser, @Param("place") String place);

}