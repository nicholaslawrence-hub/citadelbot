package com.kt.pnwanalyzer.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Constants for the Politics & War game mechanics
 */
public class GameConstants {
    // MMR Types (Military Readiness Requirements)
    public static final Map<String, Map<String, Object>> MMR_TYPES = new HashMap<>();
    static {
        Map<String, Object> peacetime = new HashMap<>();
        peacetime.put("barracks", 0);
        peacetime.put("factory", 2);
        peacetime.put("hangar", 5);
        peacetime.put("drydock", 0);
        peacetime.put("name", "Peacetime MMR (0/2/5/0)");
        
        Map<String, Object> raid = new HashMap<>();
        raid.put("barracks", 5);
        raid.put("factory", 0);
        raid.put("hangar", 0);
        raid.put("drydock", 0);
        raid.put("name", "Raid MMR (5/0/0/0)");
        
        Map<String, Object> wartime = new HashMap<>();
        wartime.put("barracks", 5);
        wartime.put("factory", 5);
        wartime.put("hangar", 5);
        wartime.put("drydock", 3);
        wartime.put("name", "Wartime MMR (5/5/5/3)");
        
        MMR_TYPES.put("peacetime", peacetime);
        MMR_TYPES.put("raid", raid);
        MMR_TYPES.put("wartime", wartime);
    }
    
    // Military Unit Daily Upkeep
    public static final Map<String, Double> MILITARY_UNIT_UPKEEP = new HashMap<>();
    static {
        MILITARY_UNIT_UPKEEP.put("soldiers", 1.25);
        MILITARY_UNIT_UPKEEP.put("tanks", 50.0);
        MILITARY_UNIT_UPKEEP.put("aircraft", 500.0);
        MILITARY_UNIT_UPKEEP.put("ships", 3375.0);
    }
    
