package com.example.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {
    
    private final JdbcTemplate jdbcTemplate;
    
    @GetMapping("/db")
    public Map<String, Object> checkDatabase() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test database connection
            Integer count = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            result.put("status", "OK");
            result.put("database", "Connected");
            result.put("test_query", count);
            
            log.info("Database connection test: SUCCESS");
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("database", "Failed");
            result.put("error", e.getMessage());
            
            log.error("Database connection test: FAILED", e);
        }
        
        return result;
    }
    
    @GetMapping("/env")
    public Map<String, String> checkEnvironment() {
        Map<String, String> env = new HashMap<>();
        
        env.put("DB_HOST", System.getenv("DB_HOST"));
        env.put("DB_PORT", System.getenv("DB_PORT"));
        env.put("DB_NAME", System.getenv("DB_NAME"));
        env.put("DB_USER", System.getenv("DB_USER"));
        env.put("DB_PASSWORD", System.getenv("DB_PASSWORD") != null ? "***SET***" : "NOT SET");
        env.put("SPRING_PROFILES_ACTIVE", System.getenv("SPRING_PROFILES_ACTIVE"));
        
        return env;
    }
}
