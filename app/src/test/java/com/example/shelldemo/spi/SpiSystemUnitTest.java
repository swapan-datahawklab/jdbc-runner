package com.example.shelldemo.spi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests for the SPI system with sealed interface hierarchy and service loading
 */
class SpiSystemUnitTest {

    @Test
    void testServiceDiscovery() {
        Collection<DatabaseVendor> vendors = DatabaseVendorRegistry.getAllVendors();
        assertNotNull(vendors, "The vendor list should not be null");
        assertFalse(vendors.isEmpty(), "The vendor list should not be empty");
        
        // Should have our 4 standard vendors
        assertEquals(4, vendors.size(), "Should have exactly 4 vendor implementations");
        
        // Test vendor name retrieval
        Set<String> vendorNames = DatabaseVendorRegistry.getVendorNames();
        assertTrue(vendorNames.contains("oracle"), "Oracle vendor should be registered");
        assertTrue(vendorNames.contains("postgresql"), "PostgreSQL vendor should be registered");
        assertTrue(vendorNames.contains("mysql"), "MySQL vendor should be registered");
        assertTrue(vendorNames.contains("sqlserver"), "SQL Server vendor should be registered");
    }
    
    @Test
    void testVendorLookup() {
        Optional<DatabaseVendor> oracleVendor = DatabaseVendorRegistry.getVendor("oracle");
        assertTrue(oracleVendor.isPresent(), "Oracle vendor should be found");
        assertEquals("oracle", oracleVendor.get().getVendorName(), "Vendor name should match");
        
        Optional<DatabaseVendor> postgresVendor = DatabaseVendorRegistry.getVendor("postgresql");
        assertTrue(postgresVendor.isPresent(), "PostgreSQL vendor should be found");
        assertEquals("postgresql", postgresVendor.get().getVendorName(), "Vendor name should match");
        
        Optional<DatabaseVendor> notFoundVendor = DatabaseVendorRegistry.getVendor("nonexistent");
        assertFalse(notFoundVendor.isPresent(), "Nonexistent vendor should not be found");
    }
    
    @Test
    void testPatternMatching() {
        Optional<DatabaseVendor> oracleVendor = DatabaseVendorRegistry.getVendor("oracle");
        assertTrue(oracleVendor.isPresent(), "Oracle vendor should be found");
        
        // Test Java 21 pattern matching with instanceof
        assertTrue(oracleVendor.get() instanceof OracleVendor, "Oracle vendor should be an instance of OracleVendor");
        
        if (oracleVendor.get() instanceof OracleVendor oracle) {
            // This uses pattern variable binding from Java 21
            assertTrue(oracle.isPLSQL("BEGIN\n  NULL;\nEND;"), "Should identify valid PL/SQL");
            assertFalse(oracle.isPLSQL("SELECT * FROM emp"), "Should not identify SQL as PL/SQL");
        } else {
            fail("Pattern matching failed");
        }
        
        // Test switch pattern matching from Java 21
        DatabaseVendor vendor = oracleVendor.get();
        int expectedPort = switch(vendor) {
            case OracleVendor o -> 1521;
            case PostgreSqlVendor p -> 5432;
            case MySqlVendor m -> 3306;
            case SqlServerVendor s -> 1433;
        };
        
        assertEquals(1521, expectedPort, "Oracle default port should be 1521");
        assertEquals(1521, vendor.getDefaultPort(), "Oracle default port from method should be 1521");
    }
    
    @Test
    void testExhaustivePatternMatchingWithSealed() {
        Optional<DatabaseVendor> vendor = DatabaseVendorRegistry.getVendor("postgresql");
        assertTrue(vendor.isPresent(), "PostgreSQL vendor should be found");
        
        // This switch must handle all possible subtypes due to the sealed interface
        String vendorCategory = switch(vendor.get()) {
            case OracleVendor o -> "Commercial Oracle Database";
            case PostgreSqlVendor p -> "Open Source PostgreSQL Database";
            case MySqlVendor m -> "Open Source MySQL Database";
            case SqlServerVendor s -> "Commercial SQL Server Database";
            // No default needed because sealed interfaces guarantee exhaustive pattern matching
        };
        
        assertEquals("Open Source PostgreSQL Database", vendorCategory);
    }
}
