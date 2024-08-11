package com.dg.ServerRebornFarmguard.service;

import com.dg.ServerRebornFarmguard.entity.MovementsEntity;
import com.dg.ServerRebornFarmguard.entity.MovementsObshEntity;
import com.dg.ServerRebornFarmguard.entity.UserEntity;
import com.dg.ServerRebornFarmguard.repository.MovementsObshRepo;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.dg.ServerRebornFarmguard.service.TelegramBotService.getBot;

@Service
public class MovementsObshService {

    @Autowired
    MovementsObshRepo movementsObshRepo;

    @Autowired
    PlaceService placeService;

    @Autowired
    UserService serviceUser;




    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @Transactional
    public String writeComingTime(Long id, String mac_board){

        UserEntity entityUser = serviceUser.findByUserId(id);
        MovementsObshEntity movementsEntity = new MovementsObshEntity();

        if(!itsPlaceNow(id)) {

            String name = entityUser.getName();
            String time = localTimeNow();
            String place = getPlace(mac_board);
            String department = entityUser.getDepartment();
            movementsEntity.setIdUser(id);
            movementsEntity.setNameUser(entityUser.getName());
            movementsEntity.setPositionUser(entityUser.getPosition());
            movementsEntity.setDepartmentUser(entityUser.getDepartment());
            movementsEntity.setComingTime(localTimeNow());
            movementsEntity.setDate(dateNow());
            movementsEntity.setPlace(place);
            movementsObshRepo.save(movementsEntity);

            bot_tracking_and_notification_of_employee_movements(name, time, place, "прибыл(а) на пункт");
            bot_notification_emoloyee(serviceUser.chatIdUserByName(name), time, place, "прибыли на пункт");
            bot_tracking_department_notification(name,time, place,"прибыл на пункт", department);

            return "good"+entityUser.getName();
        }else{
            System.out.println("Вы не покинули пункт! Покиньте пункт: "+", чтобы войти!");
            return "not_exit_place";
        }

    }

    @Transactional
    public String writeExitTime(Long id, String mac_board){
        MovementsObshEntity movementsEntity = movementsObshRepo.findByIdUserAndExitTimeIsNull(id);
        String place = getPlace(mac_board.trim());
        if(itsPlaceNow(id)) {
            if(itsPlaceMatch(id, place)){

                String name = movementsEntity.getNameUser();
                String time = localTimeNow();
                String department = movementsEntity.getDepartmentUser();
                    movementsEntity.setExitTime(localTimeNow());
                    String timeAtPlace = countingTheTimeAtPlace(movementsEntity.getComingTime(), movementsEntity.getExitTime());
                    movementsEntity.setTimeAtPlace(timeAtPlace);
                    movementsObshRepo.save(movementsEntity);

                bot_tracking_and_notification_of_employee_movements(name, time, place, "покинул(а) пункт");
                bot_notification_emoloyee(serviceUser.chatIdUserByName(name), time, place, "покинули пункт");
                bot_tracking_department_notification(name,time, place,"покинул пункт", department);
                    return "good";

            }else{
                System.out.println("Пункты не совпадают!");
//                return "not_match_place";
                return "not_enter_on_place";
            }
        }else{
            System.out.println("Вы не отметились на вход!");
            return "not_enter_on_place";
        }

    }

    public boolean itsPlaceNow(Long id){
        MovementsObshEntity movementsEntity = movementsObshRepo.findByIdUserAndExitTimeIsNull(id);
        if (movementsEntity != null) {
            return true;
        }else{
            return false;
        }
    }

    public boolean itsPlaceMatch(Long id, String place){
        MovementsObshEntity movementsEntity = movementsObshRepo.findByIdUserAndPlaceAndExitTimeIsNull(id, place);
        if(movementsEntity == null){
            System.out.println("Пункты не совпадают");
            return false;
        }else{
            return true;
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

    public List<MovementsObshEntity> nowOnPlace(String place){
        List<MovementsObshEntity> entities = movementsObshRepo.findByPlaceAndExitTimeIsNull(place);
        return entities;
    }


    public String getPlace(String macPlace){
        return  placeService.namePlace(macPlace.trim());
    }


    private void bot_tracking_department_notification(String name, String time, String place, String status, String department) {
        if(department.equals("Отдел энергетики")) {
            //Г.Терекиев
            getBot().execute(new SendMessage(1185103930, "Сотрудник " + name + " " + status + " " + place + " в " + time + "!"));
            //С.Нечаев
            getBot().execute(new SendMessage(569371989, "Сотрудник " + name + " " + status + " " + place + " в " + time + "!"));
        }

        if(department.equals("Отдел контейнеров")) {
            //И.Волобуев
            getBot().execute(new SendMessage(659508966, "Сотрудник " + name + " " + status + " " + place + " в " + time + "!"));
            //E.Вебер
            getBot().execute(new SendMessage(733630557, "Сотрудник " + name + " " + status + " " + place + " в " + time + "!"));
        }
        if(department.equals("Отдел склада")){
            //А.Терехов
            getBot().execute(new SendMessage(540916543, "Сотрудник " + name + " " + status + " " + place + " в " + time + "!"));
        }
        // ниже добавить для других отделов
    }

    private void bot_notification_emoloyee(Long chatId, String time, String place, String status){
        if(!chatId.equals(404) || !chatId.equals(null) || !chatId.equals(0)) {
            getBot().execute(new SendMessage(chatId, "Вы " + status + " " + place + " в " + time + "!"));
        }else {
            System.out.println("Нет регистрации в боте");
            getBot().execute(new SendMessage(667788774, "У сотрудника нет регистрации в боте!"));
        }
    }

    private void bot_send_dzh(String name){
        getBot().execute(new SendMessage(667788774, "Закрываю для: " + name));
    }

    private void bot_tracking_and_notification_of_employee_movements(String name, String time, String place, String status){
        List<Long> listTracking =serviceUser.findAllChatIdByTracking();
        for(int i = 0; i<listTracking.size();i++){
            getBot().execute(new SendMessage(listTracking.get(i), "Сотрудник "+name+" "+status+" "+place+" в "+time+"!" ));
        }

    }


    public List<MovementsObshEntity> findMovementsByDateBetweenAndUserNameAndPlace(String dateFrom, String dateTo, String userName, String place){
        List<MovementsObshEntity> entities = movementsObshRepo.findByDateBetweenAndPlaceAndNameUser(dateFrom, dateTo, place, userName);
        return entities;
    }
    //Все пункты
    public List<MovementsObshEntity> findMovementsByDateBetweenAndUserName(String dateFrom, String dateTo, String userName){
        List<MovementsObshEntity> entities = movementsObshRepo.findByDateBetweenAndNameUser(dateFrom, dateTo, userName);
        return entities;
    }

    //Общие перемещения
    //Конкретный пункт
    public List<MovementsObshEntity> findMovementsByDateBetweenAndPlace(String dateFrom, String dateTo, String place){
        List<MovementsObshEntity> entities = movementsObshRepo.findByDateBetweenAndPlace(dateFrom, dateTo, place);
        return entities;
    }
    //Все перемещения за диапазон
    public List<MovementsObshEntity> findMovementsByDateBetween(String dateFrom, String dateTo){
        List<MovementsObshEntity> entities = movementsObshRepo.findByDateBetween(dateFrom, dateTo);
        return entities;
    }




}
