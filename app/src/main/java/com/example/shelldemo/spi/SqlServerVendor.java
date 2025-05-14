package com.example.shelldemo.spi;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SQL Server database vendor implementation.
 */
public final class SqlServerVendor implements DatabaseVendor {
    private static final Logger logger = LogManager.getLogger(SqlServerVendor.class);
    
    // Pattern to detect T-SQL procedure and trigger syntax
    private static final Pattern PLSQL_PATTERN = Pattern.compile(
        "^\\s*CREATE\\s+(?:OR\\s+ALTER\\s+)?(?:PROCEDURE|PROC|TRIGGER|FUNCTION)",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public String getVendorName() {
        return "sqlserver";
    }

    @Override
    public String buildConnectionUrl(String host, int port, String database, String connectionType) {
        return """
            jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false
            """.formatted(host, port > 0 ? port : 1433, database).trim();
    }

    @Override
    public Properties getDefaultConnectionProperties() {
        Properties props = new Properties();
        props.setProperty("applicationName", "ShellDemo");
        props.setProperty("sendTimeAsDatetime", "false");
        return props;
    }

    @Override
    public void initializeConnection(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            // Set session parameters
            stmt.execute("SET LANGUAGE us_english");
            stmt.execute("SET DATEFORMAT ymd");
            logger.debug("SQL Server connection initialized with preferred settings");
        } catch (SQLException e) {
            logger.warn("Failed to initialize SQL Server connection settings", e);
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
        return 1433;
    }
}
