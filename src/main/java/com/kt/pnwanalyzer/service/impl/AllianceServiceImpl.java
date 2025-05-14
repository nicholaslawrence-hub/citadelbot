package com.kt.pnwanalyzer.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.pnwanalyzer.analysis.NationOptimizer;
import com.kt.pnwanalyzer.data.PnWDataFetcher;
import com.kt.pnwanalyzer.service.AllianceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AllianceServiceImpl implements AllianceService {

    private static final Logger logger = LoggerFactory.getLogger(AllianceServiceImpl.class);
    
    @Autowired
    private PnWDataFetcher dataFetcher;
    
    @Autowired
    private NationOptimizer nationOptimizer;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public Map<String, Object> getAllianceData(int allianceId) {
        return dataFetcher.getAllianceData(allianceId);
    }
    
    @Override
    public Map<String, Object> analyzeAlliance(
            int allianceId, 
            String mmrType, 
            boolean includeMmrCheck,
            boolean includeResourceAnalysis,
            boolean includeFinancialAnalysis,
            boolean includeCityRecommendations,
            boolean skipCache) {
        
        logger.info("Analyzing alliance: {}, MMR Type: {}", allianceId, mmrType);
        
        // Get alliance data - includes nations and resource prices
        Map<String, Object> allianceData = dataFetcher.getAllDataForAnalysis(allianceId);
        
        // Check if alliance data was found
        if (allianceData.isEmpty()) {
            return new HashMap<>();
        }
        
        JsonNode allianceApiData = (JsonNode) allianceData.get("alliance");
        List<JsonNode> nationsList = (List<JsonNode>) allianceData.get("nations");
        Map<String, Double> resourcePrices = (Map<String, Double>) allianceData.get("resourcePrices");
        
        if (allianceApiData == null || nationsList == null || nationsList.isEmpty()) {
            logger.warn("No alliance data or nations found for ID: {}", allianceId);
            return Map.of("alliance", allianceApiData, "nations", List.of());
        }
        
        // Initialize result data structures
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> nationAnalyses = new ArrayList<>();
        
        // Alliance-wide statistics
        int totalCities = 0;
        double totalInfra = 0;
        double totalRevenue = 0;
        int mmrCompliantCount = 0;
        double totalMmrUpgradeCost = 0;
        Map<String, Double> totalResourceProduction = new HashMap<>();
        Map<String, Double> totalResourceUsage = new HashMap<>();
        Map<String, Integer> totalMilitaryUnits = new HashMap<>();
        double totalMilitaryUpkeep = 0;
        
        // Process each nation
        for (JsonNode nationNode : nationsList) {
            // Convert JsonNode to Map
            Map<String, Object> nationData = convertJsonNodeToMap(nationNode);
            
            // Analyze the nation
            Map<String, Object> nationAnalysis = nationOptimizer.analyzeNation(nationData, resourcePrices, mmrType);
            
            if (!nationAnalysis.isEmpty()) {
                nationAnalyses.add(nationAnalysis);
                
                // Aggregate alliance-wide statistics
                totalCities += getIntValue(nationAnalysis, "num_cities", 0);
                totalInfra += getDoubleValue(nationAnalysis, "total_infra", 0.0);
                totalRevenue += getDoubleValue(nationAnalysis, "overall_national_revenue", 0.0);
                
                if (includeMmrCheck) {
                    boolean nationMmrCompliant = getBooleanValue(nationAnalysis, "nation_mmr_compliant", false);
                    if (nationMmrCompliant) {
                        mmrCompliantCount++;
                    }
                    totalMmrUpgradeCost += getDoubleValue(nationAnalysis, "total_mmr_upgrade_cost", 0.0);
                }
                
                if (includeResourceAnalysis) {
                    // Aggregate resource data
                    aggregateResources(totalResourceProduction, 
                                      (Map<String, Double>) nationAnalysis.getOrDefault("national_resource_production", new HashMap<>()));
                    
                    aggregateResources(totalResourceUsage, 
                                      (Map<String, Double>) nationAnalysis.getOrDefault("national_resource_usage", new HashMap<>()));
                }
                
                // Aggregate military data
                Map<String, Integer> nationMilitaryUnits = (Map<String, Integer>) nationAnalysis.getOrDefault("military_units", new HashMap<>());
                for (Map.Entry<String, Integer> entry : nationMilitaryUnits.entrySet()) {
                    totalMilitaryUnits.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
                
                totalMilitaryUpkeep += getDoubleValue(nationAnalysis, "national_military_upkeep", 0.0);
            }
        }
        
        // Calculate averages
        double avgInfra = totalCities > 0 ? totalInfra / totalCities : 0;
        int mmrCompliantPercentage = nationAnalyses.size() > 0 ? (mmrCompliantCount * 100) / nationAnalyses.size() : 0;
        
        // Add alliance data to result
        result.put("alliance", allianceApiData);
        result.put("nations", nationAnalyses);
        result.put("mmrType", mmrType);
        
        // Add statistics to result
        result.put("totalCities", totalCities);
        result.put("totalInfra", totalInfra);
        result.put("avgInfra", avgInfra);
        result.put("totalRevenue", totalRevenue);
        
        // Add MMR check results if requested
        if (includeMmrCheck) {
            result.put("mmrCompliantCount", mmrCompliantCount);
            result.put("mmrCompliantPercentage", mmrCompliantPercentage);
            result.put("totalMmrUpgradeCost", totalMmrUpgradeCost);
        }
        
        // Add resource analysis if requested
        if (includeResourceAnalysis) {
            List<Map<String, Object>> resourceAnalysis = formatResourceAnalysis(totalResourceProduction, 
                                                                               totalResourceUsage, 
                                                                               resourcePrices);
            result.put("resourceAnalysis", resourceAnalysis);
            
            double totalResourceValue = calculateTotalResourceValue(totalResourceProduction, 
                                                                   totalResourceUsage, 
                                                                   resourcePrices);
            result.put("totalResourceValue", totalResourceValue);
        }
        
        // Add military breakdown
        List<Map<String, Object>> militaryBreakdown = formatMilitaryBreakdown(totalMilitaryUnits);
        result.put("militaryBreakdown", militaryBreakdown);
        result.put("totalMilitaryUpkeep", totalMilitaryUpkeep);
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getResourceAnalysis(int allianceId) {
        // Get alliance data
        Map<String, Object> allianceData = dataFetcher.getAllDataForAnalysis(allianceId);
        
        // Check if alliance data was found
        if (allianceData.isEmpty()) {
            return List.of();
        }
        
        List<JsonNode> nationsList = (List<JsonNode>) allianceData.get("nations");
        Map<String, Double> resourcePrices = (Map<String, Double>) allianceData.get("resourcePrices");
        
        if (nationsList == null || nationsList.isEmpty()) {
            return List.of();
        }
        
        // Initialize total resource maps
        Map<String, Double> totalResourceProduction = new HashMap<>();
        Map<String, Double> totalResourceUsage = new HashMap<>();
        
        // Process each nation
        for (JsonNode nationNode : nationsList) {
            Map<String, Object> nationData = convertJsonNodeToMap(nationNode);
            Map<String, Object> nationAnalysis = nationOptimizer.analyzeNation(nationData, resourcePrices, "peacetime");
            
            if (!nationAnalysis.isEmpty()) {
                // Aggregate resource data
                aggregateResources(totalResourceProduction, 
                                  (Map<String, Double>) nationAnalysis.getOrDefault("national_resource_production", new HashMap<>()));
                
                aggregateResources(totalResourceUsage, 
                                  (Map<String, Double>) nationAnalysis.getOrDefault("national_resource_usage", new HashMap<>()));
            }
        }
        
        // Format and return resource analysis
        return formatResourceAnalysis(totalResourceProduction, totalResourceUsage, resourcePrices);
    }
    
    @Override
    public List<Map<String, Object>> getMilitaryBreakdown(int allianceId) {
        // Get alliance data
        Map<String, Object> allianceData = dataFetcher.getAllDataForAnalysis(allianceId);
        
        if (allianceData.isEmpty()) {
            return List.of();
        }
        
        List<JsonNode> nationsList = (List<JsonNode>) allianceData.get("nations");
        Map<String, Double> resourcePrices = (Map<String, Double>) allianceData.get("resourcePrices");
        
        if (nationsList == null || nationsList.isEmpty()) {
            return List.of();
        }
        
        // Initialize total military units map
        Map<String, Integer> totalMilitaryUnits = new HashMap<>();
        
        // Process each nation
        for (JsonNode nationNode : nationsList) {
            Map<String, Object> nationData = convertJsonNodeToMap(nationNode);
            Map<String, Object> nationAnalysis = nationOptimizer.analyzeNation(nationData, resourcePrices, "peacetime");
            
            if (!nationAnalysis.isEmpty()) {
                // Aggregate military units
                Map<String, Integer> nationMilitaryUnits = (Map<String, Integer>) nationAnalysis.getOrDefault("military_units", new HashMap<>());
                for (Map.Entry<String, Integer> entry : nationMilitaryUnits.entrySet()) {
                    totalMilitaryUnits.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }
        
        // Format and return military breakdown
        return formatMilitaryBreakdown(totalMilitaryUnits);
    }
    
    // Helper methods
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertJsonNodeToMap(JsonNode jsonNode) {
        try {
            // Use Jackson's ObjectMapper to convert JsonNode to Map
            return objectMapper.convertValue(jsonNode, Map.class);
        } catch (Exception e) {
            logger.error("Error converting JsonNode to Map", e);
            return new HashMap<>();
        }
    }
    
    private void aggregateResources(Map<String, Double> totalResources, Map<String, Double> nationResources) {
        for (Map.Entry<String, Double> entry : nationResources.entrySet()) {
            totalResources.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
    }
    
    private List<Map<String, Object>> formatResourceAnalysis(
            Map<String, Double> production, 
            Map<String, Double> usage, 
            Map<String, Double> prices) {
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        // Process each resource type
        for (String resourceType : production.keySet()) {
            double prodAmount = production.getOrDefault(resourceType, 0.0);
            double useAmount = usage.getOrDefault(resourceType, 0.0);
            double netAmount = prodAmount - useAmount;
            double price = prices.getOrDefault(resourceType, 0.0);
            double value = netAmount * price;
            
            Map<String, Object> resourceInfo = new HashMap<>();
            resourceInfo.put("name", resourceType);
            resourceInfo.put("production", prodAmount);
            resourceInfo.put("usage", useAmount);
            resourceInfo.put("net", netAmount);
            resourceInfo.put("price", price);
            resourceInfo.put("value", value);
            
            result.add(resourceInfo);
        }
        
        // Check for resources that are only in usage
        for (String resourceType : usage.keySet()) {
            if (!production.containsKey(resourceType)) {
                double useAmount = usage.get(resourceType);
                double price = prices.getOrDefault(resourceType, 0.0);
                double value = -useAmount * price;
                
                Map<String, Object> resourceInfo = new HashMap<>();
                resourceInfo.put("name", resourceType);
                resourceInfo.put("production", 0.0);
                resourceInfo.put("usage", useAmount);
                resourceInfo.put("net", -useAmount);
                resourceInfo.put("price", price);
                resourceInfo.put("value", value);
                
                result.add(resourceInfo);
            }
        }
        
        // Sort by value (highest to lowest)
        result.sort((a, b) -> Double.compare((Double) b.get("value"), (Double) a.get("value")));
        
        return result;
    }
    
    private double calculateTotalResourceValue(
            Map<String, Double> production, 
            Map<String, Double> usage, 
            Map<String, Double> prices) {
        
        double totalValue = 0.0;
        
        // Calculate value for each resource
        for (String resourceType : prices.keySet()) {
            double prodAmount = production.getOrDefault(resourceType, 0.0);
            double useAmount = usage.getOrDefault(resourceType, 0.0);
            double netAmount = prodAmount - useAmount;
            double price = prices.get(resourceType);
            
            totalValue += netAmount * price;
        }
        
        return totalValue;
    }
    
    private List<Map<String, Object>> formatMilitaryBreakdown(Map<String, Integer> militaryUnits) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        Map<String, Double> unitUpkeep = com.kt.pnwanalyzer.model.GameConstants.MILITARY_UNIT_UPKEEP;
        
        // Format each unit type
        for (Map.Entry<String, Integer> entry : militaryUnits.entrySet()) {
            String unitType = entry.getKey();
            int count = entry.getValue();
            
            // Skip missiles and nukes as they don't have upkeep
            if (unitType.equals("missiles") || unitType.equals("nukes")) {
                continue;
            }
            
            double upkeepPerUnit = unitUpkeep.getOrDefault(unitType, 0.0);
            double totalUpkeep = count * upkeepPerUnit;
            
            Map<String, Object> unitInfo = new HashMap<>();
            unitInfo.put("type", capitalizeFirstLetter(unitType));
            unitInfo.put("count", count);
            unitInfo.put("upkeep", totalUpkeep);
            
            result.add(unitInfo);
        }
        
        result.sort((a, b) -> Double.compare((Double) b.get("upkeep"), (Double) a.get("upkeep")));
        
        return result;
    }
    
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
    
    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    private boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}