package com.dg.ServerRebornFarmguard.controller;

import com.dg.ServerRebornFarmguard.entity.UserEntity;
import com.dg.ServerRebornFarmguard.exception.ExceptionHttp;
import com.dg.ServerRebornFarmguard.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/user")
@Tag(name = "UserController", description = "Контроллер для управления пользователями")
public class UserController {

    @Autowired
    UserService userService;


    /*Лист по должности*/
    @GetMapping("/position")
    @Operation(summary = "Получить пользователей по должности",
            description = "Возвращает список пользователей, которые занимают указанную должность.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список пользователей успешно получен"),
            @ApiResponse(responseCode = "400", description = "Некорректный формат параметров"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при получении данных")
    })
    public ResponseEntity<List<UserEntity>> getUsersByPos(
            @RequestParam (value = "position") @Schema(description = "Должность пользователя", example = "Сотрудник отдела склада") String pos) {
        List<UserEntity> usersPoslist = userService.findByPos(pos);
        try {
            return ResponseEntity.ok(usersPoslist);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }

    @GetMapping("/department")
    @Operation(summary = "Получить пользователей по департаменту",
            description = "Возвращает список пользователей, которые работают в указанном департаменте.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список пользователей успешно получен"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при получении данных")
    })
    public ResponseEntity<List<UserEntity>> getUsersByDep(
            @RequestParam (value = "department")  @Schema(description = "Отдел сотрудника", example = "Отдел ремонта") String dep) {
        List<UserEntity> usersDeplist = userService.findByDepartment(dep);
        try {
            return ResponseEntity.ok(usersDeplist);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }

//http://localhost:7778/user/findById?userid=1
    @GetMapping("/findById")
    @Operation(summary = "Получить пользователя по ID",
            description = "Возвращает пользователя с указанным идентификатором.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно найден"),
            @ApiResponse(responseCode = "404", description = "Пользователь с указанным ID не найден")
    })
    public ResponseEntity<UserEntity> getUsersById(
            @RequestParam (value = "userid") @Schema(description = "ID пользователя", example = "1") Long id) {
        UserEntity userEntity = userService.findByUserId(id);
        try {
            return ResponseEntity.ok(userEntity);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }

    @GetMapping("/findByName")
    @Operation(summary = "Получить пользователя по имени",
            description = "Возвращает пользователя с указанным именем.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно найден"),
            @ApiResponse(responseCode = "404", description = "Пользователь с указанным именем не найден")
    })
    public ResponseEntity<UserEntity> getUsersByName(
            @RequestParam (value = "name") @Schema(description = "Имя пользователя", example = "Иван Иванов") String name) {
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
    @Operation(summary = "Создать пользователя",
            description = "Создает нового пользователя в системе/ Создание пропуска.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные пользователя")
    })
    public ResponseEntity<UserEntity> createUser(@RequestBody @Schema(description = "Данные пользователя") UserEntity user) {
        UserEntity createdUser = userService.createUser(user);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

//    http://localhost:7778/user/delete
    //raw : просто номер id;
    @PostMapping("/delete")
    @Operation(summary = "Удалить пользователя",
            description = "Удаляет пользователя из системы по его ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно удален"),
            @ApiResponse(responseCode = "404", description = "Пользователь с указанным ID не найден")
    })
    public ResponseEntity<String> deleteUser(@RequestBody Long id) {
       try {
           return ResponseEntity.ok(userService.deleteUser(id) + " удален из базы данных!");
       }catch (Exception e){
           return ExceptionHttp.MainExeption();
       }
    }


    @PostMapping("/usersNames")
    @Operation(summary = "Получить все имена пользователей",
            description = "Возвращает список всех имен пользователей в системе.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список имен пользователей успешно получен"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при получении данных")
    })
    public ResponseEntity<List<String>> usersNames() {
        try {
            List<String> list = userService.findAllUserName();
            return ResponseEntity.ok(list);
        }catch (Exception e){
            return ExceptionHttp.MainExeption();
        }
    }


}
