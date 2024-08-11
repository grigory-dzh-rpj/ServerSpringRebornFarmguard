package com.dg.ServerRebornFarmguard.exception;

import org.springframework.http.ResponseEntity;

public class ExceptionHttp {

    public static ResponseEntity MainExeption(){

        return ResponseEntity.badRequest().body("Ошибка на сервере");
    }
    public static Exception ResourceNotFoundException(){
        return new Exception("Ресурсы не найдены");
    }
}
