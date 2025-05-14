package com.kt.pnwanalyzer.analysis;

import com.kt.pnwanalyzer.model.GameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class NationOptimizer {
    private static final Logger logger = LoggerFactory.getLogger(NationOptimizer.class);
    
    private final CityAnalyzer cityAnalyzer;
    
    public NationOptimizer() {
        this(new CityAnalyzer());
    }
    
    public NationOptimizer(CityAnalyzer cityAnalyzer) {
        this.cityAnalyzer = cityAnalyzer;
    }
    
    public double calculateNationalMilitaryUpkeep(Map<String, Object> nationApiData, Map<String, Boolean> activeProjects) {
        double baseUpkeep = 
                (getDoubleValue(nationApiData, "soldiers", 0.00001) / 750) * GameConstants.MILITARY_UNIT_UPKEEP.get("soldiers") +
                getDoubleValue(nationApiData, "tanks", 0) * GameConstants.MILITARY_UNIT_UPKEEP.get("tanks") +
                getDoubleValue(nationApiData, "aircraft", 0) * GameConstants.MILITARY_UNIT_UPKEEP.get("aircraft") +
                getDoubleValue(nationApiData, "ships", 0) * GameConstants.MILITARY_UNIT_UPKEEP.get("ships");
        
        String nationWarPolicy = (String) nationApiData.getOrDefault("war_policy", "");
        if ("Imperialism".equals(nationWarPolicy)) {
            Map<String, Object> imperialismPolicySettings = GameConstants.POLICY_EFFECTS.get("Imperialism");
            double upkeepReductionPercentage = ((Number) imperialismPolicySettings.getOrDefault("base_reduction_percentage", 0.0)).doubleValue();
            
            String boostingProject1 = (String) imperialismPolicySettings.get("boosting_project1");
            String boostingProject2 = (String) imperialismPolicySettings.get("boosting_project2");
            
            if ((boostingProject1 != null && Boolean.TRUE.equals(activeProjects.get(boostingProject1))) ||
                (boostingProject2 != null && Boolean.TRUE.equals(activeProjects.get(boostingProject2)))) {
                double boostedValue = ((Number) imperialismPolicySettings.getOrDefault(
                    "project_boost_reduction_percentage", upkeepReductionPercentage)).doubleValue();
                upkeepReductionPercentage = boostedValue;
            }
            
            double upkeepMultiplier = 1.0 - (upkeepReductionPercentage / 100.0);
            return baseUpkeep * upkeepMultiplier;
        }
        
        return baseUpkeep;
    }
    
    /**
     * Analyze a nation and calculate key metrics
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeNation(Map<String, Object> nationApiData, Map<String, Double> resourcePrices, String mmrKeyName) {
        if (nationApiData == null || !nationApiData.containsKey("cities")) {
            return Collections.emptyMap();
        }
        
        logger.info("Analyzing nation: {} (ID: {})", 
                nationApiData.get("nation_name"), 
                nationApiData.get("id"));
        
        Map<String, Boolean> activeProjects = cityAnalyzer.getActiveProjects(nationApiData);
        
        List<Map<String, Object>> cityAnalysisResults = new ArrayList<>();
        
        double nationalBasePopulationIncome = 0.0;
        double nationalOpenMarketsBonus = 0.0;
        double nationalAllianceTreasureBonus = 0.0;
        double nationalColorTradeBlocBonus = 0.0;
        double nationalGrossMonetaryIncome = 0.0;
        double nationalImprovementUpkeep = 0.0;
        double nationalTotalResourceValue = 0.0;
        
        double totalInfra = 0.0;
        double totalLand = 0.0;
        Map<String, Double> nationResourceProductionTotals = new HashMap<>();
        Map<String, Double> nationResourceUsageTotals = new HashMap<>();
        
        List<Map<String, Object>> cities = (List<Map<String, Object>>) nationApiData.get("cities");
        
        for (Map<String, Object> cityApiData : cities) {
            Map<String, Object> cityAnalysisResult = cityAnalyzer.analyzeCity(cityApiData, nationApiData, resourcePrices, mmrKeyName);
            
            if (!cityAnalysisResult.isEmpty()) {
                cityAnalysisResults.add(cityAnalysisResult);
                
                nationalBasePopulationIncome += getDoubleValue(cityAnalysisResult, "base_population_income", 0.0);
                nationalOpenMarketsBonus += getDoubleValue(cityAnalysisResult, "open_markets_bonus_amount", 0.0);
                nationalAllianceTreasureBonus += getDoubleValue(cityAnalysisResult, "alliance_treasure_bonus_amount", 0.0);
                nationalColorTradeBlocBonus += getDoubleValue(cityAnalysisResult, "color_trade_bloc_bonus_placeholder", 0.0);
                nationalGrossMonetaryIncome += getDoubleValue(cityAnalysisResult, "gross_monetary_income", 0.0);
                nationalImprovementUpkeep += getDoubleValue(cityAnalysisResult, "improvement_upkeep", 0.0);
                nationalTotalResourceValue += getDoubleValue(cityAnalysisResult, "total_net_resource_value", 0.0);
                
                totalInfra += getDoubleValue(cityAnalysisResult, "infrastructure", 0.0);
                totalLand += getDoubleValue(cityAnalysisResult, "land", 0.0);
                
                Map<String, Double> cityResourceProduction = (Map<String, Double>) cityAnalysisResult.getOrDefault("resource_production", Collections.emptyMap());
                Map<String, Double> cityResourceUsage = (Map<String, Double>) cityAnalysisResult.getOrDefault("resource_usage", Collections.emptyMap());
                
                for (Map.Entry<String, Double> entry : cityResourceProduction.entrySet()) {
                    nationResourceProductionTotals.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
                
                for (Map.Entry<String, Double> entry : cityResourceUsage.entrySet()) {
                    nationResourceUsageTotals.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
            }
        }
        
        // Calculate military upkeep
        double nationalMilitaryUpkeepVal = calculateNationalMilitaryUpkeep(nationApiData, activeProjects);
        
        // Calculate net income
        double netNationalMonetaryIncomeVal = nationalGrossMonetaryIncome - nationalImprovementUpkeep - nationalMilitaryUpkeepVal;
        
        // Calculate per-city averages
        int numCitiesVal = getIntValue(nationApiData, "num_cities", 0);
        double avgInfraVal = numCitiesVal > 0 ? totalInfra / numCitiesVal : 0;
        double avgLandVal = numCitiesVal > 0 ? totalLand / numCitiesVal : 0;
        
        // Check MMR compliance for all cities
        boolean isNationMmrCompliant = cityAnalysisResults.stream()
                .allMatch(city -> {
                    Map<String, Object> mmrStatus = (Map<String, Object>) city.get("mmr_status");
                    return mmrStatus != null && Boolean.TRUE.equals(mmrStatus.get("compliant"));
                });
        
        double totalMmrUpgradeCostVal = cityAnalysisResults.stream()
                .mapToDouble(city -> {
                    Map<String, Object> mmrStatus = (Map<String, Object>) city.get("mmr_status");
                    if (mmrStatus != null && !Boolean.TRUE.equals(mmrStatus.get("compliant"))) {
                        return getDoubleValue(mmrStatus, "total_cost", 0.0);
                    }
                    return 0.0;
                })
                .sum();
        
        // Prepare project recommendations (placeholder for future implementation)
        List<String> projectRecommendations = new ArrayList<>();
        
        Map<String, Object> result = new HashMap<>();
        
        // Basic nation information
        result.put("nation_id", nationApiData.get("id"));
        result.put("nation_name", nationApiData.get("nation_name"));
        result.put("leader_name", nationApiData.get("leader_name"));
        result.put("score", getDoubleValue(nationApiData, "score", 0));
        result.put("num_cities", numCitiesVal);
        result.put("last_active", nationApiData.get("last_active"));
        result.put("domestic_policy", nationApiData.get("domestic_policy"));
        result.put("war_policy", nationApiData.get("war_policy"));
        result.put("color", nationApiData.get("color"));
        
        // Infrastructure and land stats
        result.put("total_infra", totalInfra);
        result.put("avg_infra_per_city", avgInfraVal);
        result.put("avg_land_per_city", avgLandVal);
        
        // Financial breakdown
        result.put("national_base_population_income", nationalBasePopulationIncome);
        result.put("national_open_markets_bonus_amount", nationalOpenMarketsBonus);
        result.put("national_alliance_treasure_bonus_amount", nationalAllianceTreasureBonus);
        result.put("national_color_trade_bloc_bonus_placeholder", nationalColorTradeBlocBonus);
        result.put("national_gross_monetary_income", nationalGrossMonetaryIncome);
        result.put("national_total_improvement_upkeep", nationalImprovementUpkeep);
        result.put("national_military_upkeep", nationalMilitaryUpkeepVal);
        result.put("national_gross_upkeep", nationalImprovementUpkeep + nationalMilitaryUpkeepVal);
        result.put("net_national_monetary_income", netNationalMonetaryIncomeVal);
        result.put("national_total_resource_value", nationalTotalResourceValue);
        result.put("overall_national_revenue", netNationalMonetaryIncomeVal + nationalTotalResourceValue);
        
        result.put("national_resource_production", nationResourceProductionTotals);
        result.put("national_resource_usage", nationResourceUsageTotals);
        
        // Military units
        Map<String, Integer> militaryUnits = new HashMap<>();
        for (String unitType : Arrays.asList("soldiers", "tanks", "aircraft", "ships", "missiles", "nukes")) {
            militaryUnits.put(unitType, getIntValue(nationApiData, unitType, 0));
        }
        result.put("military_units", militaryUnits);
        
        // MMR compliance and projects
        result.put("nation_mmr_compliant", isNationMmrCompliant);
        result.put("total_mmr_upgrade_cost", totalMmrUpgradeCostVal);
        result.put("projects_status", activeProjects);
        result.put("project_recommendations", projectRecommendations);
        result.put("city_analyses", cityAnalysisResults);
        
        return result;
    }
    
    private static double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    private static int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}