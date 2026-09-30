package com.financetracker;

import com.financetracker.db.DBConnection;
import com.financetracker.db.DatabaseInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.sql.SQLException;

/**
 * Base test class that configures a clean isolated SQLite database for unit tests.
 */
public abstract class BaseDBTest {
    private static final String TEST_DB_FILE = "finance_tracker_test.db";

    @BeforeEach
    public void setUpDatabase() {
        File dbFile = new File(TEST_DB_FILE);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        DBConnection.setDbUrl("jdbc:sqlite:" + TEST_DB_FILE);
        DatabaseInitializer.initializeDatabase();
    }

    @AfterEach
    public void tearDownDatabase() {
        DBConnection.getInstance().closeConnection();
        File dbFile = new File(TEST_DB_FILE);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        DBConnection.resetToDefaultUrl();
    }
}
