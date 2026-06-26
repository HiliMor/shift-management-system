package com.hilimor.shiftmanagement.health;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("UP", Instant.now());
    }

    public record HealthResponse(String status, Instant timestamp) {
    }
}
