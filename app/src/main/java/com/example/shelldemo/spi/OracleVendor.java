package com.example.shelldemo.spi;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Oracle database vendor implementation.
 */
public final class OracleVendor implements DatabaseVendor {
    private static final Logger logger = LogManager.getLogger(OracleVendor.class);
    
    // Regex pattern to detect PL/SQL blocks
    private static final Pattern PLSQL_PATTERN = Pattern.compile(
        "^\\s*(?:DECLARE|BEGIN|CREATE\\s+(?:OR\\s+REPLACE\\s+)?(?:FUNCTION|PROCEDURE|PACKAGE|TRIGGER))",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public String getVendorName() {
        return "oracle";
    }

    @Override
    public String buildConnectionUrl(String host, int port, String database, String connectionType) {
        // Using text block for better readability
        if (connectionType != null && "thin-ldap".equalsIgnoreCase(connectionType)) {
            return """
                jdbc:oracle:thin:@ldap://%s:%d/%s,cn=OracleContext,dc=example,dc=com
                """.formatted(host, port > 0 ? port : 389, database).trim();
        } else {
            return """
                jdbc:oracle:thin:@//%s:%d/%s
                """.formatted(host, port > 0 ? port : 1521, database).trim();
        }
    }

    @Override
    public Properties getDefaultConnectionProperties() {
        Properties props = new Properties();
        props.setProperty("oracle.jdbc.fanEnabled", "false");
        props.setProperty("oracle.jdbc.implicitStatementCacheSize", "20");
        props.setProperty("oracle.jdbc.maxCachedBufferSize", "100000");
        props.setProperty("defaultRowPrefetch", "100");
        return props;
    }

    @Override
    public void initializeConnection(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            // Set session parameters
            stmt.execute("ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD'");
            stmt.execute("ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'YYYY-MM-DD HH24:MI:SS.FF'");
            logger.debug("Oracle connection initialized with preferred NLS settings");
        } catch (SQLException e) {
            logger.warn("Failed to initialize Oracle connection settings", e);
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
        return 1521;
    }
}
