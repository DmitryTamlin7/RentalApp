package org.example.controller;

import org.example.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private HealthService healthService;

    @GetMapping
    public HealthService.HealthResponse checkHealth() {
        return healthService.getHealthStatus();
    }

    @GetMapping("/simple")
    public String simpleHealth() {
        return "OK - " + java.time.LocalDateTime.now();
    }
}