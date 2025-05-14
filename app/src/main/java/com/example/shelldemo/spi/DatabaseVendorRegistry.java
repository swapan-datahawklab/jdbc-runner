package com.example.shelldemo.spi;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Registry for database vendors using Java's ServiceLoader mechanism.
 * This enables a pluggable architecture for database support.
 */
public final class DatabaseVendorRegistry {
    private static final Logger logger = LogManager.getLogger(DatabaseVendorRegistry.class);
    private static final Map<String, DatabaseVendor> VENDORS = loadVendors();
    
    private DatabaseVendorRegistry() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Load vendor implementations using ServiceLoader.
     */
    private static Map<String, DatabaseVendor> loadVendors() {
        Map<String, DatabaseVendor> vendors = new HashMap<>();
        
        // Load built-in vendors
        vendors.put("oracle", new OracleVendor());
        vendors.put("postgresql", new PostgreSqlVendor());
        vendors.put("mysql", new MySqlVendor());
        vendors.put("sqlserver", new SqlServerVendor());
        
        // Load dynamically registered vendors using ServiceLoader
        ServiceLoader.load(DatabaseVendor.class).forEach(vendor -> {
            String name = vendor.getVendorName().toLowerCase();
            vendors.put(name, vendor);
            logger.info("Loaded database vendor: {} ({})", name, vendor.getClass().getSimpleName());
        });
        
        return Map.copyOf(vendors); // Return immutable map
    }
    
    /**
     * Gets a database vendor by name.
     * 
     * @param name The vendor name (e.g., "oracle", "postgresql")
     * @return An Optional containing the vendor if found
     */
    public static Optional<DatabaseVendor> getVendor(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(VENDORS.get(name.toLowerCase().trim()));
    }
    
    /**
     * Gets all registered vendors.
     * 
     * @return A collection of all registered vendors
     */
    public static Collection<DatabaseVendor> getAllVendors() {
        return VENDORS.values();
    }
    
    /**
     * Checks if a vendor is supported.
     * 
     * @param name Vendor name to check
     * @return true if the vendor is supported
     */
    public static boolean isSupported(String name) {
        return getVendor(name).isPresent();
    }
    
    /**
     * Gets a database vendor by name or throws an exception if not found.
     * 
     * @param name The vendor name
     * @return The DatabaseVendor instance
     * @throws IllegalArgumentException if vendor is not supported
     */
    public static DatabaseVendor getVendorOrThrow(String name) {
        return getVendor(name).orElseThrow(() -> 
            new IllegalArgumentException("Unsupported database vendor: " + name));
    }

    /**
     * Gets all registered vendor names.
     * 
     * @return Set of all vendor names
     */
    public static java.util.Set<String> getVendorNames() {
        return VENDORS.keySet();
    }
}
