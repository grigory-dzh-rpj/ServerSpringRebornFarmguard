package com.dg.ServerRebornFarmguard.service;

import com.dg.ServerRebornFarmguard.entity.MovementsEntity;
import com.dg.ServerRebornFarmguard.entity.UserEntity;
import com.dg.ServerRebornFarmguard.repository.MovementsRepo;
import com.dg.ServerRebornFarmguard.service.reports.excel.CreateDiagrams;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dg.ServerRebornFarmguard.service.TelegramBotService.bot;
import static com.dg.ServerRebornFarmguard.service.TelegramBotService.getBot;

@Service
@EnableScheduling
public class MovementsService {

    @Autowired
    MovementsRepo movementsRepo;

    @Autowired
    UserService serviceUser;

    @Autowired
    PlaceService placeService;

    @Autowired
    CreateDiagrams createDiagrams;

    @Autowired
    MovementsObshService movementsObshService;





    public  static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");



    @Transactional
    public String logic(String macPrefixAndId){
        String[] split = macPrefixAndId.split("//");
        String idAndPrefix = split[0];
        String mac_board = split[1];
        System.out.println(mac_board);
        String prefix = idAndPrefix.substring(0, 4);
        Long idUser = Long.valueOf(idAndPrefix.substring(4));

        if(!flag.get()) {
            System.out.println("Логика работает");

            if (!itsInDatabase(idUser)) {
                return "not_validation";
            }



            //Если хаб
            if(placeService.itsHub(mac_board.trim())){

                if(prefix.equals("2222")) {
                    return movementsObshService.writeComingTime(idUser, mac_board.trim());
                }else if (prefix.equals("3333")){
                    return movementsObshService.writeExitTime(idUser, mac_board.trim());
                }else{
                    return "not_validation";
                }
            }

            //Основная логика
            if (prefix.equals("2222")) {
                Optional<MovementsEntity> existingEntry = movementsRepo.findByIdUserAndPlaceAndExitTimeIsNullAndComingTimeIsNotNull(idUser, getPlace(mac_board));

                if (existingEntry.isPresent()) {

                    return "not_exit_place";
                }
                return writeComingTime(idUser, mac_board);
            } else if (prefix.equals("3333")) {
                return writeExitTime(idUser, mac_board);
            } else {
                return "not_validation";
            }

        }else{
            UserEntity user = serviceUser.findByUserId(idUser);
            if(user == null){
                return "not_validation";
            }
            if(user.getChatId() != null) {
                String message = "Подождите, идёт переотрытие смены!";
                String message2 = "Приложите карту повторно через минуту!";
                bot_send_message_by_chat_id(user.getChatId(), message);
                bot_send_message_by_chat_id(user.getChatId(), message2);
            }else{
                System.out.println("Чат id NULL");
            }
            System.out.println("Подождите идет переоткрытие !");
            return "not_validation";
        }

    }


    //Приход

    public String writeComingTime(Long id, String mac_board) {

        try {
            UserEntity entityUser = serviceUser.findByUserId(id);
            MovementsEntity movementsEntity = new MovementsEntity();

            if (!itsPlaceNow(id)) {

                movementsEntity.setIdUser(id);
                String name = entityUser.getName();
                String time = localTimeNow();
                String place = getPlace(mac_board);
                String department = entityUser.getDepartment();
                movementsEntity.setNameUser(name);
                movementsEntity.setPositionUser(entityUser.getPosition());
                movementsEntity.setDepartmentUser(department);
                movementsEntity.setComingTime(time);
                movementsEntity.setDate(dateNow());
                movementsEntity.setPlace(place);
                movementsEntity.setOpenClose(open(id));
                movementsRepo.save(movementsEntity);

                bot_tracking_and_notification_of_employee_movements(name, time, place, "прибыл(а) на пункт");
                bot_notification_emoloyee(serviceUser.chatIdUserByName(name), time, place, "прибыли на пункт");
                bot_tracking_department_notification(name, time, place, "прибыл на пункт", department);

                return "good" + entityUser.getName();
            } else {
                System.out.println("Вы не покинули пункт! Покиньте пункт: " + ", чтобы войти!");
                return "not_exit_place";
            }

        } catch (DataIntegrityViolationException e) {
            // Обработка исключения при нарушении уникального ограничения
//        logger.warn("Попытка дублирования записи для пользователя {} в месте {}", idUser, macBoard);
            return "not_validation";

        }
    }


