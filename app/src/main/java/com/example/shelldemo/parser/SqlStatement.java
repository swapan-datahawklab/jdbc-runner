package com.example.shelldemo.parser;

/**
 * Represents different types of SQL statements with a sealed hierarchy.
 * This allows exhaustive pattern matching in switch expressions.
 */
public sealed interface SqlStatement
    permits SqlStatement.RegularStatement, SqlStatement.PlSqlBlock {
    
    /**
     * Returns the SQL statement text.
     */
    String getText();
    
    /**
     * Regular SQL statement ending with a semicolon.
     */
    record RegularStatement(String text) implements SqlStatement {
        @Override
        public String getText() {
            return text;
        }
    }
    
    /**
     * PL/SQL block terminated with a forward slash.
     */
    record PlSqlBlock(String text) implements SqlStatement {
        @Override
        public String getText() {
            return text;
        }
    }
}
