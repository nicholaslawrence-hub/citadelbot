package com.kt.pnwanalyzer.service;

import java.util.List;
import java.util.Map;

/**
 * Service for analyzing nations
 */
public interface NationService {
    
    /**
     * Get nation data by ID
     * 
     * @param nationId Nation ID
     * @return Nation data
     */
    Map<String, Object> getNationData(int nationId);
    
    /**
     * Analyze a nation
     * 
     * @param nationId Nation ID
     * @param mmrType MMR type to check (peacetime, raid, wartime)
     * @param optimizationTarget What to optimize for (revenue, food, etc.)
     * @param generateOptimalBuilds Whether to generate optimal builds
     * @param includeResourceAnalysis Whether to include resource analysis
     * @param includeFinancialAnalysis Whether to include financial analysis
     * @param includeProjectRecommendations Whether to include project recommendations
     * @param skipCache Whether to skip cache and fetch fresh data
     * @return Analysis results
     */
    Map<String, Object> analyzeNation(
            int nationId, 
            String mmrType, 
            String optimizationTarget,
            boolean generateOptimalBuilds,
            boolean includeResourceAnalysis,
            boolean includeFinancialAnalysis,
            boolean includeProjectRecommendations,
            boolean skipCache);
    
    /**
     * Get resource analysis for a nation
     * 
     * @param nationId Nation ID
     * @return Resource analysis data
     */
    List<Map<String, Object>> getResourceAnalysis(int nationId);
    
    /**
     * Get city analyses for a nation
     * 
     * @param nationId Nation ID
     * @param mmrType MMR type to check
     * @return City analyses data
     */
    List<Map<String, Object>> getCityAnalyses(int nationId, String mmrType);
    
    /**
     * Generate optimal builds for a nation's cities
     * 
     * @param nationId Nation ID
     * @param mmrType MMR type to use
     * @param optimizationTarget What to optimize for
     * @return Optimal builds for each city
     */
    Map<Integer, Map<String, Object>> generateOptimalBuilds(
            int nationId, 
            String mmrType, 
            String optimizationTarget);
}