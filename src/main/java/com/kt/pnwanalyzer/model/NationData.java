package com.kt.pnwanalyzer.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents nation data from the Politics & War API
 */
public class NationData {
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
    
    public NationData() {
    }
    
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
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getNationName() {
        return nationName;
    }
    
    public void setNationName(String nationName) {
        this.nationName = nationName;
    }
    
    public String getLeaderName() {
        return leaderName;
    }
    
    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }
    
    public String getAlliancePosition() {
        return alliancePosition;
    }
    
    public void setAlliancePosition(String alliancePosition) {
        this.alliancePosition = alliancePosition;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public double getScore() {
        return score;
    }
    
    public void setScore(double score) {
        this.score = score;
    }
    
    public int getNumCities() {
        return numCities;
    }
    
    public void setNumCities(int numCities) {
        this.numCities = numCities;
    }
    
    public LocalDateTime getLastActive() {
        return lastActive;
    }
    
    public void setLastActive(LocalDateTime lastActive) {
        this.lastActive = lastActive;
    }
    
    public String getWarPolicy() {
        return warPolicy;
    }
    
    public void setWarPolicy(String warPolicy) {
        this.warPolicy = warPolicy;
    }
    
    public String getDomesticPolicy() {
        return domesticPolicy;
    }
    
    public void setDomesticPolicy(String domesticPolicy) {
        this.domesticPolicy = domesticPolicy;
    }
    
    public int getSoldiers() {
        return soldiers;
    }
    
    public void setSoldiers(int soldiers) {
        this.soldiers = soldiers;
    }
    
    public int getTanks() {
        return tanks;
    }
    
    public void setTanks(int tanks) {
        this.tanks = tanks;
    }
    
    public int getAircraft() {
        return aircraft;
    }
    
    public void setAircraft(int aircraft) {
        this.aircraft = aircraft;
    }
    
    public int getShips() {
        return ships;
    }
    
    public void setShips(int ships) {
        this.ships = ships;
    }
    
    public int getMissiles() {
        return missiles;
    }
    
    public void setMissiles(int missiles) {
        this.missiles = missiles;
    }
    
    public int getNukes() {
        return nukes;
    }
    
    public void setNukes(int nukes) {
        this.nukes = nukes;
    }
    
    public List<Map<String, Object>> getCities() {
        return cities;
    }
    
    public void setCities(List<Map<String, Object>> cities) {
        this.cities = cities;
    }
    
    public Map<String, Boolean> getProjects() {
        return projects;
    }
    
    public void setProjects(Map<String, Boolean> projects) {
        this.projects = projects;
    }
}