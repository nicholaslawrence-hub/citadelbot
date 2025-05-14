package com.kt.pnwanalyzer.service;

import java.util.Map;

/**
 * Service for optimizing city builds
 */
public interface CityService {
    
    /**
     * Generate an optimal city build
     * 
     * @param targetInfra Target infrastructure level
     * @param targetLand Target land amount
     * @param mmrType MMR type to include
     * @param optimizationType What to optimize for
     * @param domesticPolicy Nation's domestic policy
     * @param projects Projects to include
     * @return Optimal build data
     */
    Map<String, Object> generateOptimalBuild(
            double targetInfra,
            double targetLand,
            String mmrType,
            String optimizationType,
            String domesticPolicy,
            String[] projects);
    
    /**
     * Get available projects that could affect city optimization
     * 
     * @return Map of project IDs to names
     */
    Map<String, String> getAvailableProjects();
}