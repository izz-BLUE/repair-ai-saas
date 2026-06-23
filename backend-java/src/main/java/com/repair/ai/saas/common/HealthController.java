package com.repair.ai.saas.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        log.debug("Health check requested");
        return Map.of("status", "UP", "service", "repair-ai-saas-backend");
    }
}