    // Improvement Details (cost, max count, upkeep, etc.)
    public static final Map<String, Map<String, Object>> IMPROVEMENT_DETAILS = new HashMap<>();
    static {
        // Military improvements
        addImprovement("barracks", 3000000, 5, 0);
        addImprovement("factory", 15000000, 5, 0);
        addImprovement("hangar", 100000000, 5, 0);
        addImprovement("drydock", 250000000, 3, 0);
        
        // Commercial improvements
        Map<String, Object> supermarket = addImprovement("supermarket", 5000, 6, 600);
        supermarket.put("commerce_bonus", 4.0);
        
        Map<String, Object> bank = addImprovement("bank", 22000, 5, 1800);
        bank.put("commerce_bonus", 6.0);
        
        Map<String, Object> mall = addImprovement("mall", 10000, 4, 5400);
        mall.put("commerce_bonus", 8.0);
        
        Map<String, Object> stadium = addImprovement("stadium", 130000, 3, 12150);
        stadium.put("commerce_bonus", 10.0);
        
        Map<String, Object> subway = addImprovement("subway", 350000, 1, 3250);
        subway.put("commerce_bonus", 8.0);
        subway.put("pollution_reduction", 45);
        
        // Service improvements
        Map<String, Object> hospital = addImprovement("hospital", 100000, 5, 1000);
        hospital.put("disease_reduction_base", 2.5);
        
        Map<String, Object> policeStation = addImprovement("policestation", 90000, 1, 750);
        policeStation.put("crime_reduction_base", 2.5);
        
        Map<String, Object> recyclingCenter = addImprovement("recyclingcenter", 125000, 3, 2500);
        recyclingCenter.put("pollution_reduction", 70);
        
        Map<String, Object> coalMine = addImprovement("coalmine", 1000, 10, 400);
        coalMine.put("base_prod_rate_per_turn", 3.0);
        coalMine.put("resource", "coal");
        coalMine.put("pollution", 12);
        
        Map<String, Object> oilWell = addImprovement("oilwell", 1500, 10, 1500);
        oilWell.put("base_prod_rate_per_turn", 3.0);
        oilWell.put("resource", "oil");
        oilWell.put("pollution", 12);
        
        Map<String, Object> ironMine = addImprovement("ironmine", 9500, 10, 1600);
        ironMine.put("base_prod_rate_per_turn", 3.0);
        ironMine.put("resource", "iron");
        ironMine.put("pollution", 12);
        
        Map<String, Object> bauxiteMine = addImprovement("bauxitemine", 9500, 10, 1600);
        bauxiteMine.put("base_prod_rate_per_turn", 3.0);
        bauxiteMine.put("resource", "bauxite");
        bauxiteMine.put("pollution", 12);
        
        Map<String, Object> leadMine = addImprovement("leadmine", 7500, 10, 1500);
        leadMine.put("base_prod_rate_per_turn", 3.0);
        leadMine.put("resource", "lead");
        leadMine.put("pollution", 12);
        
        Map<String, Object> uraMine = addImprovement("uramine", 25000, 5, 5000);
        uraMine.put("base_prod_rate_per_turn", 3.0);
        uraMine.put("resource", "uranium");
        uraMine.put("pollution", 12.0);
        
        Map<String, Object> farm = addImprovement("farm", 1000, 20, 300);
        farm.put("resource", "food");
        farm.put("pollution", 2.0);
        
        Map<String, Object> gasRefinery = addImprovement("gasrefinery", 45000, 5, 4000);
        gasRefinery.put("base_prod_rate_daily", 6.0);
        gasRefinery.put("resource", "gasoline");
        gasRefinery.put("pollution", 32.0);
        Map<String, Double> gasInputs = new HashMap<>();
        gasInputs.put("oil", 2.0);
        gasRefinery.put("inputs_daily", gasInputs);
        
        Map<String, Object> steelMill = addImprovement("steelmill", 45000, 5, 4000);
        steelMill.put("base_prod_rate_daily", 9.0);
        steelMill.put("resource", "steel");
        steelMill.put("pollution", 40.0);
        Map<String, Double> steelInputs = new HashMap<>();
        steelInputs.put("iron", 3.0);
        steelInputs.put("coal", 3.0);
        steelMill.put("inputs_daily", steelInputs);
        
        Map<String, Object> aluminumRefinery = addImprovement("aluminumrefinery", 30000, 5, 2500);
        aluminumRefinery.put("base_prod_rate_daily", 9.0);
        aluminumRefinery.put("resource", "aluminum");
        aluminumRefinery.put("pollution", 40);
        Map<String, Double> aluminumInputs = new HashMap<>();
        aluminumInputs.put("bauxite", 3.0);
        aluminumRefinery.put("inputs_daily", aluminumInputs);
        
        Map<String, Object> munitionsFactory = addImprovement("munitionsfactory", 35000, 5, 3500);
        munitionsFactory.put("base_prod_rate_daily", 18.0);
        munitionsFactory.put("resource", "munitions");
        munitionsFactory.put("pollution", 32);
        Map<String, Double> munitionsInputs = new HashMap<>();
        munitionsInputs.put("lead", 6.0);
        munitionsFactory.put("inputs_daily", munitionsInputs);
        
        Map<String, Object> coalPower = addImprovement("coalpower", 5000, 20, 2400);
        coalPower.put("pollution", 6);
        Map<String, Double> coalInputs = new HashMap<>();
        coalInputs.put("coal", 24.0);
        coalPower.put("inputs_daily_per_plant", coalInputs);
        
        Map<String, Object> oilPower = addImprovement("oilpower", 7000, 20, 2400);
        oilPower.put("pollution", 150);
        Map<String, Double> oilInputs = new HashMap<>();
        oilInputs.put("oil", 12.0);
        oilPower.put("inputs_daily_per_plant", oilInputs);
        
        Map<String, Object> nuclearPower = addImprovement("nuclearpower", 500000, 20, 10500);
        Map<String, Double> uraniumInputs = new HashMap<>();
        uraniumInputs.put("uranium", 2.4);
        nuclearPower.put("inputs_daily_per_plant", uraniumInputs);
        
        Map<String, Object> windPower = addImprovement("windpower", 30000, 20, 500);
        windPower.put("pollution", 0);
    }
    
