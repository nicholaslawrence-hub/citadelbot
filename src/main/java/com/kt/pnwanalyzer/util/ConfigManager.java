package com.kt.pnwanalyzer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_FILE = "config.ini";
    private static ConfigManager instance;
    
    private Properties properties;
    private String apiKey = "";
    
    private ConfigManager() {
        properties = new Properties();
        load();
    }
    
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    public void load() {
        Path configPath = Paths.get(CONFIG_FILE);
        
        if (Files.exists(configPath)) {
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                properties.load(fis);
                
                if (properties.containsKey("API.key") && !properties.getProperty("API.key").trim().isEmpty()) {
                    apiKey = properties.getProperty("API.key").trim();
                    logger.info("Using API key from {}", CONFIG_FILE);
                }
            } catch (IOException e) {
                logger.error("Error reading configuration file: {}", e.getMessage());
            }
        } else {
            logger.warn("WARNING: Configuration file '{}' not found. Creating default.", CONFIG_FILE);
            createDefaultConfig();
        }
    }
    
    private void createDefaultConfig() {
        properties.setProperty("API.key", "");
        
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "PnW Analyzer Configuration");
            logger.info("Created default configuration file: {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("Error creating config file {}: {}", CONFIG_FILE, e.getMessage());
        }
    }
    
    public void save() {
        properties.setProperty("API.key", apiKey);
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "PnW Analyzer Configuration");
            logger.info("Configuration saved to {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("Error saving config file {}: {}", CONFIG_FILE, e.getMessage());
        }
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        save();
    }
}