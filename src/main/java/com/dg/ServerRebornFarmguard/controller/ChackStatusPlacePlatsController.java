package com.dg.ServerRebornFarmguard.controller;

import com.dg.ServerRebornFarmguard.service.CheckStatusPlacePlatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/check")
public class ChackStatusPlacePlatsController {


    private final CheckStatusPlacePlatsService service;

    public ChackStatusPlacePlatsController(CheckStatusPlacePlatsService service) {
        this.service = service;
    }

//    http://localhost:7778/check/a
    @PostMapping("/a")
    public void handleArduinoRequest(@RequestBody String macAddress) {
        service.updateLastRequestTime(macAddress);
    }


    //http://localhost:7778/check/status/1
    @GetMapping("/status/{macAddress}")
    public ResponseEntity<Boolean> getStatusByMacAddress(@PathVariable String macAddress) {
        boolean status = service.isArduinoOnline(macAddress);
        return ResponseEntity.ok(status);
    }

}