    public static final Map<String, Object> GAME_CONSTANTS = new HashMap<>();
    static {
        Map<String, Object> infra = new HashMap<>();
        infra.put("base_cost", 300);
        infra.put("exp_factor", 2.2);
        infra.put("divisor", 710);
        infra.put("base_offset", 10);
        GAME_CONSTANTS.put("infra", infra);
        
        Map<String, Object> land = new HashMap<>();
        land.put("base_cost", 50);
        land.put("exp_factor", 2.0);
        land.put("multiplier", 0.002);
        land.put("base_offset", 20);
        GAME_CONSTANTS.put("land", land);
        
        // Population mechanics
        Map<String, Object> population = new HashMap<>();
        population.put("base_per_infra", 100);
        population.put("base_city_pop", 250);
        population.put("age_log_divisor", 15.0);
        population.put("food_consumption_per_pop_daily", 0.001);
        population.put("crime_impact_rate_divisor", 10.0);
        population.put("crime_impact_infra_multiplier", 100.0);
        population.put("crime_impact_base_reduction", 25.0);
        GAME_CONSTANTS.put("population", population);
        
        // Disease mechanics
        Map<String, Object> disease = new HashMap<>();
        disease.put("pop_density_infra_multiplier", 100.0);
        disease.put("pop_density_factor1", 0.01);
        disease.put("pop_density_exponent", 2.0);
        disease.put("pop_density_base_reduction_percentage", 25.0);
        disease.put("infra_effect_divisor", 1000.0);
        disease.put("pollution_effect_multiplier", 0.05);
        GAME_CONSTANTS.put("disease", disease);
        
        // Crime mechanics
        Map<String, Object> crime = new HashMap<>();
        crime.put("commerce_offset", 103.0);
        crime.put("infra_multiplier", 100.0);
        crime.put("divisor", 111111.0);
        GAME_CONSTANTS.put("crime", crime);
        
        // Commerce mechanics
        Map<String, Object> commerce = new HashMap<>();
        commerce.put("base_rate", 0.0);
        commerce.put("max_rate", 100.0);
        GAME_CONSTANTS.put("commerce", commerce);
        
        // Income mechanics
        Map<String, Object> income = new HashMap<>();
        income.put("base_multiplier", 0.725);
        income.put("commerce_divisor", 50.0);
        GAME_CONSTANTS.put("income", income);
        
        // Food mechanics
        Map<String, Object> food = new HashMap<>();
        food.put("no_food_penalty_multiplier", 0.67);
        GAME_CONSTANTS.put("food", food);
        
        // Resource production mechanics
        Map<String, Object> resourceProduction = new HashMap<>();
        resourceProduction.put("mine_bulk_bonus_factor", 0.05);
        resourceProduction.put("farm_bulk_bonus_factor", 0.025);
        resourceProduction.put("farm_land_divisor_base", 500.0);
        resourceProduction.put("turns_per_day", 12.0);
        GAME_CONSTANTS.put("resource_production", resourceProduction);
        
        GAME_CONSTANTS.put("treasure_bonus_percentage", 0.02);
    }
    
    // Policy effects
    public static final Map<String, Map<String, Object>> POLICY_EFFECTS = new HashMap<>();
    static {
        Map<String, Object> openMarkets = new HashMap<>();
        openMarkets.put("effect_type", "special_income_multiplier");
        openMarkets.put("base_value_percentage", 1.5);
        openMarkets.put("boosting_project2", "Bureau of Domestic Affairs");
        openMarkets.put("boosting_project1", "Government Support Agency");
        openMarkets.put("target_metric", "net_income");
        POLICY_EFFECTS.put("Open Markets", openMarkets);
        
        Map<String, Object> imperialism = new HashMap<>();
        imperialism.put("effect_type", "special_upkeep_multiplier");
        imperialism.put("base_reduction_percentage", 5.0);
        imperialism.put("boosting_project2", "Bureau of Domestic Affairs");
        imperialism.put("boosting_project1", "Government Support Agency");
        imperialism.put("target_metric", "military_upkeep");
        POLICY_EFFECTS.put("Imperialism", imperialism);
    }
    
