package com.dg.ServerRebornFarmguard.service;

import com.dg.ServerRebornFarmguard.entity.MovementsEntity;
import com.dg.ServerRebornFarmguard.entity.MovementsObshEntity;
import com.dg.ServerRebornFarmguard.entity.UserEntity;
import com.dg.ServerRebornFarmguard.service.reports.excel.CreateDiagrams;
import com.dg.ServerRebornFarmguard.service.reports.excel.MainReports;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Contact;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class TelegramBotService {

    public static TelegramBot bot;
    public static TelegramBot bot_status;

    @Autowired
    private UserService userService;

    @Autowired
    private PlaceService placeService;

    @Autowired
    private MovementsService movementsService;

    @Autowired
    private MovementsObshService movementsObshService;

    @Autowired
    private MainReports mainReports;

    @Autowired
    private CreateDiagrams createDiagrams;

    public void createBot(String botToken){
        this.bot = new TelegramBot(botToken);
        this.chatStates = new HashMap<>();
        start();
    }

    public void createBotStatus(String botToken){
        this.bot_status = new TelegramBot(botToken);
        start_bot_status();
    }



    @Value("${telegramMain.token.test}")
    private String telegramTokenMainTest;

    @Value("${telegramStatus.token.test}")
    private String telegramTokenStatusTest;

    @Value("${telegramMain.token.prod}")
    private String telegramTokenMainProd;

    @Value("${telegramStatus.token.prod}")
    private String telegramTokenStatusProd;



    @PostConstruct
    public void init() {

//        //ОСНОВНЫЕ БОТЫ
      createBot(telegramTokenMainProd);

      createBotStatus(telegramTokenStatusProd);



//      //Test

//      createBot(telegramTokenMainTest);
//      createBotStatus(telegramTokenStatusTest);


    }

    public static TelegramBot getBot() {
        return bot;
    }

    private Map<Long, BotState> chatStates;

    /** Состояния бота*/
    private  enum BotState {
        WAITING_FOR_COMMAND,
        WAITING_FOR_NAME,
        WAITING_FOR_ACCEPT_CLOSE,
        WAITING_FOR_ADMIN,
        WAITING_FOR_REBOOT_SERVER,
        WAITING_FOR_MOVE,
        WAITING_FOR_MOVE_ALL,
        WAITING_FOR_MY_ID,
        WAITING_REG,
        WAITING_ADMIN_PANEL,
        WAITING_FOR_IND_MOVE,
        WAITING_PUSH_ALL,
        WAITING_PUSH_NAME,
        WAITING_PUSH_OTDEL, WAITING_REC_PUSH, WAITING_PUSH_SEND, WAITING_PUSH_SEND_OTDEL, WAITING_ON_PLACE, WAITING_FOR_PERSON_TRACKING, WAITING_PLACE_FOR_MOVE, WAITING_PLACE_DATE,

    }


    public void start_bot_status() {
        bot_status.setUpdatesListener(updates -> {

            for (var update : updates) {
                if (update.message() != null && update.message().text() != null) {
                    long chatId = update.message().chat().id();
                    if (update.message().text().equals("/start")) {

                        bot_status.execute(new SendMessage(chatId, "BOT_STATUS: Работает"));
                    }if(update.message().text().equals("/help")){
                        bot_status.execute(new SendMessage(chatId, "/status "));

                    }

                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

    }


    public void start() {
        bot.setUpdatesListener(updates -> {

            for (var update : updates) {
                if (update.message() != null && update.message().text() != null) {

                    if(update.message().text().equals("/start") ){
                        long chatId = update.message().chat().id();
                        setupBot(chatId);
                        sendWelcomeMessage(chatId);
                    }

//                    if(update.chatMember().chat().id())
                    onUpdateReceived(update);




                }else if(update.message() != null && update.message().contact() != null){
                    CompletableFuture.runAsync(() -> {
                        onUpdateContacts(update);
                    });
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

    }

    /** Регистрация*/
    private void onUpdateContacts(Update update){
        handleContact(update.message().chat().id() ,update.message().contact());

    }

    public void registred(Long chatId, String tel){
        UserEntity entity = userService.findByTel(tel);
        if(entity != null) {
            if(entity.getChatId() == null) {
                entity.setChatId(chatId);
                userService.save(entity);
                bot.execute(new SendMessage(chatId, "Регистрация прошла успешно!").replyMarkup(createBaseKeyboard(chatId)));
                chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);

            }else{

            }
        }else {
            bot.execute(new SendMessage(chatId, "Ваш контакт не найден в базе данных!"));
            bot.execute(new SendMessage(chatId, "Введите Фамилию и Имя сотрудника:"));
            bot.execute(new SendMessage(chatId, "Пример: Иванов Иван"));
            chatStates.put(chatId, BotState.WAITING_REG);
        }
    }

    public void registred_name(Long chatId, String name_reg){
        UserEntity entity = userService.findByName(name_reg);
        if(entity != null){
            entity.setChatId(chatId);
            userService.save(entity);
            bot.execute(new SendMessage(chatId, "Регистрация прошла успешно!").replyMarkup(createBaseKeyboard(chatId)));
            chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
        }else{
            bot.execute(new SendMessage(chatId, "Не удалось пройти регистрацию! Проверьте правильность ввода и повторите попытку!"));
            bot.execute(new SendMessage(chatId, "Введите Фамилию и Имя сотрудника:"));
            bot.execute(new SendMessage(chatId, "Пример: Иванов Иван"));
            chatStates.put(chatId, BotState.WAITING_REG);
        }
    }

    private void handleContact(long chatId, Contact contact) {
        if(contact !=null ) {
            String userPhone = contact.phoneNumber();

            bot.execute(new SendMessage(chatId, "Номер принят :" + userPhone));

                if (!onRegistred(chatId)) {
                    registred(chatId, userPhone);
                } else {
                    bot.execute(new SendMessage(chatId, "Ваш номер уже зарегистрирован :" + userPhone));
                }

            String g = contact.firstName();


            System.out.println("Номер телефона пользователя: " + userPhone);
        }

    }


    /**Настройка бота*/
    private void setupBot(long chatId) {

            String avatarPath = "image/fg_bot_photo.png";

            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(avatarPath);
            Path tempFile = null;
            try {
                tempFile = Files.createTempFile("avatar", ".png");
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                bot.execute(new SendPhoto(chatId, tempFile.toFile()));
                Files.delete(tempFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    private boolean onRegistred(Long chatId){
            if(userService.userByChatId(chatId) != null){
                return true;
            }else{
                return false;
            }
        }


    private String roles(Long chatId){
            String role = userService.userTelegramRole(chatId);
            if(role == null){
                return "user";
            }
            return role;
        }



    private void sendWelcomeMessage(long chatId) {
        if (onRegistred(chatId)) {
            String welcomeMessage = "Добро пожаловать!";
            SendMessage sendMessage;

            // если администратор
            if (roles(chatId).equals("admin")) {
                ReplyKeyboardMarkup keyboardMarkup = createBaseKeyboard(chatId);
                sendMessage = new SendMessage(chatId, welcomeMessage)
                        .replyMarkup(keyboardMarkup);

                bot.execute(sendMessage);
                bot.execute(new SendMessage(chatId, "Права доступа: Admin"));
            } else {
                // если юзер
                ReplyKeyboardMarkup keyboardMarkup = createBaseKeyboard(chatId);

                sendMessage = new SendMessage(chatId, welcomeMessage)
                        .replyMarkup(keyboardMarkup);

                bot.execute(sendMessage);
                bot.execute(new SendMessage(chatId, "Права доступа: User "));
            }

        } else {

            bot.execute(new SendMessage(chatId, "Farmguard Bot привествует Вас!"));
            bot.execute(new SendMessage(chatId, "Вы не зарегистрированы, пройдите реигстрацию, чтобы пользоваться ботом! "));
            bot.execute(new SendMessage(chatId, "Для регистрации отправьте свой 'контакт':").replyMarkup(keyboardContact()));
            sendPhotoBot(chatId, "image/reg_bot.png");
        }
    }

    /** Клавиатура для отправки push*/
    private ReplyKeyboardMarkup pushKeyboard(long chatId){

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(
                new KeyboardButton[] {
                        new KeyboardButton("Лично"),
                        new KeyboardButton("Отделу")

                }

        );
        keyboardMarkup.addRow(new KeyboardButton("Всем сотрудникам"));
        keyboardMarkup.addRow(new KeyboardButton("Отмена"));
        SendMessage sendMessage = new SendMessage(chatId,"Выберите тип отправки:")
                .replyMarkup(keyboardMarkup);

        bot.execute(sendMessage);

        return keyboardMarkup;
    }

     /** Принимает путь и chatId, и отправляет фото*/
    private void sendPhotoBot(long chatId, String path) {

        String avatarPath = path;
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(avatarPath);
        Path tempFile2;
        try {
            tempFile2 = Files.createTempFile("avatar", ".jpg");
            Files.copy(inputStream, tempFile2, StandardCopyOption.REPLACE_EXISTING);
            bot.execute(new SendPhoto(chatId, tempFile2.toFile()));
            Files.delete(tempFile2);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


     /** Для создания клавиатуры в зависмоисти от роли*/
    private ReplyKeyboardMarkup createBaseKeyboard(long chatId)  {
        String role = roles(chatId);
        ReplyKeyboardMarkup replyKeyboardMarkup = null;
        switch (role){
            case "admin":
                 replyKeyboardMarkup = adminKeyboard(chatId);
                break;
            case "user":
                replyKeyboardMarkup = userKeyboard(chatId);
                break;
        }

        return replyKeyboardMarkup;
    }

    /** Клавиатура для админа*/
    private ReplyKeyboardMarkup adminKeyboard(Long chatId){
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(
                new KeyboardButton[] {
                        new KeyboardButton("Индивидуальные перемещения"),
                        new KeyboardButton("Общие перемещения"),


                }
        );
        keyboardMarkup.addRow(new KeyboardButton("Перемещения по пунктам"), new KeyboardButton("Мои перемещения"));
        keyboardMarkup.addRow(new KeyboardButton("Сейчас на пункте"), new KeyboardButton("Закрыть смену"));
        keyboardMarkup.addRow(new KeyboardButton("Личный кабинет"), new KeyboardButton("Админ панель"));
        keyboardMarkup.addRow(new KeyboardButton("Назад"));

        SendMessage sendMessage = new SendMessage(chatId,"")
                .replyMarkup(keyboardMarkup);

        bot.execute(sendMessage);
        return  keyboardMarkup;

    }

    /** Клавиатура для админ-панели*/
    private ReplyKeyboardMarkup adminPanelKeyboard(){

        ReplyKeyboardMarkup admin_keyboard = new ReplyKeyboardMarkup(
                new KeyboardButton[] {
                        new KeyboardButton("Записать/выписать с пункта"),
                        new KeyboardButton("Узнать ID сотрудника")

                }

        );
        admin_keyboard.addRow(new KeyboardButton("Перезагрузка сервера"), new KeyboardButton("Отправить PUSH"));
        admin_keyboard.addRow(new KeyboardButton("Назад"), new KeyboardButton("Выйти"));

        return  admin_keyboard;
    }


    /** Клавиатура закрытия смены*/
    private ReplyKeyboardMarkup closeKeyboard(){

        ReplyKeyboardMarkup close_keyboard = new ReplyKeyboardMarkup(
                new KeyboardButton[] {
                        new KeyboardButton("Закрыть"),
                        new KeyboardButton("Отмена")

                }
        );

        return close_keyboard;
    }


    /** Клавиатура для User*/
    private ReplyKeyboardMarkup userKeyboard(Long chatId){
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(
                new KeyboardButton[] {
                        new KeyboardButton("Закрыть смену"),
                        new KeyboardButton("Мои перемещения")

                }

        );
        keyboardMarkup.addRow(new KeyboardButton("Личный кабинет"));
        keyboardMarkup.addRow(new KeyboardButton("Назад"));
        SendMessage sendMessage = new SendMessage(chatId,"")
                .replyMarkup(keyboardMarkup);

        bot.execute(sendMessage);
        return keyboardMarkup;

    }

     /** Возвращаем клавиатуру для отправки контакта при регистрации*/
    private Keyboard keyboardContact(){
        return new ReplyKeyboardMarkup(
                new KeyboardButton("Отправить контакт").requestContact(true));
    }


    /** Бот отправляет список секретных команд*/
    private void secretComand(long chatId){
        bot.execute(new SendMessage(chatId, "Включение общего трекинга перемещений сотрудников: \n /add_tracking"));
        bot.execute(new SendMessage(chatId, "Отключение общего трекинга перемещений сотрудников: \n /del_tracking"));
        bot.execute(new SendMessage(chatId, "Подключение персонального трекинга для выбранного сотрудника:\n /person_tracking "));
        bot.execute(new SendMessage(chatId, "Отключение персонального трекинга для выбранного сотрудника: \n /stop_person_tracking"));

    }

    /** Возвращаем имя пользователя по chatId*/
    private String nameUser(Long chatId){
        return userService.userByChatId(chatId).getName();
    }

    /** Клавиатура выбора пункта*/
    private ReplyKeyboardMarkup selectPlace(long chatId) {

        List<String> placesNames = placeService.placesName();

        List<String> sortedPlacesNames = placesNames.stream().sorted().toList();

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup( new KeyboardButton[] {
        });
        for (String e : sortedPlacesNames){
            keyboardMarkup.addRow(new KeyboardButton(e));
        }
        keyboardMarkup.addRow(new KeyboardButton("Отмена"));

        SendMessage sendMessage = new SendMessage(chatId,"Выберите пункт:")
                .replyMarkup(keyboardMarkup);

        bot.execute(sendMessage);

        return keyboardMarkup;
    }

    /** Отправка мне Имен тех кто воспользовался ботом*/
    private void sendMessageByDzhuraevGrigory(Long chatId, String message){
        bot.execute(new SendMessage(667788774, nameUser(chatId) +" - "+message ));
    }



    /** Клавиатура выбора сотрудника*/
    private ReplyKeyboardMarkup selectEmployeesName(long chatId)  {
        List<String> employees = userService.findAllUserName();
        List<String> sortedEmployees = employees.stream()
                .sorted().toList();

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup( new KeyboardButton[] {});
        for (String e :  sortedEmployees){
            keyboardMarkup.addRow(new KeyboardButton(e));
        }
        keyboardMarkup.addRow(new KeyboardButton("Отмена"));
        SendMessage sendMessage = new SendMessage(chatId,"Выберите сотрудника:")
                .replyMarkup(keyboardMarkup);

        bot.execute(sendMessage);
        return keyboardMarkup;
    }

    /** Создает подсказку диапазона дат для отпраки боту */
    private String createHintDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        return today+ "/" + tomorrow;
    }

    /** Возвращает список чат-айди всех пользователей*/
    private List<Long> chatIdAllUsers(){
        List<Long> chatIds  = new ArrayList<>();

           List<UserEntity> all  = userService.findAllUsers();
           for(UserEntity entity: all){
             chatIds.add(entity.getChatId());
           }
        return chatIds;
    }

    /** Клавиатура выбора отдела*/
    private ReplyKeyboardMarkup selectDepartment(long chatId)  {
        List<String> departments = userService.findAllDepartments();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup( new KeyboardButton[] {});
        for (String e : departments){
            keyboardMarkup.addRow(new KeyboardButton(e));
        }
        keyboardMarkup.addRow(new KeyboardButton("Отмена"));
        SendMessage sendMessage = new SendMessage(chatId,"Выберите отдел:")
                .replyMarkup(keyboardMarkup);

        bot.execute(sendMessage);
        return keyboardMarkup;
    }

    /** Узнать id*/
    private String myId(Long chatId){
        UserEntity entity = userService.userByChatId(chatId);
        return String.valueOf(entity.getIduser());
    }
    /** Узнать chatId по имени*/
    private Long getNameNote(String name){
        Long l = userService.chatIdUserByName(name);
        return l;

    }
    /** Возвращет лист chatId всех сотрудников отдела*/
    private List<Long> getDepartmentChatIdUsers(String department){
        List<Long> list = new ArrayList<>();
        List<UserEntity> entities = userService.findByDepartment(department);

        for(UserEntity entity: entities){
            list.add(entity.getChatId());
        }

        return list;
    }

    /** Бот отправляет сообщение по chatID*/
    private void sendNote(long chatId, String text){
        bot.execute(new SendMessage(chatId, text));
    }

    /** Отправка сообщение всем Юзерам*/
    private void sendPushAll(String message) {
        for (Long user_chatId : chatIdAllUsers()) {
            if(user_chatId != null) {
                sendNote(user_chatId, message);
            }
        }
    }

    /** Отправляет сообщение по выбранному отделу*/
    private void sendPushByDepartment(String message, String department)  {
        for (Long user_chatId : getDepartmentChatIdUsers(department)) {
            sendNote(user_chatId, message);
        }
    }

    /** Отправляет сообщение конкретному сотруднику*/
    private void sendPushName(String message, String name, Long chatId)  {
        Long chatid_push = getNameNote(name);
        if(chatid_push != null){
            sendNote(getNameNote(name), message);
            bot.execute(new SendMessage(chatId, "Сообщение отправлено!").replyMarkup(adminPanelKeyboard()));
        }else{

            bot.execute(new SendMessage(chatId, "Сообщение не отправлено! Сотрудник не найден!").replyMarkup(adminPanelKeyboard()));
        }
    }

    /** Отправляет пользователю сообщение о том , кто сейчас на пункте*/
    private void sendMessageNowOnPlace(Long chatId, String place){

        //Если Хаб


        if(placeService.itsHubFindByName(place)){
            
            
           List<MovementsObshEntity> list = movementsObshService.nowOnPlace(place);

            if (!list.isEmpty()) {
                sendNote(chatId, "Cейчас на пункте " + place + ":");
                for (MovementsObshEntity entity : list) {
                    sendNote(chatId, entity.getNameUser() + "  |  " + entity.getComingTime());
                }
                int i = list.size();
                sendNote(chatId, "Итого сотрудников на пункте:" + i);
            } else {
                sendNote(chatId, "На выбранном пункте нет сотрудников!");
            }



        }else {


            //Если не хаб
            List<MovementsEntity> list = movementsService.nowOnPlace(place);
            if (!list.isEmpty()) {
                sendNote(chatId, "Cейчас на пункте " + place + ":");
                for (MovementsEntity entity : list) {
                    sendNote(chatId, entity.getNameUser() + "  |  " + entity.getComingTime());
                }
                int i = list.size();
                sendNote(chatId, "Итого сотрудников на пункте:" + i);
            } else {
                sendNote(chatId, "На выбранном пункте нет сотрудников!");
            }

        }

    }
     /** Принимает массив байт и отправляет ExcelFile */
    private void sendExcelFile(Long chatId, byte[] bytes) throws IOException {
        bot.execute(new SendMessage(chatId, "стилизую файл..."));
        File tempFile = File.createTempFile("file", ".xlsx");

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
         out.write(bytes);
        }
        bot.execute(new SendDocument(chatId, tempFile));
        tempFile.deleteOnExit();
        bot.execute(new SendMessage(chatId, "Отчет сформирован! Жду новую команду!"));
    }

    private String id_move = "";
    private  String name_move ="";
    private String push_name = "";
    private String push_otdel = "";
    public String selected_place_move = "";

    private String getFirstDayOfMonth() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        return firstDayOfMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String getLastDayOfMonth() {
        LocalDate today = LocalDate.now();
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());


        return lastDayOfMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String getNowDayOfMonth(){
        LocalDate today = LocalDate.now();
        return today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String findUserIdByName(String name){

        return String.valueOf(userService.findUserIdByName(name));
    }


    public synchronized void onUpdateReceived(Update update) {
        if (update.message() != null && update.message().text() != null) {

            long chatId = update.message().chat().id();
            //Отправка мне тех - кто воспользовался ботом

            String text = update.message().text();
            BotState currentState = chatStates.getOrDefault(chatId, BotState.WAITING_FOR_COMMAND);


                if(roles(chatId).equals("admin")) {

                adminLogicUpdate(chatId, currentState, text);
                    // обработка команд если не админ
                }else{

                    switch (currentState) {
                        case WAITING_REG:
                            String name_reg = update.message().text();
                            registred_name(chatId, name_reg);
                            break;

                        case WAITING_FOR_COMMAND:

                            switch (text) {

                                case "Закрыть смену":
                                    chatStates.put(chatId, BotState.WAITING_FOR_ACCEPT_CLOSE);
                                    bot.execute(new SendMessage(chatId, "Подтвердите закрытие смены").replyMarkup(closeKeyboard()));
                                    sendMessageByDzhuraevGrigory(chatId, "USEBOT: Закрывает смену");
                                    break;



                                case "Мои перемещения":
                                    chatStates.put(chatId, BotState.WAITING_FOR_MOVE);
                                    bot.execute(new SendMessage(chatId, "Введите период:"));
                                    bot.execute(new SendMessage(chatId, "Формат дат должен быть таким:"));
                                    bot.execute(new SendMessage(chatId, "Пример: "));
                                    bot.execute(new SendMessage(chatId, createHintDateRange()));
                                    sendMessageByDzhuraevGrigory(chatId, "USEBOT: Мои перемещения");
                                    break;


                                case "Личный кабинет":
                                    bot.execute(new SendMessage(chatId, nameUser(chatId)+", "+"добрый день!"));
                                    bot.execute(new SendMessage(chatId, "Доступ: user"));
                                    String id_lk = myId(chatId);
                                    bot.execute(new SendMessage(chatId, "Ваш ID: "+id_lk));
                                    createDiagrams.createEffTimeDiagramOnDifferentPlaceAndSendBot(chatId,id_lk , bot);
                                    String effToday = createDiagrams.effectiveTime(id_lk, getNowDayOfMonth(), getNowDayOfMonth());
                                    String effOfMonth = createDiagrams.effectiveTime(id_lk, getFirstDayOfMonth(), getLastDayOfMonth());
                                    String totalOfMonth = createDiagrams.totalTime(id_lk, getFirstDayOfMonth(), getLastDayOfMonth());
                                    bot.execute(new SendMessage(chatId, "Эффективное время за текущий день: "+effToday));
                                    bot.execute(new SendMessage(chatId, "Эффективное время за текущий месяц: "+effOfMonth));
                                    bot.execute(new SendMessage(chatId, "Общее время за текущий месяц: "+totalOfMonth));
                                    sendMessageByDzhuraevGrigory(chatId, "USEBOT: Личный кабинет");
                                    break;

                                default:
                                    if (!text.equals("/start")) {
                                        if (update.message().text().equals("Назад")) {
                                            chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                                            bot.execute(new SendMessage(update.message().chat().id(), "Вы в главном меню! Жду новую команду!"));
                                            break;
                                        }

                                        bot.execute(new SendMessage(chatId, "Такой команды нет! Введите корректную команду:"));
                                        break;
                                    }
                            }

                            break;

                        /*Закрытие смены - р  */
                        case WAITING_FOR_ACCEPT_CLOSE:


                                if (update.message().text().equals("Отмена")) {
                                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                                    bot.execute(new SendMessage(update.message().chat().id(), "Вы в главном меню. Жду новую команду!").replyMarkup(createBaseKeyboard(chatId)));
                                    break;
                                }else if(update.message().text().equals("Закрыть")) {
                                    //
                                    String id = myId(chatId);
                                    movementsService.close_bot(Long.valueOf(id),bot, chatId);
                                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                                    bot.execute(new SendMessage(update.message().chat().id(), "Вы в главном меню. Жду новую команду!").replyMarkup(createBaseKeyboard(chatId)));
                                }

                            break;


                        /* Мои перемещения USER */

                        case WAITING_FOR_MOVE:
                            if (text.equals("Назад")) {
                                chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                                bot.execute(new SendMessage(chatId, "Вы в главном меню. Жду новую команду!"));
                                break;
                            }
                            String date = text;
                            if(isValidDate(date)){
                                try {
                                    bot.execute(new SendMessage(chatId, "подготавливаю файл..."));
                                    byte[] bytes = mainReports.generateExcelForDateBetweenAndUserName(date, nameUser(chatId) );
                                    if(bytes.length == 1){
                                        bot.execute(new SendMessage(chatId, "перемещения отсутствуют!"));
                                    }else {
                                        sendExcelFile(chatId, bytes);
                                    }
                                } catch (IOException e) {
                                    sendNote(chatId, "что-то пошло не так , повторите попытку...");
                                }

                                chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);

                            } else {
                                bot.execute(new SendMessage(chatId, "Неверный ввод!"));
                                bot.execute(new SendMessage(chatId, "Повторите попытку!"));
                            }
                            break;


                        default:
                            // Обработайте неожиданное состояние
                            break;


                    }

                }

        }


    }

    /** Свитч ожидания команды для адмнов*/
    public void adminWaitingForCommand(long chatId, String text){
        switch (text) {

            case "/???":
                secretComand(chatId);
                break;
            case "Перемещения по пунктам":
                chatStates.put(chatId, BotState.WAITING_PLACE_FOR_MOVE);
                selectPlace(chatId);
                break;
            case "Индивидуальные перемещения":
                chatStates.put(chatId, BotState.WAITING_FOR_NAME);
                selectEmployeesName(chatId);
                break;
            case "Общие перемещения":
                chatStates.put(chatId, BotState.WAITING_FOR_MOVE_ALL);
                groupMovementForWaitingCommand(chatId);
                break;
            case "Мои перемещения":
                chatStates.put(chatId, BotState.WAITING_FOR_MOVE);
                personalMovementForWaitingCommand(chatId);
                break;

            case "Закрыть смену":
                chatStates.put(chatId, BotState.WAITING_FOR_ACCEPT_CLOSE);
                bot.execute(new SendMessage(chatId, "Подтвердите закрытие смены").replyMarkup(closeKeyboard()));
                break;

            case "Личный кабинет":
                personCabinet(chatId, "Админ");
                break;
            case "Админ панель":
                chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
                bot.execute(new SendMessage(chatId, "Вы перешли в админ панель!").replyMarkup(adminPanelKeyboard()));
                break;
            case "Сейчас на пункте":
                chatStates.put(chatId, BotState.WAITING_ON_PLACE);
                selectPlace(chatId);
                break;
            case "/add_tracking":
                bot.execute(new SendMessage(chatId, "Отслеживание перемещений сотрудников включено!"));
                break;
            case "/del_tracking":
                bot.execute(new SendMessage(chatId, "Отслеживание перемещений сотрудников отключено!"));

                break;
            case "/person_tracking":
                chatStates.put(chatId, BotState.WAITING_FOR_PERSON_TRACKING);
//                                    select_employees_name_include_MK(chatId);
                break;
            case "/stop_person_tracking":
//                                    stopPersonTracking(chatId);
                bot.execute(new SendMessage(chatId, "Отслеживание индивидуальных перемещений сотрудников отключено!"));
                break;

            case "Выйти":
                chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                bot.execute(new SendMessage(chatId, "Вы перешли в главное меню!").replyMarkup(createBaseKeyboard(chatId)));
                break;

            default:
                if (!text.equals("/start")) {
                    if (text.equals("Назад")) {
                        chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                        bot.execute(new SendMessage(chatId, "Вы в главном меню! Жду новую команду!"));
                        break;
                    }
                    bot.execute(new SendMessage(chatId, "Такой команды нет! Введите корректную команду:"));
                    break;
                }


        }


    }

        /** Методы для adminWaitingForCommand*/

        /** Общие перемещения*/
        private void groupMovementForWaitingCommand(long chatId){
            bot.execute(new SendMessage(chatId, "Введите период:"));
            bot.execute(new SendMessage(chatId, "Формат дат должен быть таким:"));
            bot.execute(new SendMessage(chatId, "Пример: "));
            bot.execute(new SendMessage(chatId, createHintDateRange()));
        }

        /** Личные перемещения*/
        private void personalMovementForWaitingCommand(long chatId){
            bot.execute(new SendMessage(chatId, "Введите период:"));
            bot.execute(new SendMessage(chatId, "Формат дат должен быть таким:"));
            bot.execute(new SendMessage(chatId, "Пример: "));
            bot.execute(new SendMessage(chatId, createHintDateRange()));
        }

        /** Личные кабинет*/
        private void personCabinet(long chatId, String role){
            bot.execute(new SendMessage(chatId, nameUser(chatId)+", "+"добрый день!"));
            bot.execute(new SendMessage(chatId, "Доступ: "+role));
            String id_lk = myId(chatId);
            bot.execute(new SendMessage(chatId, "Ваш ID: "+id_lk));
            createDiagrams.createEffTimeDiagramOnDifferentPlaceAndSendBot(chatId,id_lk , bot);
            String effToday = createDiagrams.effectiveTime(id_lk, getNowDayOfMonth(), getNowDayOfMonth());
            String effOfMonth = createDiagrams.effectiveTime(id_lk, getFirstDayOfMonth(), getLastDayOfMonth());
            String totalOfMonth = createDiagrams.totalTime(id_lk, getFirstDayOfMonth(), getLastDayOfMonth());
            bot.execute(new SendMessage(chatId, "Эффективное время за текущий день: "+effToday));
            bot.execute(new SendMessage(chatId, "Эффективное время за текущий месяц: "+effOfMonth));
            bot.execute(new SendMessage(chatId, "Общее время за текущий месяц: "+totalOfMonth));
        }

    /** WAITING_ADMIN_PANEL*/
    private void adminWaitingAdminPanel(long chatId, String text){
        switch (text){
            case "Записать/выписать с пункта":
                chatStates.put(chatId, BotState.WAITING_FOR_ADMIN);
                bot.execute(new SendMessage(chatId, "Вы перешли в режим удаленного управления перемещениями:"));
                bot.execute(new SendMessage(chatId, "Введите: 2222//ID//Название пункта//Время - для прихода:"));
                bot.execute(new SendMessage(chatId, "Введите: 3333//ID//Название пункта//Время - для ухода:"));
                bot.execute(new SendMessage(chatId, "Пример:"));
                bot.execute(new SendMessage(chatId, "2222//10101034300//Офис//17:00"));
                break;
            case "Перезагрузка сервера":
                chatStates.put(chatId, BotState.WAITING_FOR_REBOOT_SERVER);
                bot.execute(new SendMessage(chatId, "Вы перешли в режим перезагрузки сервера!"));
                bot.execute(new SendMessage(chatId, "Введите необходимую команду:"));
                break;


            case "Узнать ID сотрудника":
                chatStates.put(chatId, BotState.WAITING_FOR_MY_ID);
                selectEmployeesName(chatId);
                break;
            case "Отправить PUSH":
                pushKeyboard(chatId);
                chatStates.put(chatId, BotState.WAITING_REC_PUSH);
                break;


            default:
                if (text.equals("Выйти")) {
                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                    bot.execute(new SendMessage(chatId, "Вы в главном меню. Жду новую команду!").replyMarkup(createBaseKeyboard(chatId)));
                    break;
                }else if(text.equals("Назад")){
                    sendNote(chatId, "Вы уже находитесь в админ панели!");
                }else{
                    sendNote(chatId, "Такой команды нет в админ панели!");
                }

        }
    }

    /** WAITING_REC_PUSH*/
    private void adminWaitingRecPush(long chatId, String text){
        switch (text){
            case "Лично":
                chatStates.put(chatId, BotState.WAITING_PUSH_NAME);
                selectEmployeesName(chatId);
                break;
            case "Отделу":
                chatStates.put(chatId, BotState.WAITING_PUSH_OTDEL);
                selectDepartment(chatId);
                break;
            case "Всем сотрудникам":
                chatStates.put(chatId, BotState.WAITING_PUSH_ALL);
                bot.execute(new SendMessage(chatId, " Введите сообщение:").replyMarkup(adminPanelKeyboard()));
                break;
            case "Отмена":
                chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
                bot.execute(new SendMessage(chatId, "Вы вернулись в админ панель!").replyMarkup(adminPanelKeyboard()));
                break;
            default:
                if (!text.equals("/start")) {
                    if (text.equals("Назад")) {
                        chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                        bot.execute(new SendMessage(chatId, "Вы в главном меню! Жду новую команду!"));
                        break;
                    }
                    bot.execute(new SendMessage(chatId, "Такой команды нет! Введите корректную команду:"));
                    break;
                }
        }

    }
        /** Waiting_push_all*/
        private void adminWaitingPushAll(long chatId, String text){
            if(text.equals("Назад")){
                chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
                bot.execute(new SendMessage(chatId, "Вы вернулись в админ панель").replyMarkup(adminPanelKeyboard()));

            }else {
                bot.execute(new SendMessage(chatId, "отправляю..."));
                String push_all = nameUser(chatId) + ": " + text;
                sendPushAll(push_all);
                bot.execute(new SendMessage(chatId, "Сообщение отправлено!").replyMarkup(adminPanelKeyboard()));
                chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
            }
        }
        /** Waiting_push_name*/
        private void adminWaitingPushName(long chatId, String text){
            if(text.equals("Отмена")){
                chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
                bot.execute(new SendMessage(chatId, "Вы вернулись в админ панель").replyMarkup(adminPanelKeyboard()));
            }else{
                push_name = text;
                chatStates.put(chatId, BotState.WAITING_PUSH_SEND);
                sendNote(chatId, "Введите сообщение:");
            }
        }
        /** */

    /** Главный метод который собирает всю логику действий админа в боте*/
    public void adminLogicUpdate(long chatId, BotState currentState, String text){

        switch (currentState) {
            case WAITING_FOR_COMMAND:
            adminWaitingForCommand(chatId, text);
                break;

            /*Person tracking*/
            case WAITING_FOR_PERSON_TRACKING:

//                            startPersonTracking(update.message().text(), chatId);
                chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                bot.execute(new SendMessage(chatId, "Отслеживание перемещений сотрудника началось!").replyMarkup(createBaseKeyboard(chatId)));
                break;

            /* Админ панель - р(дополнить) */
            case WAITING_ADMIN_PANEL:
               adminWaitingAdminPanel(chatId, text);
                break;

            /* Пуши */
            case WAITING_REC_PUSH:
                if (text.equals("Назад")) {
                    chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
                    bot.execute(new SendMessage(chatId, "Вы вернулись в админ панель! Жду новую команду!"));
                    break;
                }
                if (text.equals("Выйти")) {
                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                    bot.execute(new SendMessage(chatId, "Вы в главном меню. Жду новую команду!").replyMarkup(createBaseKeyboard(chatId)));
                    break;
                }
                adminWaitingRecPush(chatId, text);
                break;

            case WAITING_PUSH_ALL:
                adminWaitingPushAll(chatId, text);
                break;
            case WAITING_PUSH_NAME:
                adminWaitingPushName(chatId, text);
                break;
            case WAITING_PUSH_SEND:
                if(text.equals("Отмена")){
                    chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
                    bot.execute(new SendMessage(chatId, "Вы вернулись в админ панель").replyMarkup(adminPanelKeyboard()));

                }else {
                    bot.execute(new SendMessage(chatId, "отправляю..."));
                    sendPushName(nameUser(chatId) + ": " + text, push_name, chatId);

                    chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
                }
                break;
            case WAITING_PUSH_OTDEL:
                push_otdel = text;
                if(push_name.equals("Отмена")){
                    chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
                    bot.execute(new SendMessage(chatId, "Вы вернулись в админ панель").replyMarkup(adminPanelKeyboard()));

                }else {
                    chatStates.put(chatId, BotState.WAITING_PUSH_SEND_OTDEL);
                    sendNote(chatId, "Введите сообщение отделу:");
                }
                break;
            case WAITING_PUSH_SEND_OTDEL:
                if(text.equals("Отмена")){
                    chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
                    bot.execute(new SendMessage(chatId, "Вы вернулись в админ панель").replyMarkup(adminPanelKeyboard()));

                }else {
                    bot.execute(new SendMessage(chatId, "отправляю..."));
                    sendPushByDepartment(nameUser(chatId) + ": " + text, push_otdel);
                    bot.execute(new SendMessage(chatId, "Сообщение отправлено!").replyMarkup(adminPanelKeyboard()));
                    chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
                }
                break;




            /* Закрытие смены */
            case WAITING_FOR_ACCEPT_CLOSE:


                if (text.equals("Отмена")) {
                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                    bot.execute(new SendMessage(chatId, "Вы в главном меню. Жду новую команду!").replyMarkup(createBaseKeyboard(chatId)));
                    break;
                }else if(text.equals("Закрыть")) {
                    //
                    String id = myId(chatId);
                    movementsService.close_bot(Long.valueOf(id),bot, chatId);
                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                    bot.execute(new SendMessage(chatId, "Вы в главном меню. Жду новую команду!").replyMarkup(createBaseKeyboard(chatId)));
                }

                break;
            /*Сейчас на пункте*/
            case WAITING_ON_PLACE:
                if (text.equals("Отмена")) {
                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                    bot.execute(new SendMessage(chatId, "Вы в главном меню! Жду новую команду!").replyMarkup(createBaseKeyboard(chatId)));
                    break;
                }
                sendMessageNowOnPlace(chatId, text);
                chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                bot.execute(new SendMessage(chatId, "Вы в главном меню! Жду новую команду!").replyMarkup(createBaseKeyboard(chatId)));

                break;
            /*Перемещение по пунктам - ожидание даты*/
            case WAITING_PLACE_FOR_MOVE:
                if (text.equals("Отмена")) {
                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                    bot.execute(new SendMessage(chatId, "Вы в главном меню! Жду новую команду!").replyMarkup(createBaseKeyboard(chatId)));
                    break;
                }
                selected_place_move = text;
                bot.execute(new SendMessage(chatId, "Введите период:"));
                bot.execute(new SendMessage(chatId, "Формат дат должен быть таким:"));
                bot.execute(new SendMessage(chatId, "Пример: "));
                bot.execute(new SendMessage(chatId, createHintDateRange()).replyMarkup(createBaseKeyboard(chatId)));
                chatStates.put(chatId, BotState.WAITING_PLACE_DATE);



                break;
            /*Перемещение по пунктам обработка даты*/
            case WAITING_PLACE_DATE:
                if (text.equals("Назад")) {
                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                    bot.execute(new SendMessage(chatId, "Вы в главном меню! Жду новую команду!"));
                    sendMessageByDzhuraevGrigory(chatId, "A_USEBOT: Перемещения по пунктам :"+selected_place_move +"Назад");

                    break;
                }
                String date_all_p = text;
                if(isValidDate(date_all_p)){
                    bot.execute(new SendMessage(chatId, "подготавливаю файл..."));

                    try {
                        byte[] bytes = mainReports.generateExcelForDateBetweenAndPlace(date_all_p, selected_place_move);
                        if(bytes.length == 1){
                            bot.execute(new SendMessage(chatId, "перемещения отсутствуют!"));
                        }else {
                            sendExcelFile(chatId, bytes);
                        }

                    } catch (IOException e) {
                        sendNote(chatId, "что-то пошло не так , повторите попытку...");
                    }
                        chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                } else {
                    bot.execute(new SendMessage(chatId, "Неверный ввод!"));
                    bot.execute(new SendMessage(chatId, "Повторите попытку!"));
                }
                sendMessageByDzhuraevGrigory(chatId, "A_USEBOT: Перемещения по пунктам :"+selected_place_move +" "+ date_all_p);

                break;

            /* Общие перемещения */
            case WAITING_FOR_MOVE_ALL:
                if (text.equals("Назад")) {
                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                    bot.execute(new SendMessage(chatId, "Вы в главном меню! Жду новую команду!"));
                    break;
                }
                String date_all = text;
                if(isValidDate(date_all)){
                    try {
                        bot.execute(new SendMessage(chatId, "подготавливаю файл..."));
                        byte[] bytes = mainReports.generateExcelForDateBetween(date_all);
                        if(bytes.length == 1){
                            bot.execute(new SendMessage(chatId, "перемещения отсутствуют!"));
                        }else {
                            sendExcelFile(chatId, bytes);
                        }
                    } catch (IOException e) {
                        sendNote(chatId, "что-то пошло не так , повторите попытку...");
                    }

                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);

                } else {
                    bot.execute(new SendMessage(chatId, "Неверный ввод!"));
                    bot.execute(new SendMessage(chatId, "Повторите попытку!"));
                }
                sendMessageByDzhuraevGrigory(chatId, "A_USEBOT:  Общие перемещения :" + date_all);
                break;


            /* Мои перемещения */
            case WAITING_FOR_MOVE:
                if (text.equals("Назад")) {
                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                    bot.execute(new SendMessage(chatId, "Вы в главном меню. Жду новую команду!"));
                    break;
                }
                String date = text;
                if(isValidDate(date)){
                    try {
                        bot.execute(new SendMessage(chatId, "подготавливаю файл..."));
                        byte[] bytes = mainReports.generateExcelForDateBetweenAndUserName(date, nameUser(chatId) );
                        if(bytes.length == 1){
                            bot.execute(new SendMessage(chatId, "перемещения отсутствуют!"));
                        }else {
                            sendExcelFile(chatId, bytes);
                        }
                    } catch (IOException e) {
                        sendNote(chatId, "что-то пошло не так , повторите попытку...");
                    }

                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);

                } else {
                    bot.execute(new SendMessage(chatId, "Неверный ввод!"));
                    bot.execute(new SendMessage(chatId, "Повторите попытку!"));
                }
                sendMessageByDzhuraevGrigory(chatId, "A_USEBOT: Мои перемещения :"+selected_place_move +" ");

                break;

            /* Ожидание для ввода имени*/
            case WAITING_FOR_NAME:
                System.out.println(currentState);
                if (text.equals("Назад")) {
                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                    bot.execute(new SendMessage(chatId, "Вы вернулись в главное меню! Жду новую команду!"));
                    break;
                }
                if(text.equals("Отмена")){
                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                    bot.execute(new SendMessage(chatId, "Вы вернулись в главное меню").replyMarkup(createBaseKeyboard(chatId)));
                    break;
                }else {

                    name_move = text;
                    chatStates.put(chatId, BotState.WAITING_FOR_IND_MOVE);
                    bot.execute(new SendMessage(chatId, "Введите период:").replyMarkup(createBaseKeyboard(chatId)));
                    bot.execute(new SendMessage(chatId, "Формат дат должен быть таким:"));
                    bot.execute(new SendMessage(chatId, "Пример: "));
                    bot.execute(new SendMessage(chatId, createHintDateRange()));
                }

                break;


            /* Индивидуальные перемещения */

            case WAITING_FOR_IND_MOVE:
                System.out.println(currentState);

                if (text.equals("Назад")) {
                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                    bot.execute(new SendMessage(chatId, "Вы в главном меню. Жду новую команду!"));
                    sendMessageByDzhuraevGrigory(chatId, "A_USEBOT:  Инд. перемещения : Назад");

                    break;
                }
                if(text.equals("Отмена")){
                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                    bot.execute(new SendMessage(chatId, "Вы вернулись в главное меню!").replyMarkup(createBaseKeyboard(chatId)));
                    break;
                }


                String date1 = text;


                if(isValidDate(date1)){
                    try {

                        bot.execute(new SendMessage(chatId, "подготавливаю файл..."));
                        byte[] bytes = mainReports.generateExcelForDateBetweenAndUserName(date1, name_move);
                        byte[] bytes2 = mainReports.generateExcelForDateBetweenAndNameForHub(date1, name_move);

                        boolean hasMoves = (bytes.length > 1) || (bytes2.length > 1);

                        if(!hasMoves){
                            bot.execute(new SendMessage(chatId, "перемещения отсутствуют!"));
                        }else {
                            if(bytes.length > 1) {
                                sendExcelFile(chatId, bytes);
                            }
                            if(bytes2.length > 1){
//                                bot.execute(new SendMessage(chatId, "формирую пере!"));
                                sendExcelFile(chatId, bytes2);
                            }
                        }


                    } catch (IOException e) {
                        sendNote(chatId, "что-то пошло не так , повторите попытку...");
                    }
                    sendMessageByDzhuraevGrigory(chatId, "A_USEBOT:  Инд. перемещения :"+name_move +" "+ date1);

                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);

                } else {
                    bot.execute(new SendMessage(chatId, "Неверный ввод!"));
                    bot.execute(new SendMessage(chatId, "Повторите попытку!"));
                }


                break;
            // АДМИН ПАНЕЛЬ
            /* Приход/Уход */
            // Для удаленного прихода и ухода
            case WAITING_FOR_ADMIN:

                if (text.equals("Назад")) {
                    chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
                    bot.execute(new SendMessage(chatId, "Вы вернулись в админ панель! Жду новую команду!"));
                    sendMessageByDzhuraevGrigory(chatId, "A_USEBOT: П/У :  назад");
                    break;
                }
                try {


                    String id_id = text;
                    try {
                        movementsService.logic_bot(id_id,bot, chatId);
                        chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
                        sendMessageByDzhuraevGrigory(chatId, "A_USEBOT: П/У :"+id_id );
                        bot.execute(new SendMessage(chatId, "Рад был помочь! Жду новую команду! "));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        bot.execute(new SendMessage(chatId, "Неверный ввод!"));
                        bot.execute(new SendMessage(chatId, "Повторите попытку!"));
                    }


                } catch (NumberFormatException e) {
                    // Обработайте случай, когда ввод не является допустимым числом
                    bot.execute(new SendMessage(chatId, "Неверный формат ID. Введите число."));
                }
                break;


            /* Перезагрузка сервера */
            case WAITING_FOR_REBOOT_SERVER:

                try {
                    if (text.equals("Назад")) {
                        chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
                        bot.execute(new SendMessage(chatId, "Вы вернулись в админ панель! Жду новую команду!"));
                        break;
                    }
                    // Разберите ввод как число
                    int id = Integer.parseInt(text);

                    if (id == 404) {


                        chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);

                    }

                } catch (NumberFormatException e) {
                    bot.execute(new SendMessage(chatId, "Неверный формат ID. Введите число."));
                }
                break;




            /* Узнать ID сотрудника */
            case WAITING_FOR_MY_ID:

                if(text.equals("Отмена")){
                    chatStates.put(chatId, BotState.WAITING_FOR_COMMAND);
                    bot.execute(new SendMessage(chatId, "Вы вернулись в главное меню!").replyMarkup(createBaseKeyboard(chatId)));
                    break;
                }else {


                    String name_idd = text;
                    sendMessageByDzhuraevGrigory(chatId, "A_USEBOT: Узнать ID : "+ name_idd);

                    bot.execute(new SendMessage(chatId, findUserIdByName(name_idd)));
                    bot.execute(new SendMessage(chatId, "Жду новую команду!").replyMarkup(adminPanelKeyboard()));
                    chatStates.put(chatId, BotState.WAITING_ADMIN_PANEL);
                }
                break;




            default:

                break;


        }

    }


    /** Проверка валидацим дат*/
    public  boolean isValidDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return false;
        }

        String[] parts = dateString.split("/");
        if (parts.length != 2) {
            return false;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            LocalDate startDate = LocalDate.parse(parts[0], formatter);
            LocalDate endDate = LocalDate.parse(parts[1], formatter);

            return startDate.isBefore(endDate) || startDate.isEqual(endDate);
        } catch (DateTimeParseException e) {
            return false;
        }
    }









}
