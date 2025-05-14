package com.kt.pnwanalyzer.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
                // Handle date parsing error
            }
        }
        for (Map.Entry<String, Object> entry : apiData.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("date") && entry.getValue() instanceof Number) {
                prices.put(key, ((Number) entry.getValue()).doubleValue());
            }
        }
    }
    
    public LocalDateTime getDate() {
        return date;
    }
    
    public void setDate(LocalDateTime date) {
        this.date = date;
    }
    
    public Map<String, Double> getPrices() {
        return prices;
    }
    
    public void setPrices(Map<String, Double> prices) {
        this.prices = prices;
    }
    
    public Double getPrice(String resource) {
        return prices.getOrDefault(resource, 0.0);
    }
    
    public void setPrice(String resource, Double price) {
        prices.put(resource, price);
    }
}