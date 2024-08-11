package com.dg.ServerRebornFarmguard.service;

import com.dg.ServerRebornFarmguard.entity.PlaceEntity;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.SendResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.dg.ServerRebornFarmguard.service.TelegramBotService.bot_status;

@Service
@EnableScheduling
public class CheckStatusPlacePlatsService {

    @Autowired
    PlaceService placeService;

    private static final int TIMEOUT_SECONDS = 20;
    private final Map<String, Long> lastRequestTimes = new ConcurrentHashMap<>();
    private final Map<String, Boolean> arduinoStatuses = new ConcurrentHashMap<>();

    public void updateLastRequestTime(String macAddress) {
        lastRequestTimes.put(macAddress, System.currentTimeMillis());
        arduinoStatuses.put(macAddress, true);
    }

    /** */
    @PostConstruct // Инициализация при запуске приложения
    public void initArduinoStatuses() {
        List<PlaceEntity> placeEntityList = placeService.returnAllPlaceEntity();

        for (PlaceEntity placeEntity : placeEntityList) {
            String macAddress = placeEntity.getMacPlace();
            lastRequestTimes.put(macAddress, 0L); // Инициализация с нулевым временем
            arduinoStatuses.put(macAddress, true); // Инициализация с false
        }
    }


    @Scheduled(fixedRate = 2000)
    public void checkArduinoStatus() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : lastRequestTimes.entrySet()) {
            String macAddress = entry.getKey();
            long lastRequestTime = entry.getValue();
            if (currentTime - lastRequestTime > TIMEOUT_SECONDS * 1000) {
                arduinoStatuses.put(macAddress, false);
            }

        }
    }

    /*Закрепленное сообщение*/
    @Scheduled(fixedRate = 10000)
    public void sendMessageByBotStatus() {
        List<PlaceEntity> placeEntityList = placeService.returnAllPlaceEntity();
        List<Long> chatIds = Arrays.asList(667788774L, 929477908L, 5087116051L, 1139708989L, 434612982L, 2057585812L, 453373063L );


        for (Long chatId : chatIds) {
            StringBuilder messageBuilder = new StringBuilder("Статус пунктов:\n\n");

            for (PlaceEntity placeEntity : placeEntityList) {
                String placeName = placeEntity.getNamePlace();
                boolean isOnline = isArduinoOnline(placeEntity.getMacPlace());

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
//                    System.out.println("Кому-то не отрпавлено!");
                }

            }
        }
    }

    Set<String> previousOffline = new HashSet<>();



    @Scheduled(fixedRate = 10000)
    public void sendMessageByBotStatus2() {
        List<PlaceEntity> placeEntityList = placeService.returnAllPlaceEntity();
        List<Long> chatIds = Arrays.asList(667788774L, 929477908L, 5087116051L, 1139708989L, 434612982L, 2057585812L, 453373063L );



            for (PlaceEntity placeEntity : placeEntityList) {
                String macPlace = placeEntity.getMacPlace();
                String placeName = placeEntity.getNamePlace();
                boolean isOnline = isArduinoOnline(macPlace);

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







        private final Map<Long, Integer> chatMessageIds = new ConcurrentHashMap<>();
        private static final String POINT_EMOJI = "\uD83D\uDCCD"; // 📍
        private static final String GREEN_CHECK_EMOJI = "✅";
        private static final String RED_CROSS_EMOJI = "❌";










    public boolean isArduinoOnline(String macAddress) {
        return arduinoStatuses.getOrDefault(macAddress, false);
    }

    private void sendMessageByStatusForSYSADMIN(){

    }

}