    //Уход
//    @Transactional
    public String writeExitTime(Long id, String mac_board){
        MovementsEntity movementsEntity = movementsRepo.findByIdUserAndExitTimeIsNull(id);
        String place = getPlace(mac_board.trim());
        if(itsPlaceNow(id)) {
                if(itsPlaceMatch(id, place)){
                        String name = movementsEntity.getNameUser();
                        String time = localTimeNow();
                        String department = movementsEntity.getDepartmentUser();
                        movementsEntity.setExitTime(time);
                        String timeAtPlace = countingTheTimeAtPlace(movementsEntity.getComingTime(), movementsEntity.getExitTime());
                        movementsEntity.setTimeAtPlace(timeAtPlace);
                        movementsRepo.save(movementsEntity);
                        bot_tracking_and_notification_of_employee_movements(name, time, place, "покинул(а) пункт");
                    bot_notification_emoloyee(serviceUser.chatIdUserByName(name), time, place, "покинули пункт");
                    bot_tracking_department_notification(name,time, place,"покинул пункт", department);
                        if(itsHaveClose(movementsEntity)){
                           closeWriteTime(id);
                        }
                        return "good";
                }else{
                    System.out.println("Пункты не совпадают!");
//                    return "not_match_place";
                    return "not_enter_on_place";
                }
        }else{
            System.out.println("Вы не отметились на вход!");
            return "not_enter_on_place";
        }

    }




    public void writeComingAndExitTimeAfterMidnight2(Long id, String place) {

        UserEntity entityUser = serviceUser.findByUserId(id);
        MovementsEntity movementsEntity = new MovementsEntity();

        movementsEntity.setIdUser(id);
        movementsEntity.setNameUser(entityUser.getName());
        movementsEntity.setPositionUser(entityUser.getPosition());
        movementsEntity.setDepartmentUser(entityUser.getDepartment());
        movementsEntity.setComingTime("00:00");
        movementsEntity.setDate(dateNow());
        movementsEntity.setPlace(place);
        movementsEntity.setOpenClose("open");

        movementsRepo.save(movementsEntity);

    }


    //Проверка есть ли сейчас на пункте
    public boolean itsPlaceNow(Long id){
        MovementsEntity movementsEntity = movementsRepo.findByIdUserAndExitTimeIsNull(id);
        if (movementsEntity != null) {
            return true;
        }else{
           return false;
        }
    }

    //Пункты совпадают?
    public boolean itsPlaceMatch(Long id, String place){
        MovementsEntity movementsEntity = movementsRepo.findByIdUserAndPlaceAndExitTimeIsNull(id, place);
        if(movementsEntity == null){
            System.out.println("Пункты не совпадают");
            return false;
        }else{
            return true;
        }
    }

    //Есть ли в базе данных?
    public boolean itsInDatabase(Long id){
        UserEntity entity = serviceUser.findByUserId(id);

        if(entity != null){
            return true;
        }else{
            return false;
        }
    }

    //Закрыта ли смена?


    public boolean itsHaveClose(MovementsEntity entity){
        if(entity == null){
            return false;
        }

        if(entity.getOpenClose().equals("close")){
            return true;
        }else if(entity.getOpenClose().equals("openclose")){
            return true;
        } else{
           return false;
        }

    }

    //Дата совпадает
    public boolean itsDateMatch(String date){
        if(date.equals(dateNow())){
            return true;
        }else {
            return false;
        }
    }



    //Время сейчас
    public String localTimeNow(){
        LocalTime localTime = LocalTime.now();
        String formattedTime = localTime.format(TIME_FORMATTER);
        return  formattedTime;
    }
    //Дата сейчас
    public String dateNow(){
        LocalDate localDate = LocalDate.now();
        String formattedDate = localDate.format(DATE_FORMATTER);
        return formattedDate;
    }

