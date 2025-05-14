package com.example.shelldemo.spi;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * PostgreSQL database vendor implementation.
 */
public final class PostgreSqlVendor implements DatabaseVendor {
    private static final Logger logger = LogManager.getLogger(PostgreSqlVendor.class);
    
    // Regex pattern to detect PostgreSQL PL/pgSQL blocks
    private static final Pattern PLSQL_PATTERN = Pattern.compile(
        "^\\s*(?:DO\\s+\\$\\$|CREATE\\s+(?:OR\\s+REPLACE\\s+)?(?:FUNCTION|PROCEDURE|TRIGGER))",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public String getVendorName() {
        return "postgresql";
    }

    @Override
    public String buildConnectionUrl(String host, int port, String database, String connectionType) {
        // Using text block for better readability
        return """
            jdbc:postgresql://%s:%d/%s
            """.formatted(host, port > 0 ? port : 5432, database).trim();
    }

    @Override
    public Properties getDefaultConnectionProperties() {
        Properties props = new Properties();
        props.setProperty("ApplicationName", "ShellDemo");
        props.setProperty("preferQueryMode", "extended");
        props.setProperty("autoReconnect", "true");
        return props;
    }

    @Override
    public void initializeConnection(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            // Set session parameters
            stmt.execute("SET search_path TO public");
            stmt.execute("SET TIME ZONE 'UTC'");
            logger.debug("PostgreSQL connection initialized with preferred settings");
        } catch (SQLException e) {
            logger.warn("Failed to initialize PostgreSQL connection settings", e);
        }
    }

    @Override
    public boolean isPLSQL(String sql) {
        if (sql == null || sql.isEmpty()) {
            return false;
        }
        return PLSQL_PATTERN.matcher(sql.trim()).find();
    }
    
    @Override
    public int getDefaultPort() {
        return 5432;
    }
}
