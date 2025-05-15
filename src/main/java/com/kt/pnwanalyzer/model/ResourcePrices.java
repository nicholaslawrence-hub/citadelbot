package com.kt.pnwanalyzer.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ResourcePrices {
    private LocalDateTime date;
    private Map<String, Double> prices;
    
    public ResourcePrices(Map<String, Object> apiData) {
        this.prices = new HashMap<>();
        
        Object dateObj = apiData.get("date");
        if (dateObj instanceof String) {
            try {
                this.date = LocalDateTime.parse((String) dateObj);
            } catch (Exception e) {
            }
        }
        for (Map.Entry<String, Object> entry : apiData.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("date") && entry.getValue() instanceof Number) {
                prices.put(key, ((Number) entry.getValue()).doubleValue());
            }
        }
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public void setDate(String date) {
        try {
            this.date = LocalDateTime.parse(date);
        } catch (Exception e) {
            // Handle date parsing error
        }
    }

    public Double getPrice(String resource) {
        return prices.getOrDefault(resource, 0.0);
    }
    
    public void setPrice(String resource, Double price) {
        prices.put(resource, price);
    }
}