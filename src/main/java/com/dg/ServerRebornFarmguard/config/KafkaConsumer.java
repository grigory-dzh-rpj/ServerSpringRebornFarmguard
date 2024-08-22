package com.dg.ServerRebornFarmguard.config;

import com.dg.ServerRebornFarmguard.model.ReqDateAndPlace;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

//@Component
//@KafkaListener(topics = "my-topic", groupId = "my-consumer-group")
//@Slf4j
public class KafkaConsumer {

//
//
//    @KafkaHandler
//    public void handler(String message) {
//
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        try {
//            TypeReference<ReqDateAndPlace> typeReference = new TypeReference<>() {};
//            ReqDateAndPlace myObject = objectMapper.readValue(message, typeReference);
//            System.out.println(myObject);
//        } catch (JsonProcessingException e) {
//            log.error("Ошибка : ", e);
//        }
//    }



}