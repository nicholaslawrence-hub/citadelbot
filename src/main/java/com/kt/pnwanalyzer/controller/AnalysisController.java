package com.kt.pnwanalyzer.controller;

import com.kt.pnwanalyzer.analysis.NationOptimizer;
import com.kt.pnwanalyzer.data.PnWDataFetcher;
import com.kt.pnwanalyzer.service.AllianceService;
import com.kt.pnwanalyzer.service.CityService;
import com.kt.pnwanalyzer.service.NationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisController.class);
    
    @Autowired
    private PnWDataFetcher dataFetcher;
    
    @Autowired
    private NationOptimizer nationOptimizer;
    
    @Autowired
    private AllianceService allianceService;
    
    @Autowired
    private NationService nationService;
    
    @Autowired
    private CityService cityService;
    
    @GetMapping("/generate")
    public String showGenerateForm(Model model) {
        model.addAttribute("defaultMmrType", "peacetime");
        model.addAttribute("defaultTargetInfra", 2500);
        
        Map<String, String> projects = new HashMap<>();
        projects.put("mass_irrigation", "Mass Irrigation");
        projects.put("green_technologies", "Green Technologies");
        projects.put("clinical_research_center", "Clinical Research Center");
        projects.put("specialized_police_training", "Specialized Police Training");
        projects.put("telecommunications_satellite", "Telecommunications Satellite");
        projects.put("international_trade_center", "International Trade Center");
        
        model.addAttribute("availableProjects", projects);
        
        return "generate_analysis_form";
    }
    
    @PostMapping("/alliance/analyze")
    public String analyzeAlliance(
            @RequestParam("allianceId") int allianceId,
            @RequestParam("mmrType") String mmrType,
            @RequestParam(value = "includeMmrCheck", required = false, defaultValue = "false") boolean includeMmrCheck,
            @RequestParam(value = "includeResourceAnalysis", required = false, defaultValue = "false") boolean includeResourceAnalysis,
            @RequestParam(value = "includeFinancialAnalysis", required = false, defaultValue = "false") boolean includeFinancialAnalysis,
            @RequestParam(value = "includeCityRecommendations", required = false, defaultValue = "false") boolean includeCityRecommendations,
            @RequestParam(value = "skipCache", required = false, defaultValue = "false") boolean skipCache,
            Model model) {
        
        logger.info("Analyzing alliance: {}, MMR Type: {}", allianceId, mmrType);
        
        try {
            // Use the AllianceService to analyze the alliance
            Map<String, Object> analysisResult = allianceService.analyzeAlliance(
                    allianceId, 
                    mmrType, 
                    includeMmrCheck, 
                    includeResourceAnalysis, 
                    includeFinancialAnalysis, 
                    includeCityRecommendations, 
                    skipCache);
            
            // Check if alliance data was found
            if (analysisResult.isEmpty() || analysisResult.get("alliance") == null) {
                model.addAttribute("error", "Alliance not found with ID: " + allianceId);
                return "error";
            }
            
            // Add all analysis results to the model
            for (Map.Entry<String, Object> entry : analysisResult.entrySet()) {
                model.addAttribute(entry.getKey(), entry.getValue());
            }
            
            // Add analysis options to model for the view
            model.addAttribute("includeMmrCheck", includeMmrCheck);
            model.addAttribute("includeResourceAnalysis", includeResourceAnalysis);
            model.addAttribute("includeFinancialAnalysis", includeFinancialAnalysis);
            model.addAttribute("includeCityRecommendations", includeCityRecommendations);
            
            return "alliance-analysis";
        } catch (Exception e) {
            logger.error("Error analyzing alliance", e);
            model.addAttribute("error", "Error analyzing alliance: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Handle nation analysis request
     */
    @PostMapping("/nation/analyze")
    public String analyzeNation(
            @RequestParam("nationId") int nationId,
            @RequestParam("mmrType") String mmrType,
            @RequestParam(value = "optimizationTarget", required = false, defaultValue = "revenue") String optimizationTarget,
            @RequestParam(value = "generateOptimalBuilds", required = false, defaultValue = "false") boolean generateOptimalBuilds,
            @RequestParam(value = "includeResourceAnalysis", required = false, defaultValue = "false") boolean includeResourceAnalysis,
            @RequestParam(value = "includeFinancialAnalysis", required = false, defaultValue = "false") boolean includeFinancialAnalysis,
            @RequestParam(value = "includeProjectRecommendations", required = false, defaultValue = "false") boolean includeProjectRecommendations,
            @RequestParam(value = "skipCache", required = false, defaultValue = "false") boolean skipCache,
            Model model) {
        
        logger.info("Analyzing nation: {}, MMR Type: {}", nationId, mmrType);
        
        try {
            // Use the NationService to analyze the nation
            Map<String, Object> analysisResult = nationService.analyzeNation(
                    nationId, 
                    mmrType, 
                    optimizationTarget, 
                    generateOptimalBuilds, 
                    includeResourceAnalysis, 
                    includeFinancialAnalysis, 
                    includeProjectRecommendations, 
                    skipCache);
            
            // Check if nation data was found
            if (analysisResult.isEmpty() || analysisResult.get("nation") == null) {
                model.addAttribute("error", "Nation not found with ID: " + nationId);
                return "error";
            }
            
            // Add all analysis results to the model
            for (Map.Entry<String, Object> entry : analysisResult.entrySet()) {
                model.addAttribute(entry.getKey(), entry.getValue());
            }
            
            return "nation-analysis";
        } catch (Exception e) {
            logger.error("Error analyzing nation", e);
            model.addAttribute("error", "Error analyzing nation: " + e.getMessage());
            return "error";
        }
    }
    
    @GetMapping("/city/optimize/form")
    public String showOptimizeBuildForm(Model model) {
        // Add default values to the model
        model.addAttribute("defaultMmrType", "peacetime");
        model.addAttribute("defaultTargetInfra", 1750);
        
        // Get available projects from the service
        model.addAttribute("availableProjects", cityService.getAvailableProjects());
        
        return "optimal_build_form";
    }
    
    @PostMapping("/city/optimize")
    public String optimizeCityBuild(
            @RequestParam("targetInfra") double targetInfra,
            @RequestParam("targetLand") double targetLand,
            @RequestParam("mmrType") String mmrType,
            @RequestParam(value = "optimizationType", required = false, defaultValue = "revenue") String optimizationType,
            @RequestParam(value = "domesticPolicy", required = false) String domesticPolicy,
            @RequestParam(value = "projects", required = false) String[] projects,
            Model model) {
        
        logger.info("Generating optimal build for infra: {}, land: {}, MMR: {}", targetInfra, targetLand, mmrType);
        
        try {
            Map<String, Object> buildResult = cityService.generateOptimalBuild(
                    targetInfra, 
                    targetLand, 
                    mmrType, 
                    optimizationType, 
                    domesticPolicy, 
                    projects);
            
            model.addAttribute("buildResult", buildResult);
            
            if (projects != null && projects.length > 0) {
                model.addAttribute("selectedProjects", projects);
            }
            
            return "optimal_build_result";
        } catch (Exception e) {
            logger.error("Error generating optimal build", e);
            model.addAttribute("error", "Error generating optimal build: " + e.getMessage());
            return "error";
        }
    }
}