    //Пункт по мак-адресу
    public String getPlace(String macPlace){

        return  placeService.namePlace(macPlace.trim());
    }


    //Время проведенное на пункте
    public  String countingTheTimeAtPlace(String comingTime, String exitTime){
        LocalTime coming = LocalTime.parse(comingTime, TIME_FORMATTER);
        LocalTime exit = LocalTime.parse(exitTime, TIME_FORMATTER);

        Duration duration = Duration.between(coming, exit);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%02d:%02d", hours, minutes);

    }


    public void closeHelpWriteTime(Long id, String status, MovementsEntity lastMovement){
        Optional<MovementsEntity> openMovementOptional = movementsRepo.findTopByIdUserAndOpenCloseOrderByIdmovementsDesc(id, status);

        if (openMovementOptional.isPresent()) {
            MovementsEntity openMovement = openMovementOptional.get();
            LocalDate openDate = LocalDate.parse(openMovement.getDate(),DATE_FORMATTER);
            LocalDate closeDate = LocalDate.parse(lastMovement.getDate(), DATE_FORMATTER);

            if(openDate.isEqual(closeDate)) {
                LocalTime comingTime = LocalTime.parse(openMovement.getComingTime(), DateTimeFormatter.ofPattern("HH:mm"));
                LocalTime now = LocalTime.now();

                Duration duration = Duration.between(comingTime, now);
                long hours = duration.toHours();
                long minutes = duration.toMinutesPart();
                String openCloseTime = String.format("%02d:%02d", hours, minutes);

                lastMovement.setOpenCloseTime(openCloseTime);
                lastMovement.setOpenClose("close");
                movementsRepo.save(lastMovement);
            }else{
                handleDifferentDates(openMovement, lastMovement);
            }
        }
    }


    //Расчет общего времени за смену
    public void closeWriteTime(Long id) {
        Optional<MovementsEntity> lastMovementOptional = movementsRepo.findTopByIdUserOrderByIdmovementsDesc(id);

        if (lastMovementOptional.isPresent()) {
            MovementsEntity lastMovement = lastMovementOptional.get();
            if(lastMovement.getOpenClose().equals("openclose")){
                closeHelpWriteTime(id, "openclose", lastMovement);
            }else {
                closeHelpWriteTime(id, "open", lastMovement);
            }
        }

    }



    //Меняем статус на закрытие смены
    public String closeStatus(Long id){
        Optional<MovementsEntity> lastMovementOptional = movementsRepo.findTopByIdUserOrderByIdmovementsDesc(id);
        if (lastMovementOptional.isPresent()) {
            if(lastMovementOptional.get().getOpenClose().equals("open")){
                MovementsEntity lastMovement = lastMovementOptional.get();
                lastMovement.setOpenClose("openclose");
                movementsRepo.save(lastMovement);
                return "close good: "+ lastMovement.getNameUser();

            }else if(!lastMovementOptional.get().getOpenClose().equals("close")) {
                MovementsEntity lastMovement = lastMovementOptional.get();
                lastMovement.setOpenClose("close");
                movementsRepo.save(lastMovement);
                return "close good: " + lastMovement.getNameUser();
            }else{
                return "смена уже была закрыта!";
            }
        }else {
            return "close bad";
        }
    }

    public String closeStatusByName(String name){
        Optional<MovementsEntity> lastMovementOptional = movementsRepo.findTopByNameUserOrderByIdmovementsDesc(name);
        if (lastMovementOptional.isPresent()) {
            if(!lastMovementOptional.get().getOpenClose().equals("close")) {
                MovementsEntity lastMovement = lastMovementOptional.get();
                lastMovement.setOpenClose("close");
                movementsRepo.save(lastMovement);
                return "good";
            }else{
                return "tobe";
            }
        }else {
            return "bad";
        }
    }


