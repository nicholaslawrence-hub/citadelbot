package com.kt.pnwanalyzer.service.impl;

import com.kt.pnwanalyzer.analysis.CityAnalyzer;
import com.kt.pnwanalyzer.model.GameConstants;
import com.kt.pnwanalyzer.model.ResourcePrices;
import com.kt.pnwanalyzer.service.CityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CityServiceImpl implements CityService {

    private static final Logger logger = LoggerFactory.getLogger(CityServiceImpl.class);
    
    @Autowired
    private CityAnalyzer cityAnalyzer;
    
    @Override
    public Map<String, Object> generateOptimalBuild(
            double targetInfra,
            double targetLand,
            String mmrType,
            String optimizationType,
            String domesticPolicy,
            String[] projectsArray) {
        
        logger.info("Generating optimal build for infra: {}, land: {}, MMR: {}, Optimization: {}, Policy: {}",
                targetInfra, targetLand, mmrType, optimizationType, domesticPolicy);
        
        Map<String, Object> nationContext = new HashMap<>();
        
        if (domesticPolicy != null && !domesticPolicy.trim().isEmpty()) {
            nationContext.put("domestic_policy", domesticPolicy);
        } else {
            nationContext.put("domestic_policy", "Open Markets"); 
        }
        
        Map<String, Boolean> activeProjects = new HashMap<>();
        if (projectsArray != null) {
            for (String projectKey : projectsArray) {
                String projectName = getProjectNameFromKey(projectKey);
                if (projectName != null) {
                    activeProjects.put(projectName, true);
                    
                    nationContext.put(projectKey, true);
                }
            }
        }

        Map<String, Double> resourcePrices = getCurrentResourcePrices(); 
        Map<String, Object> buildResult = cityAnalyzer.generateOptimalBuild(
                targetInfra, targetLand, nationContext, resourcePrices, mmrType);
        
        buildResult.put("optimizationType", optimizationType);
        buildResult.put("domesticPolicy", domesticPolicy != null ? domesticPolicy : "None");
        
        if (projectsArray != null && projectsArray.length > 0) {
            buildResult.put("selectedProjects", projectsArray);
        }
        
        return buildResult;
    }
    
    @Override
    public Map<String, String> getAvailableProjects() {
        Map<String, String> projects = new HashMap<>();
        projects.put("mass_irrigation", "Mass Irrigation");
        projects.put("green_technologies", "Green Technologies");
        projects.put("clinical_research_center", "Clinical Research Center");
        projects.put("specialized_police_training", "Specialized Police Training");
        projects.put("telecommunications_satellite", "Telecommunications Satellite");
        projects.put("international_trade_center", "International Trade Center");
        projects.put("bureau_of_domestic_affairs", "Bureau of Domestic Affairs");
        projects.put("government_support_agency", "Government Support Agency");
        
        return projects;
    }
    
    private String getProjectNameFromKey(String projectKey) {
        Map<String, String> projectMap = GameConstants.PROJECT_NAME_MAP;
        return projectMap.getOrDefault(projectKey, null);
    }
    
    private Map<String, Double> getCurrentResourcePrices() {
        return ResourcePrices.getPrices();
    }
}