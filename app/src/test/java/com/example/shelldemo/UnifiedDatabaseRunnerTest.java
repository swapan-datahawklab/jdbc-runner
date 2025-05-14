package com.example.shelldemo;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.net.URL;
import java.net.URISyntaxException;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import com.example.shelldemo.testutil.BaseDbTest;
import com.example.shelldemo.testutil.NoStackTraceWatcher;

@DisplayName("UnifiedDatabaseRunner Integration Tests")
@ExtendWith(NoStackTraceWatcher.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UnifiedDatabaseRunnerTest extends BaseDbTest {

    @BeforeEach
    void ensureTestTableExists() throws Exception {
        String dropTable = "BEGIN EXECUTE IMMEDIATE 'DROP TABLE test_table'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;";
        String createTable = "CREATE TABLE test_table (id NUMBER PRIMARY KEY, name VARCHAR2(50))";
        Path dropFile = createSqlFile("drop_test_table.sql", dropTable);
        Path createFile = createSqlFile("create_test_table.sql", createTable);
        executeCommand(dropFile);
        executeCommand(createFile);
    }

    @Test
    @Order(1)
    @DisplayName("Should verify database connection")
    void testDatabaseConnection() throws Exception {
        // Arrange
        Path sqlFile = createSqlFile("connection_test.sql", "SELECT 1 FROM DUAL");

        // Act
        ExecutionResult result = executeCommand(sqlFile);

        assertAll(
            () -> assertEquals(0, result.exitCode(), "Connection should be successful"),
            () -> assertNoErrors(result.output())
        );
    }

    @Test
    @Order(2)
    @DisplayName("Should handle invalid SQL gracefully")
    void testInvalidSql() throws Exception {
        // Arrange
        Path sqlFile = createSqlFile("invalid.sql", "SELECT * FROM nonexistent_table");

        // Act
        ExecutionResult result = executeCommand(sqlFile);

        // Assert - only check for non-zero exit code since this is an expected error
        assertNotEquals(0, result.exitCode(), "Should fail with non-zero exit code");
    }

    @Test
    @Order(3)
    @DisplayName("Should execute complex query with joins")
    void testComplexQuery() throws Exception {
        // Arrange
        String complexQuery = 
            "SELECT id, name FROM test_table WHERE id = 1";
        
        Path sqlFile = createSqlFile("hr_complex_query.sql", complexQuery);

        // Act
        ExecutionResult result = executeCommand(sqlFile);

        assertAll(
            () -> assertEquals(0, result.exitCode(), "Should succeed with zero exit code"),
            () -> assertTrue(
                result.logOutput().toUpperCase().contains("ID") && 
                result.logOutput().toUpperCase().contains("NAME"), 
                "Log should contain ID and NAME columns"
            )
        );
    }

    @Test
    @Order(4)
    @DisplayName("Should execute multiple SQL statements")
    void testMultipleStatements() throws Exception {
        // Arrange
        String multipleQueries = String.join("\n",
            "SELECT employee_id, first_name, last_name FROM employees WHERE rownum <= 3;",
            "SELECT department_id, department_name FROM departments WHERE rownum <= 3;"
        );
        Path sqlFile = createSqlFile("test_script.sql", multipleQueries);

        // Act
        ExecutionResult result = executeCommand(sqlFile);

        // Assert (use custom assertion)
        assertSuccessfulExecution(result, output -> 
            output.contains("EMPLOYEE_ID") && output.contains("DEPARTMENT_ID")
        );
    }

    @Test
    @Order(5)
    @DisplayName("Should execute DDL script")
    void testDdlScript() throws URISyntaxException {
        // Arrange
        Path sqlScriptPath = tempDir.resolve("test_ddl.sql");
        URL resourceUrl = getClass().getClassLoader().getResource("sql/create_employee_info_proc.sql");
        if (resourceUrl == null) {
            fail("Test SQL file not found in classpath");
        }
        
        Path sourcePath = Path.of(resourceUrl.toURI());
        copyAndVerifyFile(sourcePath, sqlScriptPath);

        // Act
        ExecutionResult result = executeCommand(sqlScriptPath);

        // Assert
        assertSuccessfulExecution(result, output -> {
            // Check for successful execution indicators
            return output.contains("Successfully parsed 3 SQL statements") &&
                   output.contains("Statement affected") &&
                   output.contains("Database connection closed successfully");
        });
    }

    @Test
    @Order(6)
    @DisplayName("Should execute DML script transactionally when requested")
    void testDmlScriptTransactional() throws Exception {
        String dmlScript = String.join("\n",
            "INSERT INTO test_table (id, name) VALUES (1, 'A');",
            "INSERT INTO test_table (id, name) VALUES (2, 'B');"
        );
        Path sqlFile = createSqlFile("dml_script.sql", dmlScript);

        // Prepare args with --transactional
        String[] args = new String[] {
            sqlFile.toString(),
            "-t", "oracle",
            "--connection-type", "thin",
            "-H", ORACLE_HOST,
            "-P", String.valueOf(ORACLE_PORT),
            "-u", ORACLE_USERNAME,
            "-p", ORACLE_PASSWORD,
            "-d", ORACLE_DATABASE,
            "--stop-on-error",
            "--print-statements",
            "--transactional"
        };

        // Act
        ExecutionResult result = executeCommandWithArgs(args);

        // Assert
        assertSuccessfulExecution(result, output ->
            result.getCombinedOutput().contains("Executing DML statements in a transaction")
        );
    }

    @Test
    @Order(7)
    @DisplayName("Should execute DML script non-transactionally by default")
    void testDmlScriptNonTransactional() throws Exception {
        String dmlScript = String.join("\n",
            "INSERT INTO test_table (id, name) VALUES (3, 'C');",
            "INSERT INTO test_table (id, name) VALUES (4, 'D');"
        );
        Path sqlFile = createSqlFile("dml_script2.sql", dmlScript);

        // Act (default args, no --transactional)
        ExecutionResult result = executeCommand(sqlFile);

        // Assert
        assertSuccessfulExecution(result, output ->
            result.getCombinedOutput().contains("Executing DML statements non-transactionally")
        );
    }
}