    public void closeWriteTimeForAuto(Long id, LocalTime timeLastExit) {
        Optional<MovementsEntity> lastMovementOptional = movementsRepo.findTopByIdUserOrderByIdmovementsDesc(id);

        if (lastMovementOptional.isPresent()) {
            MovementsEntity lastMovement = lastMovementOptional.get();
            Optional<MovementsEntity> openMovementOptional = movementsRepo.findTopByIdUserAndOpenCloseOrderByIdmovementsDesc(id, "open");

            if (openMovementOptional.isPresent()) {
                MovementsEntity openMovement = openMovementOptional.get();
                LocalDate openDate = LocalDate.parse(openMovement.getDate(),DATE_FORMATTER);
                LocalDate closeDate = LocalDate.parse(lastMovement.getDate(), DATE_FORMATTER);


                    LocalTime comingTime = LocalTime.parse(openMovement.getComingTime(), DateTimeFormatter.ofPattern("HH:mm"));
                    LocalTime now = LocalTime.now();

                    Duration duration = Duration.between(comingTime, timeLastExit).abs();
                    long hours = duration.toHours();
                    long minutes = duration.toMinutesPart();
                    String openCloseTime = String.format("%02d:%02d", hours, minutes);

                    lastMovement.setOpenCloseTime(openCloseTime);
                    movementsRepo.save(lastMovement);

            }
        }

    }

