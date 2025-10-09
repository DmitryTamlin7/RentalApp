package com.example.rental.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class HealthService {

    public Map<String, Object> getHealth() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "ok");
        map.put("timestamp", System.currentTimeMillis());
        return map;
    }
}
