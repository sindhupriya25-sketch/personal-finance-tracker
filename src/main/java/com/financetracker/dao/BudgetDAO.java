package com.financetracker.dao;

import com.financetracker.db.DBConnection;
import com.financetracker.model.Budget;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Budget management and spending limit comparisons.
 */
public class BudgetDAO {

    /**
     * Inserts or updates (upserts) a category budget limit for a user.
     *
     * @param budget budget object
     * @return generated or updated ID
     * @throws SQLException on database error
     */
    public int setBudget(Budget budget) throws SQLException {
        String sql = """
            INSERT INTO budgets (user_id, category_id, monthly_limit)
            VALUES (?, ?, ?)
            ON CONFLICT(user_id, category_id) DO UPDATE SET
            monthly_limit = excluded.monthly_limit;
        """;
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, budget.getUserId());
            pstmt.setInt(2, budget.getCategoryId());
            pstmt.setDouble(3, budget.getMonthlyLimit());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    budget.setId(id);
                    return id;
                }
            }
        }

        // If updated, fetch the ID
        Optional<Budget> existing = getBudgetByUserAndCategory(budget.getUserId(), budget.getCategoryId());
        return existing.map(Budget::getId).orElse(0);
    }

    /**
     * Retrieves all budgets configured for a user.
     *
     * @param userId user ID
     * @return list of budgets with category names
     * @throws SQLException on database error
     */
    public List<Budget> getBudgetsByUser(int userId) throws SQLException {
        List<Budget> list = new ArrayList<>();
        String sql = """
            SELECT b.id, b.user_id, b.category_id, c.name AS category_name, b.monthly_limit
            FROM budgets b
            JOIN categories c ON b.category_id = c.id
            WHERE b.user_id = ?
            ORDER BY c.name ASC;
        """;
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    /**
     * Finds a budget for a specific user and category.
     *
     * @param userId user ID
     * @param categoryId category ID
     * @return Optional of Budget
     * @throws SQLException on database error
     */
    public Optional<Budget> getBudgetByUserAndCategory(int userId, int categoryId) throws SQLException {
        String sql = """
            SELECT b.id, b.user_id, b.category_id, c.name AS category_name, b.monthly_limit
            FROM budgets b
            JOIN categories c ON b.category_id = c.id
            WHERE b.user_id = ? AND b.category_id = ?;
        """;
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, categoryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Deletes a budget by ID.
     *
     * @param id budget ID
     * @param userId user ID
     * @return true if deleted
     * @throws SQLException on database error
     */
    public boolean deleteBudget(int id, int userId) throws SQLException {
        String sql = "DELETE FROM budgets WHERE id = ? AND user_id = ?;";
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all budgets for a user along with actual spent amounts calculated for that month.
     *
     * @param userId user ID
     * @param year target year
     * @param month target month
     * @return list of budgets with currentSpent populated
     * @throws SQLException on database error
     */
    public List<Budget> getBudgetUsages(int userId, int year, int month) throws SQLException {
        YearMonth ym = YearMonth.of(year, month);
        String start = ym.atDay(1).toString();
        String end = ym.atEndOfMonth().toString();

        String sql = """
            SELECT b.id, b.user_id, b.category_id, c.name AS category_name, b.monthly_limit,
                   COALESCE(SUM(t.amount), 0.0) AS current_spent
            FROM budgets b
            JOIN categories c ON b.category_id = c.id
            LEFT JOIN transactions t ON t.user_id = b.user_id
                                    AND t.category_id = b.category_id
                                    AND t.date >= ? AND t.date <= ?
            WHERE b.user_id = ?
            GROUP BY b.id, b.user_id, b.category_id, c.name, b.monthly_limit
            ORDER BY (COALESCE(SUM(t.amount), 0.0) / b.monthly_limit) DESC, c.name ASC;
        """;

        List<Budget> list = new ArrayList<>();
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, start);
            pstmt.setString(2, end);
            pstmt.setInt(3, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Budget budget = mapResultSet(rs);
                    budget.setCurrentSpent(rs.getDouble("current_spent"));
                    list.add(budget);
                }
            }
        }
        return list;
    }

    /**
     * Deletes all budgets for a user.
     *
     * @param userId user ID
     * @throws SQLException on database error
     */
    public void deleteAllForUser(int userId) throws SQLException {
        String sql = "DELETE FROM budgets WHERE user_id = ?;";
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
    }

    private Budget mapResultSet(ResultSet rs) throws SQLException {
        return new Budget(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("category_id"),
                rs.getString("category_name"),
                rs.getDouble("monthly_limit")
        );
    }
}
