package com.review.system.observability;

import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HealthSummaryBuilder {

    private final HealthEndpoint healthEndpoint;

    public HealthSummaryBuilder(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    public Status status() {
        return healthEndpoint.health().getStatus();
    }

    public Map<String, Object> componentSummary() {
        HealthComponent health = healthEndpoint.health();
        Map<String, Object> components = new LinkedHashMap<>();
        if (health instanceof CompositeHealth composite) {
            composite.getComponents().forEach((name, component) -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("status", component.getStatus().getCode());
                if (component instanceof org.springframework.boot.actuate.health.Health h) {
                    h.getDetails().forEach((key, value) -> {
                        if (isPublicDetail(key, value)) {
                            item.put(key, value);
                        }
                    });
                }
                components.put(name, item);
            });
        }
        return components;
    }

    private boolean isPublicDetail(String key, Object value) {
        if ("message".equals(key) || "mode".equals(key) || "provider".equals(key) || "configured".equals(key)) {
            return true;
        }
        return "host".equals(key) && value != null && !value.toString().contains("@");
    }
}
