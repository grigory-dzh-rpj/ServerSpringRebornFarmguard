package com.dg.ServerRebornFarmguard.controller;

import com.dg.ServerRebornFarmguard.entity.UserEntity;
import com.dg.ServerRebornFarmguard.exception.ExceptionHttp;
import com.dg.ServerRebornFarmguard.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;


    /*Лист по должности*/
    @GetMapping("/position")
    public ResponseEntity<List<UserEntity>> getUsersByPos(
            @RequestParam (value = "position") String pos) {
        List<UserEntity> usersPoslist = userService.findByPos(pos);
        try {
            return ResponseEntity.ok(usersPoslist);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }

    @GetMapping("/department")
    public ResponseEntity<List<UserEntity>> getUsersByDep(
            @RequestParam (value = "department") String dep) {
        List<UserEntity> usersDeplist = userService.findByDepartment(dep);
        try {
            return ResponseEntity.ok(usersDeplist);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }

//http://localhost:7778/user/findById?userid=1
    @GetMapping("/findById")
    public ResponseEntity<UserEntity> getUsersById(
            @RequestParam (value = "userid") Long id) {
        UserEntity userEntity = userService.findByUserId(id);
        try {
            return ResponseEntity.ok(userEntity);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }

    @GetMapping("/findByName")
    public ResponseEntity<UserEntity> getUsersByName(
            @RequestParam (value = "name") String name) {
        UserEntity userEntity = userService.findByName(name);
        try {
            return ResponseEntity.ok(userEntity);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }




    //    http://localhost:7778/user/create
//    {
//  "iduser": "134567",
//  "name": "Никита Иванов",
//  "position": "Менеджер",
//  "department": "Продажи",
//  "chatId": 1234567890,
//  "tel": "+79991112233",
//  "role": "Администратор",
//  "access": "Полный доступ",
//  "tracking": "true",
//  "personTracking": "true"
//}
    @PostMapping("/create")
    public ResponseEntity<UserEntity> createUser(@RequestBody UserEntity user) {
        UserEntity createdUser = userService.createUser(user);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

//    http://localhost:7778/user/delete
    //raw : просто номер id;
    @PostMapping("/delete")
    public ResponseEntity<String> deleteUser(@RequestBody Long id) {
       try {
           return ResponseEntity.ok(userService.deleteUser(id) + " удален из базы данных!");
       }catch (Exception e){
           return ExceptionHttp.MainExeption();
       }
    }


    @PostMapping("/usersNames")
    public ResponseEntity<List<String>> usersNames() {
        try {
            List<String> list = userService.findAllUserName();
            return ResponseEntity.ok(list);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }


}
