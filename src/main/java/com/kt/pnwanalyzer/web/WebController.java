package com.kt.pnwanalyzer.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.pnwanalyzer.analysis.CityAnalyzer;
import com.kt.pnwanalyzer.analysis.NationOptimizer;
import com.kt.pnwanalyzer.data.PnWDataFetcher;
import com.kt.pnwanalyzer.model.GameConstants;
import com.kt.pnwanalyzer.util.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Controller
public class WebController {
    private static final Logger logger = LoggerFactory.getLogger(WebController.class);
    private static final int KNIGHTS_TEMPLAR_ID = 4124;
    
    @Value("${app.output-dir:output}")
    private String outputDir;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    
    /**
     * Home page
     */
    @GetMapping("/")
    public String index(Model model) {
        LocalDateTime now = LocalDateTime.now();
        String jsonReportPattern = String.format("analysis_data_%d_*.json", KNIGHTS_TEMPLAR_ID);
        String latestJsonPath = getLatestJsonFile(jsonReportPattern);
        
        boolean regenerateReport = false;
        if (latestJsonPath == null) {
            regenerateReport = true;
            model.addAttribute("message", "No existing analysis data found for default alliance. Generating new data...");
        } else {
            Path path = Paths.get(latestJsonPath);
            try {
                long lastModifiedMillis = Files.getLastModifiedTime(path).toMillis();
                long reportAgeSeconds = (now.toEpochSecond(ZoneOffset.UTC) * 1000 - lastModifiedMillis) / 1000;
                
                if (reportAgeSeconds > 3600) { // Older than 1 hour
                    regenerateReport = true;
                    model.addAttribute("message", "Existing analysis data is older than 1 hour. Generating new data...");
                }
            } catch (IOException e) {
                logger.error("Error checking file modification time: {}", e.getMessage());
            }
        }
        
        if (regenerateReport) {
            CompletableFuture.runAsync(() -> {
                boolean success = runAnalyzerProcess(String.valueOf(KNIGHTS_TEMPLAR_ID), "analyze");
                if (!success) {
                    logger.warn("Failed to regenerate default alliance data");
                }
            }, executorService);
            
            // Re-fetch after starting generation
            latestJsonPath = getLatestJsonFile(jsonReportPattern);
        }
        
        model.addAttribute("latest_analysis_filename", 
                latestJsonPath != null ? new File(latestJsonPath).getName() : null);
        model.addAttribute("default_alliance_id", KNIGHTS_TEMPLAR_ID);
        model.addAttribute("now", now);
        
        return "index";
    }
    
