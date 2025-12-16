package com.github.cfogrady.dim.modifier.utils;

import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FlashromUtil {
    
    public static List<String> listProgrammers() throws IOException {
        return executeFlashromCommand("--list-programmers");
    }
    
    public static List<String> detectChip() throws IOException {
        return executeFlashromCommand("--detect");
    }
    
    public static List<String> readFlash(String outputFile) throws IOException {
        return executeFlashromCommand("-r", outputFile);
    }
    
    public static List<String> writeFlash(String inputFile) throws IOException {
        return executeFlashromCommand("-w", inputFile);
    }
    
    public static List<String> verifyFlash(String inputFile) throws IOException {
        return executeFlashromCommand("-v", inputFile);
    }
    
    private static List<String> executeFlashromCommand(String... args) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("flashrom");
        command.addAll(List.of(args));
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        List<String> output = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
                log.info("Flashrom output: {}", line);
            }
        }
        
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Flashrom command failed with exit code: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Flashrom command interrupted", e);
        }
        
        return output;
    }
} 