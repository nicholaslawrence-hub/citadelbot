package com.kt.pnwanalyzer.analysis;

import com.kt.pnwanalyzer.model.GameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Analyzes city data and calculates optimal builds
 */
public class CityAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(CityAnalyzer.class);
    
    private final Map<String, Object> gameConstants;
    private final Map<String, Map<String, Object>> improvementDetails;
    private final Map<String, Map<String, Object>> policyEffects;
    private final Map<String, Map<String, Object>> projectEffects;
    private final Map<String, String> projectNameMap;
    
    public CityAnalyzer() {
        this(
            GameConstants.GAME_CONSTANTS,
            GameConstants.IMPROVEMENT_DETAILS,
            GameConstants.POLICY_EFFECTS, 
            GameConstants.PROJECT_EFFECTS
        );
    }
    
    public CityAnalyzer(
            Map<String, Object> gameConstants,
            Map<String, Map<String, Object>> improvementDetails,
            Map<String, Map<String, Object>> policyEffects,
            Map<String, Map<String, Object>> projectEffects) {
        this.gameConstants = gameConstants;
        this.improvementDetails = improvementDetails;
        this.policyEffects = policyEffects;
        this.projectEffects = projectEffects;
        this.projectNameMap = GameConstants.PROJECT_NAME_MAP;
    }
    
    public Map<String, Boolean> getActiveProjects(Map<String, Object> nationApiData) {
        Map<String, Boolean> activeProjects = new HashMap<>();
        
        for (Map.Entry<String, String> entry : projectNameMap.entrySet()) {
            String gqlField = entry.getKey();
            String mappedName = entry.getValue();
            
            if (nationApiData.containsKey(gqlField)) {
                Boolean isActive = Boolean.valueOf(nationApiData.get(gqlField).toString());
                activeProjects.put(mappedName, isActive);
            }
        }
        
        return activeProjects;
    }
    
    private Object getProjectSetting(String projectName, String settingKey, Map<String, Boolean> activeProjects, Object defaultValue) {
        if (Boolean.TRUE.equals(activeProjects.get(projectName))) {
            Map<String, Object> projectSettings = projectEffects.get(projectName);
            if (projectSettings != null && projectSettings.containsKey(settingKey)) {
                return projectSettings.get(settingKey);
            }
        }
        return defaultValue;
    }

    public Map<String, Object> analyzeCity  (
            Map<String, Object> cityApiData, 
            Map<String, Object> nationApiData, 
            Map<String, Double> resourcePrices,
            String mmrKeyName) {
        
        if (cityApiData == null) {
            return new HashMap<>();
        }
        
        String cityName = (String) cityApiData.getOrDefault("name", "Unnamed City");
        int cityId = ((Number) cityApiData.getOrDefault("id", 0)).intValue();
        
        double infra = ((Number) cityApiData.getOrDefault("infrastructure", 0)).doubleValue();
        double land = ((Number) cityApiData.getOrDefault("land", 0)).doubleValue();
        boolean isPowered = (boolean) cityApiData.getOrDefault("powered", false);
        
        Map<String, Integer> cityImprovements = new HashMap<>();
        for (String improvementKey : new String[] {
                "coalpower", "oilpower", "nuclearpower", "windpower", 
                "coalmine", "oilwell", "ironmine", "bauxitemine", "leadmine", "uramine", 
                "farm", "gasrefinery", "steelmill", "aluminumrefinery", "munitionsfactory", 
                "policestation", "hospital", "recyclingcenter", "subway", 
                "supermarket", "bank", "mall", "stadium", 
                "barracks", "factory", "hangar", "drydock"
        }) {
            Object value = cityApiData.get(improvementKey);
            if (value instanceof Number) {
                cityImprovements.put(improvementKey, ((Number) value).intValue());
            }
        }
        
        Map<String, Boolean> activeProjects = getActiveProjects(nationApiData);
        
        Map<String, Object> financialDetails = calculateCityFinancialDetails(
                cityImprovements, infra, land, isPowered, nationApiData, resourcePrices);
        
        Map<String, Object> mmrComplianceResult = checkMmrCompliance(cityImprovements, mmrKeyName);
        
        Map<String, Object> result = new HashMap<>();
        result.put("city_id", cityId);
        result.put("city_name", cityName);
        result.put("infrastructure", infra);
        result.put("land", land);
        result.put("powered", isPowered);
        result.put("improvements", cityImprovements);
        result.put("mmr_status", mmrComplianceResult);
        
        // Add the financial details
        result.putAll(financialDetails);
        
        return result;
    }
    private Map<String, Object> checkMmrCompliance(Map<String, Integer> cityImprovements, String mmrKeyName) {
        Map<String, Object> result = new HashMap<>();
        
        Map<String, Object> mmrRequirements = GameConstants.MMR_TYPES.getOrDefault(
                mmrKeyName, GameConstants.MMR_TYPES.get("peacetime"));
        
        boolean isCompliant = true;
        Map<String, Integer> missingImprovements = new HashMap<>();
        double totalCost = 0.0;
        
        for (Map.Entry<String, Object> entry : mmrRequirements.entrySet()) {
            String impKey = entry.getKey();
            if (!"name".equals(impKey)) {
                int requiredCount = ((Number) entry.getValue()).intValue();
                int actualCount = cityImprovements.getOrDefault(impKey, 0);
                
                if (actualCount < requiredCount) {
                    isCompliant = false;
                    int missingCount = requiredCount - actualCount;
                    missingImprovements.put(impKey, missingCount);
                    
                    Map<String, Object> impDetails = improvementDetails.get(impKey);
                    double impCost = ((Number) impDetails.getOrDefault("cost", 0)).doubleValue();
                    totalCost += impCost * missingCount;
                }
            }
        }
        
        result.put("compliant", isCompliant);
        result.put("mmr_type", mmrRequirements.get("name"));
        result.put("missing_improvements", missingImprovements);
        result.put("total_cost", totalCost);
        
        return result;
    }

    private int getMaxImprovementCount(String improvementKey, Map<String, Boolean> activeProjects) {
        Map<String, Object> improvementConfig = improvementDetails.get(improvementKey);
        if (improvementConfig == null) {
            return 0;
        }
        
        int baseMaxCount = ((Number) improvementConfig.getOrDefault("max", 0)).intValue();
        
        if ("policestation".equals(improvementKey) && Boolean.TRUE.equals(activeProjects.get("Specialized Police Training"))) {
            return baseMaxCount + 1;
        }
        
        for (Map.Entry<String, Map<String, Object>> entry : projectEffects.entrySet()) {
            String projectName = entry.getKey();
            Map<String, Object> projectSettings = entry.getValue();
            
            if (Boolean.TRUE.equals(activeProjects.get(projectName)) && 
                projectSettings.containsKey("max_change")) {
                
                @SuppressWarnings("unchecked")
                Map<String, Integer> maxChanges = (Map<String, Integer>) projectSettings.get("max_change");
                if (maxChanges != null && maxChanges.containsKey(improvementKey)) {
                    return maxChanges.get(improvementKey);
                }
            }
        }
        
        return baseMaxCount;
    }
    

    public double calculatePollutionValue(Map<String, Integer> cityImprovements, Map<String, Boolean> activeProjects, String nationPolicy) {
        double currentPollution = 0.0;
        
        Map<String, Object> farmConfig = improvementDetails.get("farm");
        if (farmConfig != null) {
            int farmCount = cityImprovements.getOrDefault("farm", 0);
            double farmPollution = ((Number) farmConfig.getOrDefault("pollution", 0)).doubleValue();
            currentPollution += farmCount * farmPollution;
        }
        
        // Mine pollution
        String[] mineKeys = {"coalmine", "bauxitemine", "leadmine", "oilwell", "ironmine", "uramine"};
        for (String mineKey : mineKeys) {
            Map<String, Object> mineConfig = improvementDetails.get(mineKey);
            if (mineConfig != null) {
                int mineCount = cityImprovements.getOrDefault(mineKey, 0);
                double minePollution = ((Number) mineConfig.getOrDefault("pollution", 0)).doubleValue();
                currentPollution += mineCount * minePollution;
            }
        }
        
        // Factory pollution with Green Technologies modifier
        double factoryPollutionMultiplier = 1.0;
        Object greenTechMultiplier = getProjectSetting("Green Technologies", "value", activeProjects, null);
        if (greenTechMultiplier != null && 
            "factory_pollution_multiplier".equals(getProjectSetting("Green Technologies", "target_metric", activeProjects, ""))) {
            factoryPollutionMultiplier = ((Number) greenTechMultiplier).doubleValue();
        }
        
        String[] factoryKeys = {"gasrefinery", "steelmill", "aluminumrefinery", "munitionsfactory", "coalpower", "oilpower"};
        for (String factoryKey : factoryKeys) {
            Map<String, Object> factoryConfig = improvementDetails.get(factoryKey);
            if (factoryConfig != null && factoryConfig.containsKey("pollution")) {
                int factoryCount = cityImprovements.getOrDefault(factoryKey, 0);
                double factoryPollution = ((Number) factoryConfig.get("pollution")).doubleValue();
                currentPollution += factoryCount * factoryPollution * factoryPollutionMultiplier;
            }
        }
        
        // Apply policy multiplier if applicable
        Map<String, Object> policySettings = policyEffects.get(nationPolicy);
        if (policySettings != null && "pollution_multiplier".equals(policySettings.get("target_metric"))) {
            double policyMultiplier = ((Number) policySettings.getOrDefault("value", 1.0)).doubleValue();
            currentPollution *= policyMultiplier;
        }
        
        // Subtract pollution reductions
        // Recycling centers
        Map<String, Object> recyclingConfig = improvementDetails.get("recyclingcenter");
        if (recyclingConfig != null) {
            int recyclingCount = cityImprovements.getOrDefault("recyclingcenter", 0);
            double baseReduction = ((Number) recyclingConfig.getOrDefault("pollution_reduction", 0)).doubleValue();
            
            // Green Technologies bonus to recycling
            double recyclingBonus = 0.0;
            Object bonusPoints = getProjectSetting("Green Technologies", "recycling_effectiveness_bonus_points", activeProjects, 0.0);
            if (bonusPoints != null) {
                recyclingBonus = ((Number) bonusPoints).doubleValue();
            }
            
            currentPollution -= recyclingCount * (baseReduction + recyclingBonus);
        }
        
        // Subway pollution reduction
        Map<String, Object> subwayConfig = improvementDetails.get("subway");
        if (subwayConfig != null) {
            int subwayCount = cityImprovements.getOrDefault("subway", 0);
            double subwayReduction = ((Number) subwayConfig.getOrDefault("pollution_reduction", 0)).doubleValue();
            currentPollution -= subwayCount * subwayReduction;
        }
        
        return Math.max(0.0, currentPollution);
    }
    

    public double calculateCommercePercentage(Map<String, Integer> cityImprovements, Map<String, Boolean> activeProjects) {
        double currentCommerce = 0.0;
        
        // Commerce from improvements
        String[] commerceImprovements = {"supermarket", "bank", "mall", "stadium", "subway"};
        for (String impKey : commerceImprovements) {
            Map<String, Object> impConfig = improvementDetails.get(impKey);
            if (impConfig != null) {
                int impCount = cityImprovements.getOrDefault(impKey, 0);
                double commerceBonus = ((Number) impConfig.getOrDefault("commerce_bonus", 0)).doubleValue();
                currentCommerce += impCount * commerceBonus;
            }
        }
        
        // Specialized Police Training commerce bonus
        if (Boolean.TRUE.equals(activeProjects.get("Specialized Police Training"))) {
            Map<String, Object> sptConfig = projectEffects.get("Specialized Police Training");
            if (sptConfig != null) {
                double commerceBonus = ((Number) sptConfig.getOrDefault("commerce_bonus_points", 0)).doubleValue();
                currentCommerce += commerceBonus;
            }
        }
        
        // Get max commerce rate, modified by projects
        @SuppressWarnings("unchecked")
        Map<String, Object> commerceConfig = (Map<String, Object>) gameConstants.get("commerce");
        double maxCommerce = ((Number) commerceConfig.getOrDefault("max_rate", 100.0)).doubleValue();
        
        // Telecommunications Satellite
        if (Boolean.TRUE.equals(activeProjects.get("Telecommunications Satellite")) &&
            "max_commerce".equals(getProjectSetting("Telecommunications Satellite", "target_metric", activeProjects, ""))) {
            maxCommerce = ((Number) getProjectSetting("Telecommunications Satellite", "value", activeProjects, maxCommerce)).doubleValue();
            double tsBonus = ((Number) getProjectSetting("Telecommunications Satellite", "commerce_bonus_points", activeProjects, 0.0)).doubleValue();
            currentCommerce += tsBonus;
        }
        // International Trade Center
        else if (Boolean.TRUE.equals(activeProjects.get("International Trade Center")) &&
                "max_commerce".equals(getProjectSetting("International Trade Center", "target_metric", activeProjects, ""))) {
            maxCommerce = ((Number) getProjectSetting("International Trade Center", "value", activeProjects, maxCommerce)).doubleValue();
            double itcBonus = ((Number) getProjectSetting("International Trade Center", "commerce_bonus_points", activeProjects, 0.0)).doubleValue();
            currentCommerce += itcBonus;
        }
        
        return Math.min(currentCommerce, maxCommerce);
    }
    
    // More methods for calculating disease rate, crime rate, etc.
    
    /**
     * Calculate the optimal build for a city with the given infrastructure and land
     */
    public Map<String, Object> generateOptimalBuild(
            double targetInfraLevel, 
            double targetLandAmount, 
            Map<String, Object> nationDataContext,
            Map<String, Double> resourcePrices,
            String mmrTypeName) {
        
        Map<String, Boolean> activeProjects = getActiveProjects(nationDataContext);
        
        // Round infra to nearest 50
        targetInfraLevel = Math.ceil(targetInfraLevel / 50.0) * 50.0;
        int maxSlotsForInfra = (int) (targetInfraLevel / 50);
        
        // Start with empty build
        Map<String, Integer> buildInProgress = new HashMap<>();
        for (String impKey : improvementDetails.keySet()) {
            buildInProgress.put(impKey, 0);
        }
        
        // Add MMR requirements
        @SuppressWarnings("unchecked")
        Map<String, Object> mmrRequirements = (Map<String, Object>) GameConstants.MMR_TYPES.getOrDefault(
                mmrTypeName, GameConstants.MMR_TYPES.get("peacetime"));
        
        for (Map.Entry<String, Object> entry : mmrRequirements.entrySet()) {
            String impKey = entry.getKey();
            if (!"name".equals(impKey)) {
                int countVal = ((Number) entry.getValue()).intValue();
                buildInProgress.put(impKey, countVal);
            }
        }
        
        // Check if city is powered
        boolean cityIsPoweredForBuild = isCityPowered(buildInProgress);
        
        // If not powered, add nuclear power
        if (!cityIsPoweredForBuild) {
            int nukeMaxCount = getMaxImprovementCount("nuclearpower", activeProjects);
            int nukesToAddInitially = Math.min(nukeMaxCount, 2);
            
            // Calculate slots available after MMR
            int slotsUsedByMMR = 0;
            for (Map.Entry<String, Object> entry : mmrRequirements.entrySet()) {
                String impKey = entry.getKey();
                if (!"name".equals(impKey)) {
                    slotsUsedByMMR += ((Number) entry.getValue()).intValue();
                }
            }
            
            int slotsAvailableAfterMMR = maxSlotsForInfra - slotsUsedByMMR;
            nukesToAddInitially = Math.min(nukesToAddInitially, slotsAvailableAfterMMR);
            
            if (nukesToAddInitially > 0) {
                buildInProgress.put("nuclearpower", nukesToAddInitially);
                cityIsPoweredForBuild = true;
            } else {
                logger.warn("Warning: Optimal build for infra {} lacks slots for power after MMR.", targetInfraLevel);
            }
        }
        
        // Log initial build
        StringBuilder initialBuildInfo = new StringBuilder("Optimal Build: Initial (MMR+Power): { ");
        for (Map.Entry<String, Integer> entry : buildInProgress.entrySet()) {
            if (entry.getValue() > 0) {
                initialBuildInfo.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
            }
        }
        if (initialBuildInfo.length() > 2) {
            initialBuildInfo.setLength(initialBuildInfo.length() - 2); // Remove trailing comma and space
        }
        initialBuildInfo.append(" }, Slots used: ")
                      .append(getTotalImprovementCount(buildInProgress))
                      .append("/")
                      .append(maxSlotsForInfra);
        logger.info(initialBuildInfo.toString());
        
        // Iteratively add improvements
        int iterationCount = 0;
        int maxBuildIterations = maxSlotsForInfra * 2;
        
        while (getTotalImprovementCount(buildInProgress) < maxSlotsForInfra && iterationCount < maxBuildIterations) {
            iterationCount++;
            
            // Calculate current financials
            Map<String, Object> currentFinancials = calculateCityFinancialDetails(
                    buildInProgress, targetInfraLevel, targetLandAmount, 
                    cityIsPoweredForBuild, nationDataContext, resourcePrices);
            
            double currentTotalCityRevenue = (double) currentFinancials.get("total_revenue_city");
            
            String bestNextImprovement = null;
            double highestRevenueIncreaseFound = 0.0;
            
            // Consider all possible improvements except military (already handled by MMR)
            List<String> improvementsToConsider = new ArrayList<>();
            for (String impKey : improvementDetails.keySet()) {
                if (!impKey.equals("barracks") && !impKey.equals("factory") && 
                    !impKey.equals("hangar") && !impKey.equals("drydock")) {
                    improvementsToConsider.add(impKey);
                }
            }
            
            for (String impKeyToEvaluate : improvementsToConsider) {
                int maxCountForThisImp = getMaxImprovementCount(impKeyToEvaluate, activeProjects);
                int currentCount = buildInProgress.getOrDefault(impKeyToEvaluate, 0);
                
                if (currentCount < maxCountForThisImp) {
                    // Create temporary build with one more of this improvement
                    Map<String, Integer> tempBuildConfig = new HashMap<>(buildInProgress);
                    tempBuildConfig.put(impKeyToEvaluate, currentCount + 1);
                    
                    if (getTotalImprovementCount(tempBuildConfig) > maxSlotsForInfra) {
                        continue; // Skip if adding this would exceed slot limit
                    }
                    
                    // Check if this improvement would power the city
                    boolean tempCityPoweredStatus = cityIsPoweredForBuild;
                    if (isPowerPlant(impKeyToEvaluate) && tempBuildConfig.get(impKeyToEvaluate) > 0) {
                        tempCityPoweredStatus = true;
                    }
                    
                    // Calculate financials with this improvement added
                    Map<String, Object> nextIterationFinancials = calculateCityFinancialDetails(
                            tempBuildConfig, targetInfraLevel, targetLandAmount, 
                            tempCityPoweredStatus, nationDataContext, resourcePrices);
                    
                    double revenueIncreaseFromThisImp = (double) nextIterationFinancials.get("total_revenue_city") - currentTotalCityRevenue;
                    
                    if (revenueIncreaseFromThisImp > highestRevenueIncreaseFound) {
                        highestRevenueIncreaseFound = revenueIncreaseFromThisImp;
                        bestNextImprovement = impKeyToEvaluate;
                    }
                }
            }
            
            if (bestNextImprovement != null && highestRevenueIncreaseFound > 0) {
                // Add the best improvement to our build
                int currentCount = buildInProgress.getOrDefault(bestNextImprovement, 0);
                buildInProgress.put(bestNextImprovement, currentCount + 1);
                
                // Update power status if needed
                if (isPowerPlant(bestNextImprovement) && buildInProgress.get(bestNextImprovement) > 0) {
                    cityIsPoweredForBuild = true;
                }
                
                logger.info("Optimal Build: Added {}. Revenue Increase: ${:,.2f}. Slots: {}/{}",
                        bestNextImprovement, highestRevenueIncreaseFound,
                        getTotalImprovementCount(buildInProgress), maxSlotsForInfra);
            } else {
                logger.info("Optimal Build: No further improvement found with positive revenue increase. Final slots: {}/{}",
                        getTotalImprovementCount(buildInProgress), maxSlotsForInfra);
                break;
            }
        }
        
        // Calculate final financials with the complete build
        Map<String, Object> finalCityFinancials = calculateCityFinancialDetails(
                buildInProgress, targetInfraLevel, targetLandAmount, 
                cityIsPoweredForBuild, nationDataContext, resourcePrices);
        
        // Filter the build to only include non-zero improvements
        Map<String, Integer> finalBuildDict = new HashMap<>();
        for (Map.Entry<String, Integer> entry : buildInProgress.entrySet()) {
            if (entry.getValue() > 0) {
                finalBuildDict.put(entry.getKey(), entry.getValue());
            }
        }
        
        // Prepare the result
        Map<String, Object> result = new HashMap<>();
        result.put("build", finalBuildDict);
        result.put("target_infra", targetInfraLevel);
        result.put("target_land", targetLandAmount);
        result.put("mmr_type", mmrTypeName);
        result.put("max_improvement_slots", maxSlotsForInfra);
        
        int totalImproveBuilt = getTotalImprovementCount(finalBuildDict);
        result.put("total_improvements_built", totalImproveBuilt);
        result.put("unused_slots", maxSlotsForInfra - totalImproveBuilt);
        
        // Add all the financial details
        result.putAll(finalCityFinancials);
        
        // Add template format for easy copy-paste into the game
        result.put("template_format", generateBuildTemplate(finalBuildDict, targetInfraLevel));
        
        return result;
    }
    
    /**
     * Check if the city has power
     */
    private boolean isCityPowered(Map<String, Integer> cityImprovements) {
        return cityImprovements.getOrDefault("nuclearpower", 0) > 0 ||
               cityImprovements.getOrDefault("coalpower", 0) > 0 ||
               cityImprovements.getOrDefault("oilpower", 0) > 0 ||
               cityImprovements.getOrDefault("windpower", 0) > 0;
    }
    
    /**
     * Check if the improvement is a power plant
     */
    private boolean isPowerPlant(String improvementKey) {
        return improvementKey.equals("nuclearpower") ||
               improvementKey.equals("coalpower") ||
               improvementKey.equals("oilpower") ||
               improvementKey.equals("windpower");
    }
    
    /**
     * Get the total number of improvements in a build
     */
    private int getTotalImprovementCount(Map<String, Integer> cityImprovements) {
        return cityImprovements.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Generate a template string for the PnW template format
     */
    private Map<String, Object> generateBuildTemplate(Map<String, Integer> buildConfig, double infraLevel) {
        Map<String, Object> templateOutput = new HashMap<>();
        templateOutput.put("infra_needed", infraLevel);
        templateOutput.put("imp_total", getTotalImprovementCount(buildConfig));
        
        // Special case for hangar
        if (buildConfig.containsKey("hangar") && buildConfig.get("hangar") > 0) {
            templateOutput.put("imp_hangars", buildConfig.get("hangar"));
        }
        
        // Add all other improvements
        for (Map.Entry<String, Integer> entry : buildConfig.entrySet()) {
            String key = entry.getKey();
            int count = entry.getValue();
            
            if (!key.equals("hangar") && count > 0) {
                templateOutput.put("imp_" + key, count);
            }
        }
        
        return templateOutput;
    }
    
    /**
     * Calculate the city's financial details
     */
    public Map<String, Object> calculateCityFinancialDetails(
            Map<String, Integer> cityImprovements, 
            double infraLevel, 
            double landAmount,
            boolean cityIsPowered, 
            Map<String, Object> nationDataContext,
            Map<String, Double> resourcePrices) {
        
        Map<String, Boolean> activeProjects = getActiveProjects(nationDataContext);
        String nationPolicyName = (String) nationDataContext.getOrDefault("domestic_policy", "");
        
        // Get city age (defaulting to 1 year if not available)
        int cityAgeInDays = 365;
        if (cityImprovements.containsKey("date_founded")) {
            // Date parsing would go here, using a library like LocalDate or similar
            // For simplicity, we'll use 1 year as default if parsing fails
        }
        
        // Calculate metrics
        double pollutionVal = calculatePollutionValue(cityImprovements, activeProjects, nationPolicyName);
        double commercePerc = calculateCommercePercentage(cityImprovements, activeProjects);
        double diseaseRateDec = estimateDiseaseRateDecimal(infraLevel, landAmount, 
                cityImprovements.getOrDefault("hospital", 0), pollutionVal, activeProjects, cityIsPowered);
        double crimeRatePercVal = estimateCrimeRatePercentage(infraLevel, commercePerc, 
                cityImprovements.getOrDefault("policestation", 0), activeProjects, nationPolicyName);
        double currentPopulation = calculateFinalPopulation(infraLevel, diseaseRateDec, crimeRatePercVal, cityAgeInDays);
        
        // Calculate resources and income
        Map<String, Object> resourceInfo = calculateCityResources(cityImprovements, activeProjects, landAmount, infraLevel);
        @SuppressWarnings("unchecked")
        Map<String, Double> resourceProductionMap = (Map<String, Double>) resourceInfo.get("production");
        @SuppressWarnings("unchecked")
        Map<String, Double> resourceUsageMap = (Map<String, Double>) resourceInfo.get("usage");
        
        // Food consumption by population
        @SuppressWarnings("unchecked")
        Map<String, Object> populationConfig = (Map<String, Object>) gameConstants.get("population");
        double foodConsumedByPopulation = currentPopulation * ((Number) populationConfig.get("food_consumption_per_pop_daily")).doubleValue();
        double currentFoodUsage = resourceUsageMap.getOrDefault("food", 0.0);
        resourceUsageMap.put("food", currentFoodUsage + foodConsumedByPopulation);
        
        // Base population income calculation
        @SuppressWarnings("unchecked")
        Map<String, Object> incomeConfig = (Map<String, Object>) gameConstants.get("income");
        double baseMultiplier = ((Number) incomeConfig.get("base_multiplier")).doubleValue();
        double commerceDivisor = ((Number) incomeConfig.get("commerce_divisor")).doubleValue();
        
        double basePopulationIncome = (((commercePerc / commerceDivisor) * baseMultiplier) + baseMultiplier) * currentPopulation;
        
        // Apply food shortage penalty if applicable
        double foodProduction = resourceProductionMap.getOrDefault("food", 0.0);
        double foodConsumption = resourceUsageMap.getOrDefault("food", 0.0);
        boolean foodPenaltyApplied = false;
        
        if (foodProduction < foodConsumption) {
            @SuppressWarnings("unchecked")
            Map<String, Object> foodConfig = (Map<String, Object>) gameConstants.get("food");
            double noFoodPenaltyMultiplier = ((Number) foodConfig.get("no_food_penalty_multiplier")).doubleValue();
            basePopulationIncome *= noFoodPenaltyMultiplier;
            foodPenaltyApplied = true;
        }
        
        // Policy bonuses (Open Markets)
        double openMarketsBonusAmount = 0.0;
        if ("Open Markets".equals(nationPolicyName)) {
            Map<String, Object> openMarketsPolicy = policyEffects.get("Open Markets");
            double incomeBonusPercentage = ((Number) openMarketsPolicy.getOrDefault("base_value_percentage", 0.0)).doubleValue();
            
            // Check for boosting projects
            String boostingProject1 = (String) openMarketsPolicy.get("boosting_project1");
            String boostingProject2 = (String) openMarketsPolicy.get("boosting_project2");
            
            if ((boostingProject1 != null && Boolean.TRUE.equals(activeProjects.get(boostingProject1))) ||
                (boostingProject2 != null && Boolean.TRUE.equals(activeProjects.get(boostingProject2)))) {
                // Use boosted value if available, otherwise use base value
                double boostedValue = ((Number) openMarketsPolicy.getOrDefault("project_boost_value_percentage", incomeBonusPercentage)).doubleValue();
                incomeBonusPercentage = boostedValue;
            }
            
            openMarketsBonusAmount = basePopulationIncome * (incomeBonusPercentage / 100.0);
        }
        
        double incomeAfterOpenMarkets = basePopulationIncome + openMarketsBonusAmount;
        
        // Alliance treasure bonus
        double treasureBonusPercentage = ((Number) gameConstants.getOrDefault("treasure_bonus_percentage", 0.02)).doubleValue();
        double allianceTreasureBonusAmount = incomeAfterOpenMarkets * treasureBonusPercentage;
        
        // Color trade bloc bonus (placeholder value for now)
        double colorTradeBlocBonusAmount = 800000.0;
        
        double grossMonetaryIncomeVal = incomeAfterOpenMarkets + allianceTreasureBonusAmount + colorTradeBlocBonusAmount;
        
        // Calculate resource values
        Map<String, Map<String, Double>> netResourceValuesMap = new HashMap<>();
        double totalNetResourceMonetaryValue = 0.0;
        
        for (Map.Entry<String, Double> entry : resourcePrices.entrySet()) {
            String resourceKey = entry.getKey();
            double priceVal = entry.getValue();
            
            double production = resourceProductionMap.getOrDefault(resourceKey, 0.0);
            double usage = resourceUsageMap.getOrDefault(resourceKey, 0.0);
            double netAmountVal = production - usage;
            double monetaryValue = netAmountVal * priceVal;
            
            Map<String, Double> resourceValueInfo = new HashMap<>();
            resourceValueInfo.put("net_amount", netAmountVal);
            resourceValueInfo.put("value", monetaryValue);
            
            netResourceValuesMap.put(resourceKey, resourceValueInfo);
            totalNetResourceMonetaryValue += monetaryValue;
        }
        
        // Calculate upkeep
        double totalImprovementUpkeep = calculateTotalImprovementUpkeep(cityImprovements, nationPolicyName, activeProjects);
        double netMonetaryIncomeForCity = grossMonetaryIncomeVal - totalImprovementUpkeep;
        
        double avgIncomePerPerson = currentPopulation > 0 ? grossMonetaryIncomeVal / currentPopulation : 0;
        
        // Compose the results
        Map<String, Object> result = new HashMap<>();
        result.put("population", currentPopulation);
        result.put("commerce_rate", commercePerc);
        result.put("disease_rate_percentage", diseaseRateDec * 100.0);
        result.put("crime_rate_percentage", crimeRatePercVal);
        result.put("pollution", pollutionVal);
        result.put("base_population_income", basePopulationIncome);
        result.put("open_markets_bonus_amount", openMarketsBonusAmount);
        result.put("alliance_treasure_bonus_amount", allianceTreasureBonusAmount);
        result.put("color_trade_bloc_bonus_placeholder", colorTradeBlocBonusAmount);
        result.put("gross_monetary_income", grossMonetaryIncomeVal);
        result.put("average_income_per_person", avgIncomePerPerson);
        result.put("improvement_upkeep", totalImprovementUpkeep);
        result.put("net_monetary_income_city", netMonetaryIncomeForCity);
        result.put("resource_production", resourceProductionMap);
        result.put("resource_usage", resourceUsageMap);
        result.put("net_resource_values", netResourceValuesMap);
        result.put("total_net_resource_value", totalNetResourceMonetaryValue);
        result.put("total_revenue_city", netMonetaryIncomeForCity + totalNetResourceMonetaryValue);
        result.put("city_age_days", cityAgeInDays);
        result.put("is_powered", cityIsPowered);
        result.put("food_shortage_penalty_applied", foodPenaltyApplied);
        
        return result;
    }
    
    /**
     * Calculate the city's resource production and consumption
     */
    public Map<String, Object> calculateCityResources(
            Map<String, Integer> cityImprovements,
            Map<String, Boolean> activeProjects,
            double landAmount,
            double infra) {
        
        Map<String, Double> productionTotals = new HashMap<>();
        Map<String, Double> usageTotals = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Object>> entry : improvementDetails.entrySet()) {
            String impKey = entry.getKey();
            Map<String, Object> impConfig = entry.getValue();
            
            int count = cityImprovements.getOrDefault(impKey, 0);
            if (count > 0) {
                // Handle mines and other base resource producers
                if (impConfig.containsKey("base_prod_rate_per_turn") && impConfig.containsKey("resource")) {
                    String resource = (String) impConfig.get("resource");
                    
                    if ("farm".equals(impKey)) {
                        // Special handling for farms
                        @SuppressWarnings("unchecked")
                        Map<String, Object> resourceProdConfig = (Map<String, Object>) gameConstants.get("resource_production");
                        
                        double farmLandDivisorBase = ((Number) resourceProdConfig.get("farm_land_divisor_base")).doubleValue();
                        double farmLandDivisor = ((Number) getProjectSetting("Mass Irrigation", "value", activeProjects, farmLandDivisorBase)).doubleValue();
                        
                        double prodPerTurn = landAmount / farmLandDivisor;
                        double farmBulkBonusFactor = ((Number) resourceProdConfig.get("farm_bulk_bonus_factor")).doubleValue();
                        double farmBulkMultiplier = 1.0 + (Math.max(0, count - 1) * farmBulkBonusFactor);
                        double turnsPerDay = ((Number) resourceProdConfig.get("turns_per_day")).doubleValue();
                        
                        double foodProduction = count * prodPerTurn * farmBulkMultiplier * turnsPerDay;
                        productionTotals.put("food", productionTotals.getOrDefault("food", 0.0) + foodProduction);
                    } else {
                        // Mines
                        double baseRate = ((Number) impConfig.get("base_prod_rate_per_turn")).doubleValue();
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> resourceProdConfig = (Map<String, Object>) gameConstants.get("resource_production");
                        
                        double bulkBonusFactor = ((Number) resourceProdConfig.get("mine_bulk_bonus_factor")).doubleValue();
                        double bulkMultiplier = 1.0 + (Math.max(0, count - 1) * bulkBonusFactor);
                        double turnsPerDay = ((Number) resourceProdConfig.get("turns_per_day")).doubleValue();
                        
                        double dailyProduction = count * baseRate * bulkMultiplier * turnsPerDay;
                        
                        // Apply project multipliers if applicable
                        String projectProdMultiplierKey = resource + "_production_multiplier";
                        for (Map.Entry<String, Map<String, Object>> projEntry : projectEffects.entrySet()) {
                            String projName = projEntry.getKey();
                            Map<String, Object> projSettings = projEntry.getValue();
                            
                            if (Boolean.TRUE.equals(activeProjects.get(projName)) && 
                                projectProdMultiplierKey.equals(projSettings.get("target_metric"))) {
                                double multiplier = ((Number) projSettings.getOrDefault("value", 1.0)).doubleValue();
                                dailyProduction *= multiplier;
                                break;
                            }
                        }
                        
                        productionTotals.put(resource, productionTotals.getOrDefault(resource, 0.0) + dailyProduction);
                    }
                } 
                // Handle processing buildings that produce resources
                else if (impConfig.containsKey("base_prod_rate_daily") && impConfig.containsKey("resource")) {
                    String resource = (String) impConfig.get("resource");
                    double baseDailyRate = ((Number) impConfig.get("base_prod_rate_daily")).doubleValue();
                    double dailyProduction = count * baseDailyRate;
                    
                    // Apply project multipliers if applicable
                    String projectProdMultiplierKey = resource + "_production_multiplier";
                    for (Map.Entry<String, Map<String, Object>> projEntry : projectEffects.entrySet()) {
                        String projName = projEntry.getKey();
                        Map<String, Object> projSettings = projEntry.getValue();
                        
                        if (Boolean.TRUE.equals(activeProjects.get(projName)) && 
                            projectProdMultiplierKey.equals(projSettings.get("target_metric"))) {
                            double multiplier = ((Number) projSettings.getOrDefault("value", 1.0)).doubleValue();
                            dailyProduction *= multiplier;
                            break;
                        }
                    }
                    
                    productionTotals.put(resource, productionTotals.getOrDefault(resource, 0.0) + dailyProduction);
                }
                
                // Handle resource inputs for processing buildings
                if (impConfig.containsKey("inputs_daily")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Double> inputsDaily = (Map<String, Double>) impConfig.get("inputs_daily");
                    
                    for (Map.Entry<String, Double> input : inputsDaily.entrySet()) {
                        String inputRes = input.getKey();
                        double amountDaily = input.getValue();
                        usageTotals.put(inputRes, usageTotals.getOrDefault(inputRes, 0.0) + (count * amountDaily));
                    }
                } 
                // Handle power plant inputs which scale with infra
                else if (impConfig.containsKey("inputs_daily_per_plant")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Double> inputsDaily = (Map<String, Double>) impConfig.get("inputs_daily_per_plant");
                    
                    for (Map.Entry<String, Double> input : inputsDaily.entrySet()) {
                        String inputRes = input.getKey();
                        double amountDailyPerPlant = input.getValue();
                        
                        if (count * 2000 >= infra && "uranium".equals(inputRes)) {
                            // Special case for nuclear power plants with uranium
                            double uraniumUsage = ((Math.round(infra / 1000) * 1000) / 1000 * amountDailyPerPlant) * count;
                            usageTotals.put(inputRes, usageTotals.getOrDefault(inputRes, 0.0) + uraniumUsage);
                        } else {
                            // Standard case for other power plants
                            double resourceUsage = (Math.round(infra / 100) * 100) / 100 * amountDailyPerPlant * count;
                            usageTotals.put(inputRes, usageTotals.getOrDefault(inputRes, 0.0) + resourceUsage);
                        }
                    }
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("production", productionTotals);
        result.put("usage", usageTotals);
        
        return result;
    }
    
    /**
     * Calculate the total upkeep cost for all improvements
     */
    private double calculateTotalImprovementUpkeep(
            Map<String, Integer> cityImprovements,
            String nationPolicyName,
            Map<String, Boolean> activeProjects) {
        
        double totalUpkeep = 0.0;
        
        for (Map.Entry<String, Integer> entry : cityImprovements.entrySet()) {
            String impKey = entry.getKey();
            int count = entry.getValue();
            
            if (count > 0 && improvementDetails.containsKey(impKey)) {
                Map<String, Object> impConfig = improvementDetails.get(impKey);
                double upkeep = ((Number) impConfig.getOrDefault("upkeep", 0)).doubleValue();
                totalUpkeep += count * upkeep;
            }
        }
        
        // Policy effects could go here
        
        return totalUpkeep;
    }
    
    /**
     * Estimate the disease rate (as a decimal) for a city
     */
    public double estimateDiseaseRateDecimal(
            double infraLevel,
            double landAmount,
            int numHospitals,
            double pollutionValue,
            Map<String, Boolean> activeProjects,
            boolean cityIsPoweredFlag) {
        
        @SuppressWarnings("unchecked")
        Map<String, Object> diseaseConf = (Map<String, Object>) gameConstants.get("disease");
        
        // Calculate population density
        double popDensityInfraMultiplier = ((Number) diseaseConf.get("pop_density_infra_multiplier")).doubleValue();
        double basePopForDensityCalc = infraLevel * popDensityInfraMultiplier;
        double populationDensityForDisease = basePopForDensityCalc / (landAmount + 0.001);
        
        // Calculate disease components
        double popDensityFactor1 = ((Number) diseaseConf.get("pop_density_factor1")).doubleValue();
        double popDensityExponent = ((Number) diseaseConf.get("pop_density_exponent")).doubleValue();
        double popDensityBaseReductionPercentage = ((Number) diseaseConf.get("pop_density_base_reduction_percentage")).doubleValue();
        
        double term1DensityEffectDecimal = ((popDensityFactor1 * Math.pow(populationDensityForDisease, popDensityExponent)) - 
                                          popDensityBaseReductionPercentage) / 100.0;
        
        double infraEffectDivisor = ((Number) diseaseConf.get("infra_effect_divisor")).doubleValue();
        double term2InfraEffectPercent = infraLevel / infraEffectDivisor;
        
        double pollutionEffectMultiplier = ((Number) diseaseConf.get("pollution_effect_multiplier")).doubleValue();
        double pollutionModifierDecimal = pollutionValue * pollutionEffectMultiplier;
        
        double hospitalModifierPoints = 0.0;
        if (cityIsPoweredFlag && numHospitals > 0) {
            Map<String, Object> hospitalConfig = improvementDetails.get("hospital");
            double hospitalEffectivenessPoints = ((Number) hospitalConfig.get("disease_reduction_base")).doubleValue();
            
            // Check for Clinical Research Center project
            if (Boolean.TRUE.equals(activeProjects.get("Clinical Research Center")) && 
                "hospital_disease_reduction".equals(getProjectSetting("Clinical Research Center", "target_metric", activeProjects, ""))) {
                hospitalEffectivenessPoints = ((Number) getProjectSetting(
                    "Clinical Research Center", 
                    "disease_reduction_boosted_val_points", 
                    activeProjects, 
                    hospitalEffectivenessPoints)).doubleValue();
            }
            
            hospitalModifierPoints = numHospitals * hospitalEffectivenessPoints;
        }
        
        double netDiseaseDecimal = term1DensityEffectDecimal + (term2InfraEffectPercent / 100.0) + 
                                   pollutionModifierDecimal - (hospitalModifierPoints / 100.0);
        
        return Math.max(0.0, netDiseaseDecimal);
    }
    
    /**
     * Estimate the crime rate (as a percentage) for a city
     */
    public double estimateCrimeRatePercentage(
            double infraLevel,
            double commercePercentage,
            int numPoliceStations,
            Map<String, Boolean> activeProjects,
            String nationPolicyName) {
        
        @SuppressWarnings("unchecked")
        Map<String, Object> crimeConf = (Map<String, Object>) gameConstants.get("crime");
        
        double commerceOffset = ((Number) crimeConf.get("commerce_offset")).doubleValue();
        double infraMultiplier = ((Number) crimeConf.get("infra_multiplier")).doubleValue();
        double divisor = ((Number) crimeConf.get("divisor")).doubleValue();
        
        double crimeFromInfraCommercePercent = (Math.pow(commerceOffset - commercePercentage, 2) + 
                                               (infraLevel * infraMultiplier)) / divisor;
        
        double policeEffectivenessPoints = 0.0;
        if (numPoliceStations > 0) {
            Map<String, Object> policeConfig = improvementDetails.get("policestation");
            policeEffectivenessPoints = ((Number) policeConfig.get("crime_reduction_base")).doubleValue();
            
            // Check for Specialized Police Training project
            if (Boolean.TRUE.equals(activeProjects.get("Specialized Police Training"))) {
                Map<String, Object> sptConfig = projectEffects.get("Specialized Police Training");
                double bonusPoints = ((Number) sptConfig.getOrDefault("bonus_value_points", 0.0)).doubleValue();
                policeEffectivenessPoints += bonusPoints;
            }
        }
        
        double policeModifierPercent = numPoliceStations * policeEffectivenessPoints;
        double netCrimePercentage = crimeFromInfraCommercePercent - policeModifierPercent;
        
        return Math.max(0.0, Math.min(netCrimePercentage, 100.0));
    }
    
    public double calculateFinalPopulation(
            double infraLevel,
            double diseaseRateDecimalVal,
            double crimeRatePercentageVal,
            int cityAgeInDays) {
        
        @SuppressWarnings("unchecked")
        Map<String, Object> popConf = (Map<String, Object>) gameConstants.get("population");
        
        double basePerInfra = ((Number) popConf.get("base_per_infra")).doubleValue();
        double baseCityPop = ((Number) popConf.get("base_city_pop")).doubleValue();
        
        double basePopulationForMainFormula = infraLevel * basePerInfra + baseCityPop;
        double diseaseImpactValue = diseaseRateDecimalVal * infraLevel;
        
        double crimeImpactRateDivisor = ((Number) popConf.get("crime_impact_rate_divisor")).doubleValue();
        double crimeImpactInfraMultiplier = ((Number) popConf.get("crime_impact_infra_multiplier")).doubleValue();
        double crimeImpactBaseReduction = ((Number) popConf.get("crime_impact_base_reduction")).doubleValue();
        
        double crimeImpactValue = Math.max(0.0, 
                                         (crimeRatePercentageVal / crimeImpactRateDivisor) * 
                                         (infraLevel * crimeImpactInfraMultiplier) - 
                                         crimeImpactBaseReduction);
        
        double populationImpacted = basePopulationForMainFormula - diseaseImpactValue - crimeImpactValue;
        
        double ageLogDivisor = ((Number) popConf.get("age_log_divisor")).doubleValue();
        double ageBonusFactor = 1.0 + (Math.log(Math.max(1, cityAgeInDays)) / ageLogDivisor);
        
        double finalPopulation = populationImpacted * ageBonusFactor;
        
        return Math.max(0.0, finalPopulation);
    }
    
    // Other methods for checking MMR compliance, etc. would go here
}