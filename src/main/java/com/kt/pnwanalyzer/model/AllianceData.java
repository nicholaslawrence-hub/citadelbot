package com.kt.pnwanalyzer.model;

import java.util.List;
import java.util.Map;

public class AllianceData {
    private int id;
    private String name;
    private String acronym;
    private String color;
    private double score;
    private List<NationData> nations;
    
    public AllianceData() {
    }
    
    public AllianceData(Map<String, Object> apiData) {
        this.id = ((Number) apiData.getOrDefault("id", 0)).intValue();
        this.name = (String) apiData.getOrDefault("name", "");
        this.acronym = (String) apiData.getOrDefault("acronym", "");
        this.color = (String) apiData.getOrDefault("color", "");
        this.score = ((Number) apiData.getOrDefault("score", 0)).doubleValue();
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAcronym() {
        return acronym;
    }
    
    public void setAcronym(String acronym) {
        this.acronym = acronym;
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
    
    public List<NationData> getNations() {
        return nations;
    }
    
    public void setNations(List<NationData> nations) {
        this.nations = nations;
    }
}