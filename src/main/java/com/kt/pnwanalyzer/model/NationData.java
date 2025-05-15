package com.kt.pnwanalyzer.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents nation data from the Politics & War API
 */
@Setter
@Getter
public class NationData {
    // Getters and setters
    private int id;
    private String nationName;
    private String leaderName;
    private String alliancePosition;
    private String color;
    private double score;
    private int numCities;
    private LocalDateTime lastActive;
    private String warPolicy;
    private String domesticPolicy;
    private int soldiers;
    private int tanks;
    private int aircraft;
    private int ships;
    private int missiles;
    private int nukes;
    private List<Map<String, Object>> cities;
    private Map<String, Boolean> projects;

    
    public NationData(Map<String, Object> apiData) {
        this.id = ((Number) apiData.getOrDefault("id", 0)).intValue();
        this.nationName = (String) apiData.getOrDefault("nation_name", "");
        this.leaderName = (String) apiData.getOrDefault("leader_name", "");
        this.alliancePosition = (String) apiData.getOrDefault("alliance_position", "");
        this.color = (String) apiData.getOrDefault("color", "");
        this.score = ((Number) apiData.getOrDefault("score", 0)).doubleValue();
        this.numCities = ((Number) apiData.getOrDefault("num_cities", 0)).intValue();
        this.warPolicy = (String) apiData.getOrDefault("war_policy", "");
        this.domesticPolicy = (String) apiData.getOrDefault("domestic_policy", "");
        this.soldiers = ((Number) apiData.getOrDefault("soldiers", 0)).intValue();
        this.tanks = ((Number) apiData.getOrDefault("tanks", 0)).intValue();
        this.aircraft = ((Number) apiData.getOrDefault("aircraft", 0)).intValue();
        this.ships = ((Number) apiData.getOrDefault("ships", 0)).intValue();
        this.missiles = ((Number) apiData.getOrDefault("missiles", 0)).intValue();
        this.nukes = ((Number) apiData.getOrDefault("nukes", 0)).intValue();
        
        // Parse lastActive if present
        Object lastActiveObj = apiData.get("last_active");
        if (lastActiveObj instanceof String) {
            try {
                this.lastActive = LocalDateTime.parse((String) lastActiveObj);
            } catch (Exception e) {
                // Handle date parsing error
            }
        }
    }
}