    //Если даты отличаются
    private void handleDifferentDates(MovementsEntity openMovement, MovementsEntity lastMovement) {
        LocalDate closeDate = LocalDate.parse(lastMovement.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        LocalTime comingTime = LocalTime.parse(openMovement.getComingTime(), DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime endOfDay = LocalTime.of(23, 59, 59);

        Duration firstPart = Duration.between(comingTime, endOfDay);
        long firstPartHours = firstPart.toHours();
        long firstPartMinutes = firstPart.toMinutesPart();
        String firstPartOpenCloseTime = String.format("%02d:%02d", firstPartHours, firstPartMinutes);

        lastMovement.setOpenCloseTime(firstPartOpenCloseTime);

        movementsRepo.save(lastMovement);

        MovementsEntity nextDayMovement = new MovementsEntity();
        nextDayMovement.setIdUser(lastMovement.getIdUser());
        nextDayMovement.setNameUser(lastMovement.getNameUser());
        nextDayMovement.setPositionUser(lastMovement.getPositionUser());
        nextDayMovement.setDepartmentUser(lastMovement.getDepartmentUser());
        nextDayMovement.setComingTime("00:00");
        nextDayMovement.setDate(closeDate.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        nextDayMovement.setOpenClose("open");

        LocalTime now = LocalTime.now();
        Duration secondPart = Duration.between(LocalTime.MIDNIGHT, now);
        long secondPartHours = secondPart.toHours();
        long secondPartMinutes = secondPart.toMinutesPart();
        String secondPartOpenCloseTime = String.format("%02d:%02d", secondPartHours, secondPartMinutes);

        nextDayMovement.setOpenCloseTime(secondPartOpenCloseTime);
        movementsRepo.save(nextDayMovement);
    }

    //Открытие смены - если она была закрыта
    public String open(Long id) {
        Optional<MovementsEntity> lastMovementOptional = movementsRepo.findTopByIdUserOrderByIdmovementsDesc(id);

        if (lastMovementOptional.isPresent()) {
            MovementsEntity lastMovement = lastMovementOptional.get();

            if (lastMovement.getOpenClose().equals("close")) {
              return "open";
            }else{
                return " ";
            }
        }else{
            return "open";
        }
    }



    /*************BOT*/
    /** Лист тех кто сейчас на пункте*/
    public List<MovementsEntity> nowOnPlace(String place){
        List<MovementsEntity> entities = movementsRepo.findByPlaceAndExitTimeIsNull(place);
        return entities;
    }


    public String logic_bot(String namePlaceAndPrefixAndId, TelegramBot bot, Long chatId){

        String[] split = namePlaceAndPrefixAndId.split("//");
        if (split.length < 3) {
            bot.execute(new SendMessage(chatId, "Неверный формат веденных данных."));
            return "not_validation";
        }

        String prefix = split[0];
        Long idUser = Long.valueOf(split[1]);
        String namePlace = split[2];
        String timeString = split[3];

        try {
            LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
             bot.execute(new SendMessage(chatId, "Неверный формат веденных данных."));
            return "not_validation";
        }

        if(!itsInDatabase(idUser)){
            return "not_validation";
        }
        bot.execute(new SendMessage(chatId, "ID прошел валидацию..."));




        if(prefix.equals("2222")){
            return writeComingTime_bot(idUser, namePlace, timeString, bot, chatId);
        }else if(prefix.equals("3333")){
            return writeExitTime_bot(idUser, namePlace, timeString, bot, chatId);
        }else{
            return "not_validation";
        }


    }


    public String writeComingTime_bot(Long id, String place, String time,TelegramBot bot, Long chatId){

        UserEntity entityUser = serviceUser.findByUserId(id);
        MovementsEntity movementsEntity = new MovementsEntity();

        if(!itsPlaceNow(id)) {
            movementsEntity.setIdUser(id);
            String name = entityUser.getName();
            movementsEntity.setNameUser(name);
            movementsEntity.setPositionUser(entityUser.getPosition());
            movementsEntity.setDepartmentUser(entityUser.getDepartment());
            movementsEntity.setComingTime(time);
            movementsEntity.setDate(dateNow());
            movementsEntity.setPlace(place);
            movementsEntity.setBotMove("botmove");
            movementsEntity.setOpenClose(open(id));
            movementsRepo.save(movementsEntity);
            bot.execute(new SendMessage(chatId, "Сотрудник: "+ name));
            bot.execute(new SendMessage(chatId, "Пункт: "+ place));
            bot.execute(new SendMessage(chatId, "Время прихода: "+ time));

            return "good"+entityUser.getName();
        }else{
            bot.execute(new SendMessage(chatId, "Сотрудник не покинул пункт!"));

            return "not_exit_place";
        }

    }

    //Уход

    public String writeExitTime_bot(Long id, String place, String time,TelegramBot bot, Long chatId){
        MovementsEntity movementsEntity = movementsRepo.findByIdUserAndExitTimeIsNull(id);

        if(movementsEntity == null){

            bot.execute(new SendMessage(chatId, "Сотрудник не отметился на вход!"));
            return "not_good";
        }

        if(itsPlaceNow(id)) {
            if(itsPlaceMatch(id, movementsEntity.getPlace())){
            movementsEntity.setExitTime(time);
            String name = movementsEntity.getNameUser();
            String coming_time = movementsEntity.getComingTime();

                int comparison = coming_time.compareTo(time);
                if(comparison > 0){
                    bot.execute(new SendMessage(chatId, "Время прихода не может быть больше времени ухода!"));
                    return "not_good";
                }


            String timeAtPlace = countingTheTimeAtPlace(movementsEntity.getComingTime(), movementsEntity.getExitTime());
            movementsEntity.setTimeAtPlace(timeAtPlace);
            movementsRepo.save(movementsEntity);

            bot.execute(new SendMessage(chatId, "Сотрудник: "+ name));
            bot.execute(new SendMessage(chatId, "Пункт: "+ place));
            bot.execute(new SendMessage(chatId, "Время прихода: "+ coming_time));
            bot.execute(new SendMessage(chatId, "Время ухода: "+ time));
            bot.execute(new SendMessage(chatId, "Итого на пункте: "+ timeAtPlace));


                    return "good";

            }else{
                System.out.println("Пункты не совпадают!");
                return "not_match_place";
            }
        }else{
            System.out.println("Вы не отметились на вход!");
            return "not_enter_on_place";
        }

    }

    public void close_bot(Long id, TelegramBot bot, Long chatId){
        MovementsEntity movementsEntity = movementsRepo.findByIdUserAndExitTimeIsNull(id);
        if(movementsEntity == null){


            Optional<MovementsEntity> movementsEntity1 = movementsRepo.findTopByIdUserOrderByIdmovementsDesc(id);
            if(movementsEntity1.get().getOpenClose().equals("close")){
                bot.execute(new SendMessage(chatId, "Смена уже была закрыта!"));
                return;
            }
            closeStatus(id);
            closeWriteTime(id);

            String totalTime = movementsEntity1.get().getOpenCloseTime();
            String localNow = getNowDayOfMonth();
            String effTime = createDiagrams.effectiveTime(String.valueOf(id), localNow , localNow);
            bot.execute(new SendMessage(chatId, "Смена закрыта: "));
            bot.execute(new SendMessage(chatId, "Итого эффективное время: "+ effTime));
            bot.execute(new SendMessage(chatId, "Итого общее время: "+ totalTime));

        }else{
            bot.execute(new SendMessage(chatId, "Смена не закрыта!"));
            bot.execute(new SendMessage(chatId, "Сначала нужно покинуть пункт!"));

        }
    }


    private String getNowDayOfMonth(){
        LocalDate today = LocalDate.now();
        String formatter = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return formatter;
    }




    /***********/

    /** Лист перемещение по дате*/
    public List<MovementsEntity> findMovementsByDate(String date){
        List<MovementsEntity> entities = movementsRepo.findByDate(date);
        return entities;
    }

    /** Лист перемещение по дате и пункту*/

    public List<MovementsEntity> findMovementsByDateAndPlace(String date, String place){
        List<MovementsEntity> entities = movementsRepo.findByDateAndPlace(date, place);
        return entities;
    }
    /** Лист перемещение по дате, пункту и имени сотрудника*/
    public List<MovementsEntity> findMovementsByDateAndPlaceAndNameUser(String date, String place, String nameUser){
        List<MovementsEntity> entities = movementsRepo.findByDateAndPlaceAndNameUser(date, place, nameUser);
        return entities;
    }

    /** Лист перемещение по дате и имени*/

    public List<MovementsEntity> findMovementsByDateAndUserName(String date, String nameUser){
        List<MovementsEntity> entities = movementsRepo.findByDateAndNameUser(date, nameUser);
        return entities;
    }







    /****** Автозакрытие через 8 часов*/
//    @Scheduled(fixedRate = 10000)
    @Scheduled(fixedRate = 1800000)
    public void autoClosing() {
        //Количество часов для автозакрытия

        int val = 7;

        LocalDate today = LocalDate.now();
        // Получить вчерашнюю дату
        LocalDate tomorrow = today.minusDays(1);
        // Получить список всех пользователей, которые не закрыли смену ("open")

        List<MovementsEntity> openMovements = movementsRepo.findLastRecordsWithinDateRange(tomorrow.toString(), today.toString());


        for (MovementsEntity movement : openMovements) {


            // Получить время последнего  выхода
            LocalTime lastExitTime = LocalTime.parse(movement.getExitTime(), TIME_FORMATTER);

            // Получить текущее время
            LocalDateTime now = LocalDateTime.now();

            // Проверить, прошло ли 8 часов
            LocalDateTime exitDateTime = LocalDateTime.of(today, lastExitTime);

            long hours = ChronoUnit.HOURS.between(exitDateTime, now);


            if (hours < 0 || movement.getDate().equals(tomorrow.toString())) {
                hours = hours + 24;
            }

            if(hours >=  val  ) {
                bot_send_dzh(movement.getNameUser());
                Long id = movement.getIdUser();
                closeWriteTimeForAuto(id, lastExitTime);
                closeStatus(id);
            }


        }
    }


    private  AtomicBoolean flag = new AtomicBoolean(false);



        @Scheduled(cron = "20 59 23 * * ?")
//    @Scheduled(cron = "55 36 12 * * ?")
    public void autoCloseAndOpenFor00(){
        flag.set(true);
        System.out.println("Автопереоткрытие работает");
            bot_send_dzh_all("Автопереоткрытие начало работу!");
        List<MovementsEntity> entities = movementsRepo.findByExitTimeIsNull();

           if (entities == null) {
               flag.set(false);
               bot_send_dzh_all("Ошибка получения данных из базы данных.");
            System.out.println("Ошибка получения данных из базы данных.");
            return;
        }

            if(entities.isEmpty()) {
                flag.set(false);
                System.out.println("Никому не потребовалось переоткрыть смену");
                bot_send_dzh_all("Никому не потребовалось переоткрыть смену");
                bot_send_dzh_all("Устновил флаг :"+ flag);
                System.out.println("Устновил флаг :"+ flag);
                return;
            }

            System.out.println("entitities не null");

            for (MovementsEntity movementsEntity : entities) {
                System.out.println("перехожу к итерации");
                movementsEntity.setExitTime(localTimeNow());
                String timeAtPlace = countingTheTimeAtPlace(movementsEntity.getComingTime(), movementsEntity.getExitTime());
                movementsEntity.setTimeAtPlace(timeAtPlace);
                String place = movementsEntity.getPlace();
                System.out.println("Пункт для переотрктия " + place);
                if (movementsEntity.getOpenClose().equals("open")) {

                    movementsEntity.setOpenClose("openclose");
                } else {
                    movementsEntity.setOpenClose("close");
                }

                movementsRepo.save(movementsEntity);
                System.out.println("Сохранил ент");
                System.out.println("считаю время");
                closeWriteTime(movementsEntity.getIdUser());

                if (!place.equals("")) {
                    writeComingAndExitTimeAfterMidnight2(movementsEntity.getIdUser(), place);
                    bot_send_dzh_all("Переоткрыл: " + movementsEntity.getNameUser()+ " " + movementsEntity.getPlace());

                    System.out.println("Переоткрыл: " + movementsEntity.getNameUser() + movementsEntity.getPlace());
                }else{
                    System.out.println("Place null ");
                }
            }
            try {
                System.out.println("Ждем 70 сек");
                Thread.sleep(70000); // 70 секунд

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Восстанавливаем прерванное состояние
                System.err.println("Thread was interrupted, Failed to complete operation");
                flag.set(false);
            }finally{
                System.out.println("установил флаг в finally");
                flag.set(false);
                bot_send_dzh_all("Устновил флаг :"+ flag);
            }
            flag.set(false);
            System.out.println("Устновил флаг :"+ flag);

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
    private void bot_send_dzh_all(String message){
        getBot().execute(new SendMessage(667788774, message));
    }
    private void bot_send_message_by_chat_id(Long chatId, String message){
        getBot().execute(new SendMessage(chatId, message));
    }

    private void bot_tracking_and_notification_of_employee_movements(String name, String time, String place, String status){
        List<Long> listTracking =serviceUser.findAllChatIdByTracking();
        for(int i = 0; i<listTracking.size();i++){
            getBot().execute(new SendMessage(listTracking.get(i), "Сотрудник "+name+" "+status+" "+place+" в "+time+"!" ));
        }

    }




    /*ОТЧЕТЫ UI*/
    //Индивидуальные перемещения
    //Конкретный пункт
    /** Лист по диапазону дат , имени и пункту*/
    public List<MovementsEntity> findMovementsByDateBetweenAndUserNameAndPlace(String dateFrom, String dateTo, String userName, String place){
        List<MovementsEntity> entities = movementsRepo.findByDateBetweenAndPlaceAndNameUser(dateFrom, dateTo, place, userName);
        return entities;
    }
    //Все пункты
    public List<MovementsEntity> findMovementsByDateBetweenAndUserName(String dateFrom, String dateTo, String userName){
        List<MovementsEntity> entities = movementsRepo.findByDateBetweenAndNameUser(dateFrom, dateTo, userName);
        return entities;
    }

    //Общие перемещения
    //Конкретный пункт
    public List<MovementsEntity> findMovementsByDateBetweenAndPlace(String dateFrom, String dateTo, String place){
        List<MovementsEntity> entities = movementsRepo.findByDateBetweenAndPlace(dateFrom, dateTo, place);
        return entities;
    }
    //Все перемещения за диапазон
    public List<MovementsEntity> findMovementsByDateBetween(String dateFrom, String dateTo){
        List<MovementsEntity> entities = movementsRepo.findByDateBetween(dateFrom, dateTo);
        return entities;
    }





    @PostConstruct
    public void init() {

    }





}