    /**
     * Page to generate alliance analysis
     */
    @GetMapping("/generate_alliance_analysis")
    public String generateAllianceAnalysisForm(Model model) {
        model.addAttribute("mmr_options", GameConstants.MMR_TYPES.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get("name"))));
        model.addAttribute("now", LocalDateTime.now());
        return "generate_analysis_form";
    }
    
    /**
     * Process alliance analysis form
     */
    @PostMapping("/generate_alliance_analysis")
    public String processAllianceAnalysisForm(
            @RequestParam("alliance_id") Integer allianceId,
            @RequestParam(value = "mmr_type", defaultValue = "peacetime") String mmrType,
            RedirectAttributes redirectAttributes) {
        
        if (allianceId == null) {
            redirectAttributes.addFlashAttribute("error", "Alliance ID is required.");
            return "redirect:/generate_alliance_analysis";
        }
        
        logger.info("Alliance analysis generation triggered for Alliance ID: {}, MMR: {}", allianceId, mmrType);
        
        boolean success = runAnalyzerProcess(
                String.valueOf(allianceId), 
                "analyze", 
                "--mmr_type", mmrType);
        
        if (success) {
            String jsonPattern = String.format("analysis_data_%d_*.json", allianceId);
            String latestJson = getLatestJsonFile(jsonPattern);
            
            if (latestJson != null) {
                redirectAttributes.addFlashAttribute("message", 
                        String.format("Successfully generated analysis data for Alliance ID %d.", allianceId));
                return "redirect:/view_alliance_analysis/" + new File(latestJson).getName();
            } else {
                redirectAttributes.addFlashAttribute("warning", 
                        "Analysis data generated, but could not find the output JSON file.");
            }
        }
        
        return "redirect:/generate_alliance_analysis";
    }
    
    /**
     * View alliance analysis
     */
    @GetMapping("/view_alliance_analysis/{filename}")
    public String viewAllianceAnalysisJson(@PathVariable String filename, Model model) {
        if (filename.contains("..") || !filename.startsWith("analysis_data_") || !filename.endsWith(".json")) {
            model.addAttribute("message", "Invalid analysis data filename.");
            return "error";
        }
        
        File file = new File(outputDir, filename);
        if (!file.exists()) {
            model.addAttribute("message", "Analysis data file not found.");
            return "error";
        }
        
        model.addAttribute("json_filename", filename);
        model.addAttribute("now", LocalDateTime.now());
        
        return "alliance_report_view";
    }
    
    /**
     * Page to generate nation optimal build
     */
    @GetMapping("/generate_nation_optimal_build")
    public String generateNationOptimalBuildForm(Model model) {
        model.addAttribute("mmr_options", GameConstants.MMR_TYPES.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get("name"))));
        model.addAttribute("default_alliance_id", KNIGHTS_TEMPLAR_ID);
        model.addAttribute("now", LocalDateTime.now());
        
        return "generate_optimal_build_form";
    }
    
    /**
     * Process nation optimal build form
     */
    @PostMapping("/generate_nation_optimal_build")
    public String processNationOptimalBuildForm(
            @RequestParam("alliance_id_context") Integer allianceId,
            @RequestParam("nation_id") Integer nationId,
            @RequestParam(value = "mmr_type", defaultValue = "peacetime") String mmrType,
            @RequestParam(value = "target_infra", required = false) Double targetInfra,
            @RequestParam(value = "target_land", required = false) Double targetLand,
            RedirectAttributes redirectAttributes) {
        
        if (nationId == null || allianceId == null) {
            redirectAttributes.addFlashAttribute("error", "Nation ID and Alliance ID (for context) are required.");
            return "redirect:/generate_nation_optimal_build";
        }
        
        logger.info("Optimal build generation for Nation ID: {} (Alliance: {}), MMR: {}, Infra: {}, Land: {}", 
                nationId, allianceId, mmrType, targetInfra, targetLand);
        
        List<String> args = new java.util.ArrayList<>(Arrays.asList(
                String.valueOf(allianceId), 
                "optimal_build", 
                "--nation_id", String.valueOf(nationId), 
                "--mmr_type", mmrType));
        
        if (targetInfra != null) {
            args.add("--target_infra");
            args.add(String.valueOf(targetInfra));
        }
        
        if (targetLand != null) {
            args.add("--target_land");
            args.add(String.valueOf(targetLand));
        }
        
        boolean success = runAnalyzerProcess(args.toArray(new String[0]));
        
        if (success) {
            String jsonPattern = String.format("optimal_build_data_nation_%d_*.json", nationId);
            String latestJson = getLatestJsonFile(jsonPattern);
            
            if (latestJson != null) {
                redirectAttributes.addFlashAttribute("message", 
                        String.format("Successfully generated optimal build data for Nation ID %d.", nationId));
                return "redirect:/view_optimal_build/" + new File(latestJson).getName();
            } else {
                redirectAttributes.addFlashAttribute("warning", 
                        "Optimal build data generated, but JSON file not found.");
            }
        }
        
        return "redirect:/generate_nation_optimal_build";
    }
    
    /**
     * View optimal build
     */
    @GetMapping("/view_optimal_build/{filename}")
    public String viewOptimalBuildJson(@PathVariable String filename, Model model) {
        if (filename.contains("..") || !filename.startsWith("optimal_build_data_") || !filename.endsWith(".json")) {
            model.addAttribute("message", "Invalid optimal build data filename.");
            return "error";
        }
        
        File file = new File(outputDir, filename);
        if (!file.exists()) {
            model.addAttribute("message", "Optimal build data file not found.");
            return "error";
        }
        
        model.addAttribute("json_filename", filename);
        model.addAttribute("now", LocalDateTime.now());
        
        return "optimal_build_view";
    }
    
    /**
     * One-click optimal template API endpoint
     */
    @GetMapping("/one_click_optimal_template/{allianceId}/{nationId}")
    @ResponseBody
    public ResponseEntity<?> oneClickOptimalTemplate(
            @PathVariable int allianceId,
            @PathVariable int nationId,
            @RequestParam(value = "mmr_type", defaultValue = "peacetime") String mmrType) {
        
        String jsonPattern = String.format("optimal_build_data_nation_%d_*.json", nationId);
        
        // Always regenerate for one-click
        logger.info("One-click: Regenerating optimal build for Nation ID {} (Alliance: {})", nationId, allianceId);
        
        boolean success = runAnalyzerProcess(
                String.valueOf(allianceId),
                "optimal_build",
                "--nation_id", String.valueOf(nationId),
                "--mmr_type", mmrType);
        
        if (!success) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate optimal build data for template."));
        }
        
        String latestJsonPath = getLatestJsonFile(jsonPattern);
        if (latestJsonPath == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Optimal build data file not found after generation attempt."));
        }
        
        try {
            JsonNode data = objectMapper.readTree(new File(latestJsonPath));
            JsonNode template = data.get("template_format");
            
            if (template == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "No 'template_format' found in optimal build data."));
            }
            
            return ResponseEntity.ok(Map.of("success", true, "template", template));
        } catch (IOException e) {
            logger.error("Error serving one-click template for nation {}: {}", nationId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not process optimal build data: " + e.getMessage()));
        }
    }
    
    /**
     * Serve data file
     */
    @GetMapping("/serve_data_file/{filename}")
    @ResponseBody
    public ResponseEntity<?> serveDataFile(@PathVariable String filename) {
        if (filename.contains("..") || filename.startsWith("/")) {
            return ResponseEntity.badRequest().body("Invalid filename");
        }
        
        if (!filename.endsWith(".json")) {
            return ResponseEntity.badRequest().body("Invalid file type requested");
        }
        
        File file = new File(outputDir, filename);
        if (!file.getAbsolutePath().startsWith(new File(outputDir).getAbsolutePath())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(data);
        } catch (IOException e) {
            logger.error("Error serving data file {}: {}", filename, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }
    }
    
    /**
     * Settings page
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        String apiKey = ConfigManager.getInstance().getApiKey();
        String maskedKey = apiKey.length() > 8 
                ? apiKey.substring(0, 4) + "*".repeat(apiKey.length() - 8) + apiKey.substring(apiKey.length() - 4)
                : "********";
        
        model.addAttribute("masked_key", maskedKey);
        model.addAttribute("current_api_key_present", !apiKey.isEmpty());
        model.addAttribute("now", LocalDateTime.now());
        
        return "settings";
    }
    
    /**
     * Process settings form
     */
    @PostMapping("/settings")
    public String processSettings(
            @RequestParam("api_key") String apiKey,
            RedirectAttributes redirectAttributes) {
        
        try {
            ConfigManager.getInstance().setApiKey(apiKey.trim());
            redirectAttributes.addFlashAttribute("message", "API Key saved successfully!");
        } catch (Exception e) {
            logger.error("Error saving API Key: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error saving API Key: " + e.getMessage());
        }
        
        return "redirect:/settings";
    }
    
    // Helper methods
    
    /**
     * Find the most recent JSON file matching a pattern in the output directory
     */
    private String getLatestJsonFile(String pattern) {
        try {
            File outputDirFile = new File(outputDir);
            if (!outputDirFile.exists()) {
                outputDirFile.mkdirs();
            }
            
            File[] matchingFiles = outputDirFile.listFiles((dir, name) -> name.matches(pattern.replace("*", ".*")));
            if (matchingFiles == null || matchingFiles.length == 0) {
                return null;
            }
            
            // Find the newest file
            return Arrays.stream(matchingFiles)
                    .max((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()))
                    .map(File::getAbsolutePath)
                    .orElse(null);
        } catch (Exception e) {
            logger.error("Error finding latest JSON file for pattern {}: {}", pattern, e.getMessage());
            return null;
        }
    }
    
    private boolean runAnalyzerProcess(String... args) {
        try {
            logger.info("Running analyzer with args: {}", Arrays.toString(args));
            
            if (args.length < 2) {
                logger.error("Not enough arguments for analyzer");
                return false;
            }
            
            int allianceId = Integer.parseInt(args[0]);
            String command = args[1];
            
            // Parse options
            Map<String, String> options = new HashMap<>();
            for (int i = 2; i < args.length; i += 2) {
                if (i + 1 < args.length) {
                    String key = args[i];
                    if (key.startsWith("--")) {
                        key = key.substring(2);
                    }
                    options.put(key, args[i + 1]);
                }
            }
            
            String mmrType = options.getOrDefault("mmr_type", "peacetime");
            
            PnWDataFetcher dataFetcher = new PnWDataFetcher();
            CityAnalyzer cityAnalyzer = new CityAnalyzer();
            NationOptimizer nationOptimizer = new NationOptimizer(cityAnalyzer);
            
            File outputDirFile = new File(outputDir);
            if (!outputDirFile.exists()) {
                outputDirFile.mkdirs();
            }
            
            String timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            if ("analyze".equals(command)) {
                
                Map<String, Object> apiData = dataFetcher.getAllDataForAnalysis(allianceId);
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> nationsList = (List<Map<String, Object>>) apiData.get("nations");
                
                if (nationsList == null) {
                    logger.error("Failed to fetch alliance data");
                    return false;
                }
                
                List<Map<String, Object>> nationAnalysisResults = nationsList.stream()
                        .map(nation -> nationOptimizer.analyzeNation(nation, 
                                            (Map<String, Double>) apiData.get("resourcePrices"), 
                                            mmrType))
                        .filter(result -> !result.isEmpty())
                        .collect(Collectors.toList());
                
                Map<String, Object> outputData = new HashMap<>();
                outputData.put("timestamp", timestamp);
                outputData.put("alliance_id", allianceId);
                outputData.put("alliance_data", apiData.get("alliance"));
                outputData.put("resource_prices", apiData.get("resourcePrices"));
                outputData.put("mmr_type_used", mmrType);
                outputData.put("nation_analyses", nationAnalysisResults);
                
                String outputJsonPath = outputDir + "/analysis_data_" + allianceId + "_" + timestamp + ".json";
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputJsonPath), outputData);
                
                logger.info("Analysis JSON saved to {}", outputJsonPath);
                return true;
                
            } else if ("optimal_build".equals(command)) {
                // Optimal build
                Integer nationId = options.containsKey("nation_id") ? 
                        Integer.parseInt(options.get("nation_id")) : null;
                Double targetInfra = options.containsKey("target_infra") ? 
                        Double.parseDouble(options.get("target_infra")) : 1000.0;
                Double targetLand = options.containsKey("target_land") ? 
                        Double.parseDouble(options.get("target_land")) : 1000.0;
                
                Map<String, Object> apiData = dataFetcher.getAllDataForAnalysis(allianceId);
                
                if (apiData.isEmpty()) {
                    logger.error("Failed to fetch alliance data or resource prices for optimal build");
                    return false;
                }
                
                Map<String, Object> nationContext = null;
                if (nationId != null) {
                    logger.info("Fetching Nation ID {} for optimal build project context", nationId);
                    nationContext = objectMapper.convertValue(
                            dataFetcher.getNationDetails(nationId), 
                            Map.class);
                }
                
                Map<String, Object> optimalBuildOutput = cityAnalyzer.generateOptimalBuild(
                        targetInfra,
                        targetLand,
                        nationContext,
                        (Map<String, Double>) apiData.get("resourcePrices"),
                        mmrType);
                
                String buildFileLabel = nationId != null ? 
                        "nation_" + nationId : "generic_infra_" + (int) targetInfra.doubleValue();
                
                String outputJsonPath = outputDir + "/optimal_build_data_" + buildFileLabel + "_" + timestamp + ".json";
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputJsonPath), optimalBuildOutput);
                
                logger.info("Optimal build JSON saved to {}", outputJsonPath);
                return true;
            } else {
                logger.error("Unknown command: {}", command);
                return false;
            }
        } catch (Exception e) {
            logger.error("Exception running analyzer: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}