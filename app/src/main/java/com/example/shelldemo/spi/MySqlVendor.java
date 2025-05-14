package com.example.shelldemo.spi;

import java.util.Properties;
import java.util.regex.Pattern;


/**
 * MySQL database vendor implementation.
 */
public final class MySqlVendor implements DatabaseVendor {

    
    // Pattern to detect MySQL stored procedures and functions
    private static final Pattern DELIMITER_PATTERN = Pattern.compile(
        "^\\s*DELIMITER\\s+\\S+", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PROCEDURE_PATTERN = Pattern.compile(
        "^\\s*CREATE\\s+(DEFINER\\s*=\\S+\\s+)?PROCEDURE", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "^\\s*CREATE\\s+(DEFINER\\s*=\\S+\\s+)?FUNCTION", Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public String getVendorName() {
        return "mysql";
    }

    @Override
    public String buildConnectionUrl(String host, int port, String database, String connectionType) {
        return """
            jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true
            """.formatted(host, port > 0 ? port : 3306, database).trim();
    }

    @Override
    public Properties getDefaultConnectionProperties() {
        Properties props = new Properties();
        props.setProperty("useUnicode", "true");
        props.setProperty("characterEncoding", "UTF-8");
        props.setProperty("serverTimezone", "UTC");
        return props;
    }

    public boolean isPLSQL(String sql) {
        if (sql == null || sql.isEmpty()) {
            return false;
        }
        String trimmedSql = sql.trim();
        return DELIMITER_PATTERN.matcher(trimmedSql).find() ||
               PROCEDURE_PATTERN.matcher(trimmedSql).find() ||
               FUNCTION_PATTERN.matcher(trimmedSql).find();
    }
    
    @Override
    public int getDefaultPort() {
        return 3306;
    }
}
