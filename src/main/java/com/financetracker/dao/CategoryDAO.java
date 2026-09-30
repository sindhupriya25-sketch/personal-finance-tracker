package com.financetracker.dao;

import com.financetracker.db.DBConnection;
import com.financetracker.model.Category;
import com.financetracker.model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Category operations.
 */
public class CategoryDAO {

    /**
     * Retrieves all categories sorted by type then name.
     *
     * @return list of categories
     * @throws SQLException on database error
     */
    public List<Category> getAll() throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT id, name, type FROM categories ORDER BY type ASC, name ASC;";
        Connection conn = DBConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    /**
     * Retrieves categories filtered by transaction type.
     *
     * @param type TransactionType filter (INCOME, EXPENSE, SAVINGS)
     * @return list of categories matching type
     * @throws SQLException on database error
     */
    public List<Category> getByType(TransactionType type) throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT id, name, type FROM categories WHERE type = ? ORDER BY name ASC;";
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    /**
     * Finds category by unique ID.
     *
     * @param id category ID
     * @return Optional containing Category if found
     * @throws SQLException on database error
     */
    public Optional<Category> getById(int id) throws SQLException {
        String sql = "SELECT id, name, type FROM categories WHERE id = ?;";
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
     * Finds category by name (case-insensitive).
     *
     * @param name category name
     * @return Optional containing Category if found
     * @throws SQLException on database error
     */
    public Optional<Category> getByName(String name) throws SQLException {
        String sql = "SELECT id, name, type FROM categories WHERE LOWER(name) = LOWER(?);";
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Creates a new category.
     *
     * @param category category to persist
     * @return generated ID
     * @throws SQLException on duplicate name or database error
     */
    public int create(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, type) VALUES (?, ?);";
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, category.getName().trim());
            pstmt.setString(2, category.getType().name());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating category failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    category.setId(id);
                    return id;
                } else {
                    throw new SQLException("Creating category failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Renames or updates an existing category.
     *
     * @param category category with updated fields
     * @return true if updated
     * @throws SQLException on database error
     */
    public boolean update(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ?, type = ? WHERE id = ?;";
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category.getName().trim());
            pstmt.setString(2, category.getType().name());
            pstmt.setInt(3, category.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a category if it's not referenced in transactions.
     *
     * @param id category ID
     * @return true if deleted
     * @throws SQLException if category is used in transactions or database error occurs
     */
    public boolean delete(int id) throws SQLException {
        if (isCategoryUsedInTransactions(id)) {
            throw new SQLException("Cannot delete category because it is currently used by existing transactions.");
        }
        String sql = "DELETE FROM categories WHERE id = ?;";
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Checks if a category is used in any transactions.
     *
     * @param categoryId category ID
     * @return true if referenced
     * @throws SQLException on database error
     */
    public boolean isCategoryUsedInTransactions(int categoryId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE category_id = ?;";
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private Category mapResultSet(ResultSet rs) throws SQLException {
        return new Category(
                rs.getInt("id"),
                rs.getString("name"),
                TransactionType.fromString(rs.getString("type"))
        );
    }
}
