package com.dg.ServerRebornFarmguard.service;


import com.dg.ServerRebornFarmguard.entity.MovementsElcEntity;

import com.dg.ServerRebornFarmguard.entity.UserEntity;

import com.dg.ServerRebornFarmguard.repository.MovementsElcRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MovementsElcService {

    @Autowired
    MovementsElcRepo movementsElcRepo;

    @Autowired
    UserService serviceUser;




    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String logic(String macPrefixAndId){

        String[] split = macPrefixAndId.split("//");
        String idAndPrefix = split[0];
        String mac_board = split[1];

        String prefix = idAndPrefix.substring(0,4);
        Long idUser = Long.valueOf(idAndPrefix.substring(4));

        if(!itsInDatabase(idUser)){
            return "not_validation";
        }
        if(!itsAccess(idUser)){
            return "not_validation";
        }


        if(prefix.equals("2222")){
            return writeComingTime(idUser, mac_board);
        }else if(prefix.equals("3333")){
            return writeExitTime(idUser, mac_board);
        }else{
            return "not_validation";
        }

    }

    public boolean itsInDatabase(Long id){
        UserEntity entity = serviceUser.findByUserId(id);

        if(entity != null){
            return true;
        }else{
            return false;
        }
    }

    public boolean itsAccess(Long id){
        UserEntity entity = serviceUser.findByUserId(id);
        if(entity.getAccess().equals("elc") || entity.getAccess().equals("full") ){
            return true;
        }else{
            return false;
        }
    }

    public String writeComingTime(Long id, String mac_board){

        UserEntity entityUser = serviceUser.findByUserId(id);
        MovementsElcEntity movementsEntity = new MovementsElcEntity();

        if(!itsPlaceNow(id)) {
            movementsEntity.setIdUser(id);
            movementsEntity.setNameUser(entityUser.getName());
            movementsEntity.setPositionUser(entityUser.getPosition());
            movementsEntity.setDepartmentUser(entityUser.getDepartment());
            movementsEntity.setComingTime(localTimeNow());
            movementsEntity.setDate(dateNow());
            movementsEntity.setPlace("Общежитие");
            movementsElcRepo.save(movementsEntity);
            return "good"+entityUser.getName();
        }else{
            System.out.println("Вы не покинули пункт! Покиньте пункт: "+", чтобы войти!");
            return "not_exit_place";
        }

    }

    public String writeExitTime(Long id, String mac_board){
        MovementsElcEntity movementsEntity = movementsElcRepo.findByIdUserAndExitTimeIsNull(id);
        if(itsPlaceNow(id)) {
//            if(itsPlaceMatch(id, movementsEntity.getPlace())){

            movementsEntity.setExitTime(localTimeNow());
            String timeAtPlace = countingTheTimeAtPlace(movementsEntity.getComingTime(), movementsEntity.getExitTime());
            movementsEntity.setTimeAtPlace(timeAtPlace);
            movementsElcRepo.save(movementsEntity);

            return "good";

//            }else{
//                System.out.println("Пункты не совпадают!");
//                return "not_match_place";
//            }
        }else{
            System.out.println("Вы не отметились на вход!");
            return "not_enter_on_place";
        }

    }

    public boolean itsPlaceNow(Long id){
        MovementsElcEntity movementsEntity = movementsElcRepo.findByIdUserAndExitTimeIsNull(id);
        if (movementsEntity != null) {
            return true;
        }else{
            return false;
        }
    }


    public String localTimeNow(){
        LocalTime localTime = LocalTime.now();
        String formattedTime = localTime.format(TIME_FORMATTER);
        return  formattedTime;
    }

    public String dateNow(){
        LocalDate localDate = LocalDate.now();
        String formattedDate = localDate.format(DATE_FORMATTER);
        return formattedDate;
    }

    public  String countingTheTimeAtPlace(String comingTime, String exitTime){
        LocalTime coming = LocalTime.parse(comingTime, TIME_FORMATTER);
        LocalTime exit = LocalTime.parse(exitTime, TIME_FORMATTER);

        Duration duration = Duration.between(coming, exit);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%02d:%02d", hours, minutes);

    }

    public List<MovementsElcEntity> nowOnPlace(){
        List<MovementsElcEntity> entities = movementsElcRepo.findByDateAndPlaceAndExitTimeIsNull(dateNow(), "Общежитие");
        return entities;
    }




}
