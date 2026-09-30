package com.sukinema.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    // Comprobación de salud para Render: no toca la base de datos
    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
