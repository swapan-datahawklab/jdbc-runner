package com.example.shelldemo.spi;

import java.sql.Connection;
import java.util.Properties;

/**
 * Sealed interface for database vendor implementations.
 * Uses Java 21 sealed classes to restrict implementations.
 */
public sealed interface DatabaseVendor 
    permits OracleVendor, PostgreSqlVendor, MySqlVendor, SqlServerVendor {
    
    /**
     * Gets the canonical name of this database vendor.
     */
    String getVendorName();
    
    /**
     * Builds a connection URL for this database type.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param database Database/service name 
     * @param connectionType Optional connection type (e.g., "thin" for Oracle)
     * @return JDBC connection URL
     */
    String buildConnectionUrl(String host, int port, String database, String connectionType);
    
    /**
     * Gets default connection properties for this vendor.
     */
    Properties getDefaultConnectionProperties();
    
    /**
     * Execute vendor-specific initialization on a new connection.
     * 
     * @param connection The JDBC connection to initialize
     */
    default void initializeConnection(Connection connection) {
        // Default implementation does nothing
    }
    
    /**
     * Tests if a SQL statement is vendor-specific PL/SQL.
     * 
     * @param sql The SQL statement to check
     * @return true if the statement is PL/SQL
     */
    boolean isPLSQL(String sql);
    
    /**
     * Gets the default port for this database vendor.
     */
    int getDefaultPort();
}
