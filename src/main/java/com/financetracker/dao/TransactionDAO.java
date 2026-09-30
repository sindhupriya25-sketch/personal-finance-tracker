package com.financetracker.dao;

import com.financetracker.db.DBConnection;
import com.financetracker.model.CategorySpend;
import com.financetracker.model.MonthlySummary;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for financial transaction management, querying, and reporting.
 */
public class TransactionDAO {

    /**
     * Inserts a new transaction into the database.
     *
     * @param tx transaction entity
     * @return generated ID
     * @throws SQLException on database error
     */
    public int create(Transaction tx) throws SQLException {
        String sql = """
            INSERT INTO transactions (user_id, category_id, type, amount, description, date)
            VALUES (?, ?, ?, ?, ?, ?);
        """;
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, tx.getUserId());
            pstmt.setInt(2, tx.getCategoryId());
            pstmt.setString(3, tx.getType().name());
            pstmt.setDouble(4, tx.getAmount());
            pstmt.setString(5, tx.getDescription() != null ? tx.getDescription().trim() : "");
            pstmt.setString(6, tx.getDate() != null ? tx.getDate().toString() : LocalDate.now().toString());

            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating transaction failed, no rows affected.");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    tx.setId(id);
                    return id;
                } else {
                    throw new SQLException("Creating transaction failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Updates an existing transaction.
     *
     * @param tx transaction entity
     * @return true if updated
     * @throws SQLException on database error
     */
    public boolean update(Transaction tx) throws SQLException {
        String sql = """
            UPDATE transactions
            SET category_id = ?, type = ?, amount = ?, description = ?, date = ?
            WHERE id = ? AND user_id = ?;
        """;
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tx.getCategoryId());
            pstmt.setString(2, tx.getType().name());
            pstmt.setDouble(3, tx.getAmount());
            pstmt.setString(4, tx.getDescription() != null ? tx.getDescription().trim() : "");
            pstmt.setString(5, tx.getDate() != null ? tx.getDate().toString() : LocalDate.now().toString());
            pstmt.setInt(6, tx.getId());
            pstmt.setInt(7, tx.getUserId());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a transaction by ID.
     *
     * @param id transaction ID
     * @param userId user ID for safety ownership
     * @return true if deleted
     * @throws SQLException on database error
     */
    public boolean delete(int id, int userId) throws SQLException {
        String sql = "DELETE FROM transactions WHERE id = ? AND user_id = ?;";
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves transaction by ID with joined category name.
     *
     * @param id transaction ID
     * @return Optional of Transaction
     * @throws SQLException on database error
     */
    public Optional<Transaction> getById(int id) throws SQLException {
        String sql = """
            SELECT t.id, t.user_id, t.category_id, c.name AS category_name, t.type, t.amount, t.description, t.date
            FROM transactions t
            LEFT JOIN categories c ON t.category_id = c.id
            WHERE t.id = ?;
        """;
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Queries transactions with dynamic filter options.
     *
     * @param userId current user ID
     * @param fromDate start date (inclusive, optional)
     * @param toDate end date (inclusive, optional)
     * @param categoryId category filter (optional)
     * @param type transaction type filter (optional)
     * @param searchKeyword search string in description or category name (optional)
     * @return list of matching transactions sorted by date DESC, id DESC
     * @throws SQLException on database error
     */
    public List<Transaction> getFiltered(int userId, LocalDate fromDate, LocalDate toDate,
                                         Integer categoryId, TransactionType type, String searchKeyword) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT t.id, t.user_id, t.category_id, c.name AS category_name, t.type, t.amount, t.description, t.date
            FROM transactions t
            LEFT JOIN categories c ON t.category_id = c.id
            WHERE t.user_id = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (fromDate != null) {
            sql.append(" AND t.date >= ?");
            params.add(fromDate.toString());
        }
        if (toDate != null) {
            sql.append(" AND t.date <= ?");
            params.add(toDate.toString());
        }
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND t.category_id = ?");
            params.add(categoryId);
        }
        if (type != null) {
            sql.append(" AND t.type = ?");
            params.add(type.name());
        }
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(t.description) LIKE ? OR LOWER(c.name) LIKE ?)");
            String pattern = "%" + searchKeyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        sql.append(" ORDER BY t.date DESC, t.id DESC;");

        List<Transaction> list = new ArrayList<>();
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    /**
     * Calculates total spent in a category for a specific month and year.
     *
     * @param userId user ID
     * @param categoryId category ID
     * @param year year (e.g. 2026)
     * @param month month (1 - 12)
     * @return total spent in that month
     * @throws SQLException on database error
     */
    public double getCategoryMonthlySpend(int userId, int categoryId, int year, int month) throws SQLException {
        YearMonth ym = YearMonth.of(year, month);
        String start = ym.atDay(1).toString();
        String end = ym.atEndOfMonth().toString();

        String sql = """
            SELECT COALESCE(SUM(amount), 0.0)
            FROM transactions
            WHERE user_id = ? AND category_id = ? AND date >= ? AND date <= ?;
        """;
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, categoryId);
            pstmt.setString(3, start);
            pstmt.setString(4, end);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    /**
     * Computes the aggregated breakdown of transactions per category over a date range.
     *
     * @param userId user ID
     * @param fromDate start date
     * @param toDate end date
     * @param type optional TransactionType filter (e.g. EXPENSE)
     * @return list of CategorySpend objects with computed percentages
     * @throws SQLException on database error
     */
    public List<CategorySpend> getCategorySpends(int userId, LocalDate fromDate, LocalDate toDate, TransactionType type) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT c.name AS category_name, t.type, SUM(t.amount) AS total_amount, COUNT(t.id) AS tx_count
            FROM transactions t
            JOIN categories c ON t.category_id = c.id
            WHERE t.user_id = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (fromDate != null) {
            sql.append(" AND t.date >= ?");
            params.add(fromDate.toString());
        }
        if (toDate != null) {
            sql.append(" AND t.date <= ?");
            params.add(toDate.toString());
        }
        if (type != null) {
            sql.append(" AND t.type = ?");
            params.add(type.name());
        }

        sql.append(" GROUP BY c.name, t.type ORDER BY total_amount DESC;");

        List<CategorySpend> list = new ArrayList<>();
        double grandTotal = 0.0;

        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String catName = rs.getString("category_name");
                    TransactionType txType = TransactionType.fromString(rs.getString("type"));
                    double amt = rs.getDouble("total_amount");
                    int count = rs.getInt("tx_count");

                    CategorySpend cs = new CategorySpend(catName, txType, amt, count);
                    list.add(cs);
                    grandTotal += amt;
                }
            }
        }

        for (CategorySpend cs : list) {
            if (grandTotal > 0) {
                cs.setPercentageOfTotal((cs.getTotalAmount() / grandTotal) * 100.0);
            } else {
                cs.setPercentageOfTotal(0.0);
            }
        }

        return list;
    }

    /**
     * Computes the MonthlySummary for a specific month.
     *
     * @param userId user ID
     * @param year year
     * @param month month
     * @return MonthlySummary
     * @throws SQLException on database error
     */
    public MonthlySummary getMonthlySummary(int userId, int year, int month) throws SQLException {
        YearMonth ym = YearMonth.of(year, month);
        String start = ym.atDay(1).toString();
        String end = ym.atEndOfMonth().toString();
        String key = ym.format(DateTimeFormatter.ofPattern("MMM yyyy"));

        String sql = """
            SELECT
                COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0.0) AS total_income,
                COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0.0) AS total_expense,
                COALESCE(SUM(CASE WHEN type = 'SAVINGS' THEN amount ELSE 0 END), 0.0) AS total_savings
            FROM transactions
            WHERE user_id = ? AND date >= ? AND date <= ?;
        """;

        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, start);
            pstmt.setString(3, end);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new MonthlySummary(
                            key,
                            rs.getDouble("total_income"),
                            rs.getDouble("total_expense"),
                            rs.getDouble("total_savings")
                    );
                }
            }
        }
        return new MonthlySummary(key, 0.0, 0.0, 0.0);
    }

    /**
     * Retrieves monthly summaries for the past N months up to the current date.
     *
     * @param userId user ID
     * @param monthsCount number of past months to retrieve
     * @return list of MonthlySummary in chronological order
     * @throws SQLException on database error
     */
    public List<MonthlySummary> getMonthlySummaries(int userId, int monthsCount) throws SQLException {
        List<MonthlySummary> result = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = monthsCount - 1; i >= 0; i--) {
            YearMonth targetMonth = current.minusMonths(i);
            MonthlySummary summary = getMonthlySummary(userId, targetMonth.getYear(), targetMonth.getMonthValue());
            summary.setMonthKey(targetMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")));
            result.add(summary);
        }
        return result;
    }

    /**
     * Deletes all transactions for a specific user (used in tests or sample data reset).
     *
     * @param userId user ID
     * @throws SQLException on database error
     */
    public void deleteAllForUser(int userId) throws SQLException {
        String sql = "DELETE FROM transactions WHERE user_id = ?;";
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
    }

    private Transaction mapResultSet(ResultSet rs) throws SQLException {
        String dateStr = rs.getString("date");
        LocalDate date = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();

        return new Transaction(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("category_id"),
                rs.getString("category_name"),
                TransactionType.fromString(rs.getString("type")),
                rs.getDouble("amount"),
                rs.getString("description"),
                date
        );
    }
}
