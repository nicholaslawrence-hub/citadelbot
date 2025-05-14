package com.kt.pnwanalyzer.service;

import java.util.List;
import java.util.Map;

/**
 * Service for analyzing alliances
 */
public interface AllianceService {
    
    /**
     * Get alliance data by ID
     * 
     * @param allianceId Alliance ID
     * @return Alliance data
     */
    Map<String, Object> getAllianceData(int allianceId);
    
    /**
     * Analyze an alliance
     * 
     * @param allianceId Alliance ID
     * @param mmrType MMR type to check (peacetime, raid, wartime)
     * @param includeMmrCheck Whether to include MMR compliance check
     * @param includeResourceAnalysis Whether to include resource analysis
     * @param includeFinancialAnalysis Whether to include financial analysis
     * @param includeCityRecommendations Whether to include city recommendations
     * @param skipCache Whether to skip cache and fetch fresh data
     * @return Analysis results
     */
    Map<String, Object> analyzeAlliance(
            int allianceId, 
            String mmrType, 
            boolean includeMmrCheck,
            boolean includeResourceAnalysis,
            boolean includeFinancialAnalysis,
            boolean includeCityRecommendations,
            boolean skipCache);
    
    /**
     * Get resource analysis for an alliance
     * 
     * @param allianceId Alliance ID
     * @return Resource analysis data
     */
    List<Map<String, Object>> getResourceAnalysis(int allianceId);
    
    /**
     * Get military breakdown for an alliance
     * 
     * @param allianceId Alliance ID
     * @return Military breakdown data
     */
    List<Map<String, Object>> getMilitaryBreakdown(int allianceId);
}