    // Project effects
    public static final Map<String, Map<String, Object>> PROJECT_EFFECTS = new HashMap<>();
    static {
        Map<String, Object> massIrrigation = new HashMap<>();
        massIrrigation.put("target_metric", "farm_land_divisor");
        massIrrigation.put("value", 400.0);
        PROJECT_EFFECTS.put("Mass Irrigation", massIrrigation);
        
        Map<String, Object> clinicalResearchCenter = new HashMap<>();
        clinicalResearchCenter.put("target_metric", "hospital_disease_reduction");
        clinicalResearchCenter.put("bonus_value_points", 1.0);
        Map<String, Integer> crcMaxChanges = new HashMap<>();
        crcMaxChanges.put("hospital", 6);
        clinicalResearchCenter.put("max_change", crcMaxChanges);
        clinicalResearchCenter.put("disease_reduction_boosted_val_points", 3.5);
        PROJECT_EFFECTS.put("Clinical Research Center", clinicalResearchCenter);
        
        // Add other project effects similarly
    }

    // GraphQL field names for projects
    public static final String[] PROJECT_FIELDS_GRAPHQL = {
        "adv_city_planning", "adv_engineering_corps", "arable_land_agency", "arms_stockpile", "bauxite_works",
        "center_for_civil_engineering", "cia", "city_planning", "clinical_research_center", "emergency_gasoline_reserve",
        "green_technologies", "iron_dome", "iron_works", "international_trade_center", "mass_irrigation", "missile_launch_pad",
        "moon_landing", "nuclear_research_facility", "pirate_economy", "propaganda_bureau", "recycling_initiative",
        "space_program", "specialized_police_training", "spy_satellite", "telecommunications_satellite", "urban_planning",
        "vital_defense_system", "surveillance_network", "military_salvage", "guiding_satellite",
        "bureau_of_domestic_affairs", "fallout_shelter", "uranium_enrichment_program", "government_support_agency"
    };
    
    // Project name mapping (from GraphQL field names to display names)
    public static final Map<String, String> PROJECT_NAME_MAP = new HashMap<>();
    static {
        // Initialize with standard formatting (replace underscores with spaces, capitalize)
        for (String field : PROJECT_FIELDS_GRAPHQL) {
            PROJECT_NAME_MAP.put(field, capitalizeWords(field.replace('_', ' ')));
        }
        
        // Override specific names that need special handling
        PROJECT_NAME_MAP.put("adv_engineering_corps", "Advanced Engineering Corps");
        PROJECT_NAME_MAP.put("arable_land_agency", "Arable Land Agency");
        PROJECT_NAME_MAP.put("arms_stockpile", "Arms Stockpile");
        PROJECT_NAME_MAP.put("bauxite_works", "Bauxite Works");
        PROJECT_NAME_MAP.put("center_for_civil_engineering", "Center for Civil Engineering");
        PROJECT_NAME_MAP.put("clinical_research_center", "Clinical Research Center");
        PROJECT_NAME_MAP.put("emergency_gasoline_reserve", "Emergency Gasoline Reserve");
        PROJECT_NAME_MAP.put("green_technologies", "Green Technologies");
        PROJECT_NAME_MAP.put("iron_works", "Iron Works");
        PROJECT_NAME_MAP.put("international_trade_center", "International Trade Center");
        PROJECT_NAME_MAP.put("mass_irrigation", "Mass Irrigation");
        PROJECT_NAME_MAP.put("recycling_initiative", "Recycling Initiative");
        PROJECT_NAME_MAP.put("specialized_police_training", "Specialized Police Training");
        PROJECT_NAME_MAP.put("telecommunications_satellite", "Telecommunications Satellite");
        PROJECT_NAME_MAP.put("uranium_enrichment_program", "Uranium Enrichment Program");
        PROJECT_NAME_MAP.put("bureau_of_domestic_affairs", "Bureau of Domestic Affairs");
        PROJECT_NAME_MAP.put("research_and_development_center", "Research and Development Center");
        PROJECT_NAME_MAP.put("government_support_agency", "Government Support Agency");
    }
    
    private static Map<String, Object> addImprovement(String key, int cost, int max, int upkeep) {
        Map<String, Object> improvement = new HashMap<>();
        improvement.put("cost", cost);
        improvement.put("max", max);
        improvement.put("upkeep", upkeep);
        IMPROVEMENT_DETAILS.put(key, improvement);
        return improvement;
    }
    
    private static String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }    
        StringBuilder result = new StringBuilder();
        String[] words = str.split("\\s");
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
}