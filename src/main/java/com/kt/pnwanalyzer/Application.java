package com.kt.pnwanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.util.concurrent.Executor;

/**
 * Spring Boot application for the Politics & War Analyzer
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class Application {
    
    private static final String OUTPUT_DIR = "output";
    
    public static void main(String[] args) {
        new File(OUTPUT_DIR).mkdirs();
        
        SpringApplication.run(Application.class, args);
        
        System.out.println("Starting Knights Templar Alliance Dashboard...");
        System.out.println("Open your browser and navigate to http://localhost:8080");
    }
    
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("PnWAnalyzer-");
        executor.initialize();
        return executor;
    }
}