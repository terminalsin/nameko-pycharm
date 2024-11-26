package com.namecheap.nameko;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DebugLogger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    public static void info(String message) {
        log("INFO", message);
    }
    
    public static void debug(String message) {
        log("DEBUG", message);
    }
    
    public static void error(String message, Throwable e) {
        log("ERROR", message);
        if (e != null) {
            e.printStackTrace(System.out);
        }
    }
    
    public static void warn(String message) {
        log("WARN", message);
    }
    
    private static void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.printf("[%s] [%s] [RpcPlugin] %s%n", timestamp, level, message);
    }
    
    public static void separator() {
        System.out.println("=".repeat(80));
    }
}