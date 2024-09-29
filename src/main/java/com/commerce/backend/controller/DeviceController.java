package com.commerce.backend.controller;


import ch.qos.logback.core.net.server.Client;
import com.commerce.backend.dto.WriteDeviceDto;
import com.commerce.backend.entity.Device;
import com.commerce.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final UserService userService;

    @Autowired
    public DeviceController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("")
    public ResponseEntity<List<Device>> getDevices() {
        return ResponseEntity.ok(userService.getDevices());
    }

    @PostMapping("")
    public ResponseEntity<Device> addDevice(@RequestBody WriteDeviceDto writeDeviceDto) {
        return ResponseEntity.ok(userService.addDevice(writeDeviceDto));
    }
}