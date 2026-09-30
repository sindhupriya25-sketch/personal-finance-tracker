package com.financetracker.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializes the SQLite database schema and seeds default data.
 */
public class DatabaseInitializer {

    /**
     * Initializes all tables, indexes, and seed categories.
     */
    public static void initializeDatabase() {
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            createTables(conn);
            seedCategories(conn);
            System.out.println("Database initialization completed successfully.");
        } catch (SQLException e) {
            System.err.println("Fatal error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    salt TEXT NOT NULL,
                    created_at TEXT NOT NULL
                );
            """);

            // Categories table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    type TEXT NOT NULL
                );
            """);

            // Budgets table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS budgets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    category_id INTEGER NOT NULL,
                    monthly_limit REAL NOT NULL,
                    UNIQUE(user_id, category_id),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
                );
            """);

            // Transactions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    category_id INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    amount REAL NOT NULL,
                    description TEXT,
                    date TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
                );
            """);

            // Alerts log table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS alerts_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    category_id INTEGER NOT NULL,
                    category_name TEXT NOT NULL,
                    month_year TEXT NOT NULL,
                    limit_amount REAL NOT NULL,
                    spent_amount REAL NOT NULL,
                    percentage REAL NOT NULL,
                    alert_level TEXT NOT NULL,
                    message TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            // Indexes for fast querying
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_trans_user_date ON transactions(user_id, date);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_trans_user_cat ON transactions(user_id, category_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_budgets_user ON budgets(user_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_alerts_user ON alerts_log(user_id, created_at);");
        }
    }

    private static void seedCategories(Connection conn) throws SQLException {
        String[][] defaultCategories = {
            {"Salary", "INCOME"},
            {"Freelance", "INCOME"},
            {"Investment", "INCOME"},
            {"Food", "EXPENSE"},
            {"Rent", "EXPENSE"},
            {"Transport", "EXPENSE"},
            {"Shopping", "EXPENSE"},
            {"Entertainment", "EXPENSE"},
            {"Bills", "EXPENSE"},
            {"Health", "EXPENSE"},
            {"Education", "EXPENSE"},
            {"Savings", "SAVINGS"},
            {"Emergency Fund", "SAVINGS"}
        };

        String insertSql = "INSERT OR IGNORE INTO categories (name, type) VALUES (?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (String[] cat : defaultCategories) {
                pstmt.setString(1, cat[0]);
                pstmt.setString(2, cat[1]);
                pstmt.executeUpdate();
            }
        }
    }
}
