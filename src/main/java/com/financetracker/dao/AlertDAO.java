package com.financetracker.dao;

import com.financetracker.db.DBConnection;
import com.financetracker.model.BudgetAlert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Budget Alerts logging and querying.
 */
public class AlertDAO {

    /**
     * Persists a triggered budget alert into the database.
     *
     * @param alert alert entity
     * @return generated ID
     * @throws SQLException on database error
     */
    public int create(BudgetAlert alert) throws SQLException {
        String sql = """
            INSERT INTO alerts_log (user_id, category_id, category_name, month_year, limit_amount,
                                    spent_amount, percentage, alert_level, message, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, alert.getUserId());
            pstmt.setInt(2, alert.getCategoryId());
            pstmt.setString(3, alert.getCategoryName());
            pstmt.setString(4, alert.getMonthYear());
            pstmt.setDouble(5, alert.getLimitAmount());
            pstmt.setDouble(6, alert.getSpentAmount());
            pstmt.setDouble(7, alert.getPercentage());
            pstmt.setString(8, alert.getAlertLevel().name());
            pstmt.setString(9, alert.getMessage());
            pstmt.setString(10, alert.getTimestamp() != null ? alert.getTimestamp().toString() : LocalDateTime.now().toString());

            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Saving alert failed, no rows affected.");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    alert.setId(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /**
     * Retrieves the most recent alerts for a user.
     *
     * @param userId user ID
     * @param limit max records to retrieve
     * @return list of BudgetAlerts sorted by created_at DESC
     * @throws SQLException on database error
     */
    public List<BudgetAlert> getByUser(int userId, int limit) throws SQLException {
        List<BudgetAlert> list = new ArrayList<>();
        String sql = """
            SELECT id, user_id, category_id, category_name, month_year, limit_amount,
                   spent_amount, percentage, alert_level, message, created_at
            FROM alerts_log
            WHERE user_id = ?
            ORDER BY created_at DESC, id DESC
            LIMIT ?;
        """;
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String tsStr = rs.getString("created_at");
                    LocalDateTime ts = LocalDateTime.now();
                    if (tsStr != null && !tsStr.isEmpty()) {
                        try {
                            ts = LocalDateTime.parse(tsStr);
                        } catch (Exception ignored) {
                        }
                    }

                    BudgetAlert alert = new BudgetAlert(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getInt("category_id"),
                            rs.getString("category_name"),
                            rs.getString("month_year"),
                            rs.getDouble("limit_amount"),
                            rs.getDouble("spent_amount"),
                            rs.getDouble("percentage"),
                            BudgetAlert.AlertLevel.valueOf(rs.getString("alert_level")),
                            rs.getString("message"),
                            ts
                    );
                    list.add(alert);
                }
            }
        }
        return list;
    }

    /**
     * Clears all alerts for a user.
     *
     * @param userId user ID
     * @throws SQLException on database error
     */
    public void clearUserAlerts(int userId) throws SQLException {
        String sql = "DELETE FROM alerts_log WHERE user_id = ?;";
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
    }
}
