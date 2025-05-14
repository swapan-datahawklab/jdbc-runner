package com.example.shelldemo.util;

import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

/**
 * Utility class for secure logging in database operations.
 * Handles sensitive information to ensure it's not accidentally logged.
 */
public final class LoggingUtils {
    
    private static final String[] SENSITIVE_PROPERTY_KEYS = {
        "password", "passwd", "pwd", "secret", "token", "credentials", "key"
    };
    
    private LoggingUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Logs database connection information without exposing sensitive data.
     * 
     * @param logger The logger to use
     * @param url The JDBC URL (may contain sensitive information)
     * @param props The connection properties (may contain sensitive information)
     */
    public static void logSensitiveConnectionInfo(Logger logger, String url, Properties props) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        
        // Log connection URL with any potential password parts masked
        String maskedUrl = maskSensitiveUrlParts(url);
        logger.debug("Connection URL: {}", maskedUrl);
        
        // Log connection properties without sensitive values
        if (props != null && !props.isEmpty()) {
            String maskedProps = props.stringPropertyNames().stream()
                .filter(key -> !isSensitiveKey(key))
                .map(key -> key + "=" + props.getProperty(key))
                .collect(Collectors.joining(", "));
            
            int sensitiveCount = (int) props.stringPropertyNames().stream()
                .filter(LoggingUtils::isSensitiveKey)
                .count();
                
            logger.debug("Connection properties: {} (plus {} sensitive properties)", 
                maskedProps, sensitiveCount);
        }
    }
    
    /**
     * Checks if a property key contains sensitive information that should not be logged.
     * 
     * @param key The property key to check
     * @return true if the key contains sensitive information
     */
    public static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        
        String lowerKey = key.toLowerCase();
        for (String sensitiveKey : SENSITIVE_PROPERTY_KEYS) {
            if (lowerKey.contains(sensitiveKey)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Masks sensitive parts in database connection URLs.
     * 
     * @param url The JDBC URL to mask
     * @return The masked URL
     */
    public static String maskSensitiveUrlParts(String url) {
        if (url == null) {
            return "null";
        }
        
        // Common patterns for passwords in URLs
        return url.replaceAll("(?i)password=([^&;]*)", "password=*****")
                 .replaceAll("(?i)pwd=([^&;]*)", "pwd=*****")
                 .replaceAll("(?i):([^:/@]+)@", ":*****@");
    }
}
