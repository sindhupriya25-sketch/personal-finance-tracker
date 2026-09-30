package com.financetracker.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton database connection manager for SQLite.
 */
public class DBConnection {
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:finance_tracker.db";
    private static String dbUrl = DEFAULT_DB_URL;
    private static DBConnection instance;
    private Connection connection;

    private DBConnection() {
    }

    /**
     * Retrieves the singleton instance of DBConnection.
     *
     * @return DBConnection instance
     */
    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Sets a custom database URL (primarily for testing with SQLite in-memory DB).
     *
     * @param url database JDBC URL
     */
    public static synchronized void setDbUrl(String url) {
        dbUrl = url;
        if (instance != null) {
            instance.closeConnection();
        }
    }

    /**
     * Resets database URL to the default file database.
     */
    public static synchronized void resetToDefaultUrl() {
        setDbUrl(DEFAULT_DB_URL);
    }

    /**
     * Gets an open Connection to the SQLite database.
     * Reopens if closed or null. Enables foreign key constraints.
     *
     * @return open java.sql.Connection
     * @throws SQLException if a database access error occurs
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(dbUrl);
            // Enable foreign key support in SQLite
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        }
        return connection;
    }

    /**
     * Safely closes the active database connection if open.
     */
    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
}
