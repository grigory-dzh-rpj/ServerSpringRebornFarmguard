package com.dg.ServerRebornFarmguard.controller;

import com.dg.ServerRebornFarmguard.service.CheckStatusPlacePlatsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/check")
@Tag(name = "CheckStatusPlacePlatsController", description = "Контроллер для проверки статуса устройств по MAC-адресу")
public class CheckStatusPlacePlatsController {

    private final CheckStatusPlacePlatsService service;

    public CheckStatusPlacePlatsController(CheckStatusPlacePlatsService service) {
        this.service = service;
    }

    @PostMapping("/a")
    @Operation(summary = "Обновить время последнего запроса от устройства",
            description = "Контрольки пингуют сервер, чтобы следить за их статусом.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Время последнего запроса успешно обновлено"),
            @ApiResponse(responseCode = "400", description = "Некорректный формат MAC-адреса"),
            @ApiResponse(responseCode = "500", description = "Ошибка при обновлении времени")
    })
    public void handleArduinoRequest(@RequestBody String macAddress) {
        service.updateLastRequestTime(macAddress);
    }


//    http://localhost:7778/check/status/1
    @GetMapping("/status/{macAddress}")
    @Operation(summary = "Получить статус устройства по MAC-адресу",
            description = "Возвращает статус, указывающий, онлайн ли устройство, по его MAC-адресу.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статус устройства успешно получен",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Устройство с указанным MAC-адресом не найдено"),
            @ApiResponse(responseCode = "500", description = "Ошибка при проверке статуса устройства")
    })
    public ResponseEntity<Boolean> getStatusByMacAddress(@PathVariable String macAddress) {
        boolean status = service.isEspOnline(macAddress);
        return ResponseEntity.ok(status);
    }

}
