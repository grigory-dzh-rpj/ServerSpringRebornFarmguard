package com.dg.ServerRebornFarmguard.controller;


import com.dg.ServerRebornFarmguard.config.KafkaConsumer;
import com.dg.ServerRebornFarmguard.service.reports.excel.LearnKafka;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/kafka")
public class KafkaController {

//    @Autowired
//    LearnKafka learnKafka;
//
//    @PostMapping("/hello")
//    public String helloKafka(){
//        learnKafka.sendHello();
//        return "Топик пополнен";
//    }
//
//    @PostMapping("/m")
//    public String message(@RequestBody String string){
//
//        learnKafka.sendMessage(string);
//        learnKafka.sendHello();
//        return "Топик пополнен : "+ string;
//    }





}
