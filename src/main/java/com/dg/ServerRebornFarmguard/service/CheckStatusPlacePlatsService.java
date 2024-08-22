package com.dg.ServerRebornFarmguard.service;

import com.dg.ServerRebornFarmguard.entity.PlaceEntity;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.PinChatMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.dg.ServerRebornFarmguard.service.TelegramBotService.bot_status;

@Service
@EnableScheduling
public class CheckStatusPlacePlatsService {

    @Autowired
    public PlaceService placeService;

    private static final int TIMEOUT_SECONDS = 20;
    private final Map<String, Long> lastRequestTimes = new ConcurrentHashMap<>();
    private final Map<String, Boolean> espStatuses = new ConcurrentHashMap<>();
    private final Map<Long, Integer> chatMessageIds = new ConcurrentHashMap<>();
    private static final String POINT_EMOJI = "\uD83D\uDCCD"; // 📍
    private static final String GREEN_CHECK_EMOJI = "✅";
    private static final String RED_CROSS_EMOJI = "❌";

    public void updateLastRequestTime(String macAddress) {
        lastRequestTimes.put(macAddress, System.currentTimeMillis());
        espStatuses.put(macAddress, true);
    }

    /** */
    @PostConstruct // Инициализация при запуске приложения
    public void initEspStatuses() {
        List<PlaceEntity> placeEntityList = placeService.returnAllPlaceEntity();

        for (PlaceEntity placeEntity : placeEntityList) {
            String macAddress = placeEntity.getMacPlace();
            lastRequestTimes.put(macAddress, 0L); // Инициализация с нулевым временем
            espStatuses.put(macAddress, true); // Инициализация с false
        }
    }


    @Scheduled(fixedRate = 2000)
    public void checkEspStatus() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : lastRequestTimes.entrySet()) {
            String macAddress = entry.getKey();
            long lastRequestTime = entry.getValue();
            if (currentTime - lastRequestTime > TIMEOUT_SECONDS * 1000) {
                espStatuses.put(macAddress, false);
            }

        }
    }

    /*Закрепленное сообщение*/
    @Scheduled(fixedRate = 10000)
    public void sendMessageByBotStatus() {
        List<PlaceEntity> placeEntityList = placeService.returnAllPlaceEntity();

        List<Long> chatIds = listSysAdmins();
        System.out.println(chatIds);
        for (Long chatId : chatIds) {
            StringBuilder messageBuilder = new StringBuilder("Статус пунктов:\n\n");

            for (PlaceEntity placeEntity : placeEntityList) {
                String placeName = placeEntity.getNamePlace();
                boolean isOnline = isEspOnline(placeEntity.getMacPlace());

                messageBuilder.append(POINT_EMOJI)
                        .append(" ")
                        .append(placeName)
                        .append(" -> ")
                        .append(isOnline ? GREEN_CHECK_EMOJI + " Связь есть" : RED_CROSS_EMOJI + " Обрыв связи")
                        .append("\n");
            }

            String message = messageBuilder.toString();

            if (chatMessageIds.containsKey(chatId)) {
                // Обновляем существующее сообщение
                EditMessageText editMessage = new EditMessageText(
                        chatId.toString(),
                        chatMessageIds.get(chatId),
                        message
                );

                bot_status.execute(editMessage);

            } else {
                // Отправляем новое сообщение только если его еще нет
                SendMessage sendMessage = new SendMessage(chatId.toString(), message);
                SendResponse response = null;
                try {
                    response = bot_status.execute(sendMessage);
                    int messageId = response.message().messageId();
                    chatMessageIds.put(chatId, messageId);
                    PinChatMessage pinChatMessage = new PinChatMessage(chatId.toString(), messageId);
                    bot_status.execute(pinChatMessage);
                }catch (NullPointerException e){
                    System.out.println("Кому-то не отрпавлено!");
                }

            }
        }
    }

    Set<String> previousOffline = new HashSet<>();



    @Scheduled(fixedRate = 10000)
    public void sendMessageByBotStatus2() {
        List<PlaceEntity> placeEntityList = placeService.returnAllPlaceEntity();
            List<Long> chatIds = listSysAdmins();



            for (PlaceEntity placeEntity : placeEntityList) {
                String macPlace = placeEntity.getMacPlace();
                String placeName = placeEntity.getNamePlace();
                boolean isOnline = isEspOnline(macPlace);

                if (!isOnline) {
                    if (!previousOffline.contains(macPlace)) {
                        // Связь потеряна
                        for (Long chatId : chatIds) {
                            String message = "❌ Потеря связи с КП -> " + placeName;
                            sendMessage(chatId, message);
                        }
                        previousOffline.add(macPlace);
                    }
                } else {
                    if (previousOffline.contains(macPlace)) {
                        // Связь восстановлена
                        for (Long chatId : chatIds) {
                            String message = "✅ Связь с КП -> " + placeName + " восстановлена!";
                            sendMessage(chatId, message);
                        }
                        previousOffline.remove(macPlace);
                    }
                }
            }

    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        SendResponse sendResponse = bot_status.execute(message);
        if (sendResponse != null && sendResponse.isOk()) {
//            System.out.println("Message sent successfully to chat " + chatId);
        } else {
            System.out.println("Failed to send message to chat " + chatId);
            if (sendResponse != null) {
                System.out.println("Error code: " + sendResponse.errorCode() + ", Description: " + sendResponse.description());
            }
        }
    }





    public boolean isEspOnline(String macAddress) {
        return espStatuses.getOrDefault(macAddress, false);
    }


    public List<Long> listSysAdmins(){
        List<Long> chatIds;
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader("/Users/Spring Projects/Beckend Ferma Work Project/SpringFarmguard/src/main/resources/arrayChatIdForStatusBot.txt"))) {
            chatIds = bufferedReader.lines().map(String::trim).map(Long::parseLong).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(chatIds);
        return chatIds;
    }


}
