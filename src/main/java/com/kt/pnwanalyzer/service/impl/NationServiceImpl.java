package com.kt.pnwanalyzer.service.impl;

import com.kt.pnwanalyzer.analysis.NationOptimizer;
import com.kt.pnwanalyzer.data.PnWDataFetcher;
import com.kt.pnwanalyzer.service.NationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NationServiceImpl implements NationService {

    private static final Logger logger = LoggerFactory.getLogger(NationServiceImpl.class);
    
    @Autowired
    private PnWDataFetcher dataFetcher;
    
    @Autowired
    private NationOptimizer nationOptimizer;
    
    @Override
    public Map<String, Object> getNationData(int nationId) {
        return convertJsonNodeToMap(dataFetcher.getNationDetails(nationId));
    }
    
    @Override
    public Map<String, Object> analyzeNation(
            int nationId, 
            String mmrType, 
            String optimizationTarget,
            boolean generateOptimalBuilds,
            boolean includeResourceAnalysis,
            boolean includeFinancialAnalysis,
            boolean includeProjectRecommendations,
            boolean skipCache) {
        
        logger.info("Analyzing nation: {}, MMR Type: {}", nationId, mmrType);
        
        // Get nation data
        Map<String, Object> nationData = getNationData(nationId);
        
        // Check if nation data was found
        if (nationData == null || nationData.isEmpty()) {
            return new HashMap<>();
        }
        
        // Get resource prices
        Map<String, Double> resourcePrices = getResourcePrices();
        
        // Use the NationOptimizer to analyze the nation
        Map<String, Object> analysis = nationOptimizer.analyzeNation(nationData, resourcePrices, mmrType);
        
        // Add the nation object for the view
        analysis.put("nation", nationData);
        analysis.put("mmrType", mmrType);
        
        // Get improvement details for displaying in the view
        Map<String, Map<String, Object>> improvementDetails = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : 
             com.kt.pnwanalyzer.model.GameConstants.IMPROVEMENT_DETAILS.entrySet()) {
            improvementDetails.put(entry.getKey(), entry.getValue());
        }
        analysis.put("improvementDetails", improvementDetails);
        
        return analysis;
    }
    
    @Override
    public List<Map<String, Object>> getResourceAnalysis(int nationId) {
        // Get nation data
        Map<String, Object> nationData = getNationData(nationId);
        
        // Check if nation data was found
        if (nationData == null || nationData.isEmpty()) {
            return List.of();
        }
        
        // Get resource prices
        Map<String, Double> resourcePrices = getResourcePrices();
        
        // Use the NationOptimizer to analyze resources
        // In a production app, you'd extract this from the full nation analysis
        Map<String, Object> nationAnalysis = nationOptimizer.analyzeNation(nationData, resourcePrices, "peacetime");
        
        // Extract resource info from the nation analysis
        // This would need to be adapted based on how your NationOptimizer formats the data
        @SuppressWarnings("unchecked")
        Map<String, Double> production = (Map<String, Double>) nationAnalysis.getOrDefault("national_resource_production", new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Double> usage = (Map<String, Double>) nationAnalysis.getOrDefault("national_resource_usage", new HashMap<>());
        
        // Convert to the format expected by the view
        return formatResourceAnalysis(production, usage, resourcePrices);
    }
    
    @Override
    public List<Map<String, Object>> getCityAnalyses(int nationId, String mmrType) {
        // Get nation data
        Map<String, Object> nationData = getNationData(nationId);
        
        // Check if nation data was found
        if (nationData == null || nationData.isEmpty()) {
            return List.of();
        }
        
        // Get city analyses from the nation analysis
        Map<String, Double> resourcePrices = getResourcePrices();
        Map<String, Object> nationAnalysis = nationOptimizer.analyzeNation(nationData, resourcePrices, mmrType);
        
        // Extract city analyses from the nation analysis
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cityAnalyses = (List<Map<String, Object>>) nationAnalysis.getOrDefault("city_analyses", List.of());
        
        return cityAnalyses;
    }
    
    @Override
    public Map<Integer, Map<String, Object>> generateOptimalBuilds(
            int nationId, 
            String mmrType, 
            String optimizationTarget) {
        
        // Get nation data
        Map<String, Object> nationData = getNationData(nationId);
        
        // Check if nation data was found
        if (nationData == null || nationData.isEmpty()) {
            return Map.of();
        }
        
        // Get resource prices
        Map<String, Double> resourcePrices = getResourcePrices();
        
        // Use the NationOptimizer to analyze the nation
        Map<String, Object> nationAnalysis = nationOptimizer.analyzeNation(nationData, resourcePrices, mmrType);
        
        // Get city analyses from the nation analysis
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cityAnalyses = (List<Map<String, Object>>) nationAnalysis.getOrDefault("city_analyses", List.of());
        
        // Generate optimal builds for each city
        Map<Integer, Map<String, Object>> optimalBuilds = new HashMap<>();
        
        for (Map<String, Object> city : cityAnalyses) {
            int cityId = ((Number) city.get("city_id")).intValue();
            double infrastructure = ((Number) city.get("infrastructure")).doubleValue();
            double land = ((Number) city.get("land")).doubleValue();
            
            // Generate optimal build for this city
            Map<String, Object> optimalBuild = generateOptimalBuildForCity(infrastructure, land, nationData, resourcePrices, mmrType);
            
            optimalBuilds.put(cityId, optimalBuild);
        }
        
        return optimalBuilds;
    }
    
    // Helper methods
    
    private Map<String, Object> convertJsonNodeToMap(Object jsonNode) {
        // In a real implementation, this would convert a JsonNode to a Map
        // For now, just creating a sample nation so we have something to work with
        if (jsonNode == null) {
            return new HashMap<>();
        }
        
        // Create a basic map with nation data
        Map<String, Object> nationData = new HashMap<>();
        nationData.put("id", 123);
        nationData.put("nation_name", "Sample Nation");
        nationData.put("leader_name", "Sample Leader");
        nationData.put("alliance_position", "Member");
        nationData.put("color", "Blue");
        nationData.put("score", 5000.0);
        nationData.put("num_cities", 10);
        nationData.put("war_policy", "Attrition");
        nationData.put("domestic_policy", "Open Markets");
        
        // Add some cities
        List<Map<String, Object>> cities = List.of(
            Map.of(
                "id", 1001,
                "name", "Capital City",
                "infrastructure", 1850.0,
                "land", 2100.0,
                "powered", true
            ),
            Map.of(
                "id", 1002,
                "name", "Industrial Hub",
                "infrastructure", 1650.0,
                "land", 1900.0,
                "powered", true
            )
        );
        
        nationData.put("cities", cities);
        
        return nationData;
    }
    
    private Map<String, Double> getResourcePrices() {
        // In a real implementation, this would come from the API
        Map<String, Double> resourcePrices = new HashMap<>();
        resourcePrices.put("coal", 2500.0);
        resourcePrices.put("oil", 3000.0);
        resourcePrices.put("uranium", 5000.0);
        resourcePrices.put("iron", 3000.0);
        resourcePrices.put("bauxite", 2500.0);
        resourcePrices.put("lead", 1800.0);
        resourcePrices.put("gasoline", 3500.0);
        resourcePrices.put("munitions", 3800.0);
        resourcePrices.put("steel", 4000.0);
        resourcePrices.put("aluminum", 3200.0);
        resourcePrices.put("food", 550.0);
        
        return resourcePrices;
    }
    
    private List<Map<String, Object>> formatResourceAnalysis(
            Map<String, Double> production, 
            Map<String, Double> usage, 
            Map<String, Double> prices) {
        
        // In a real implementation, convert the raw data to the format expected by the view
        return List.of();
    }
    
    private Map<String, Object> generateOptimalBuildForCity(
            double infrastructure, 
            double land, 
            Map<String, Object> nationData, 
            Map<String, Double> resourcePrices, 
            String mmrType) {
        
        // Use the existing CityAnalyzer class from the nation optimizer
        // This is a simplified example - in a real app, you would use your CityAnalyzer instance
        return nationOptimizer.getCityAnalyzer().generateOptimalBuild(infrastructure, land, nationData, resourcePrices, mmrType);
    }
}