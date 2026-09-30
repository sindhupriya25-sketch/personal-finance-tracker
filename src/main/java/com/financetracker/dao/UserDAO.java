package com.financetracker.dao;

import com.financetracker.db.DBConnection;
import com.financetracker.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for User operations.
 */
public class UserDAO {

    /**
     * Inserts a new user into the database.
     *
     * @param user user entity to persist
     * @return generated user ID
     * @throws SQLException if username already exists or database error occurs
     */
    public int create(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, salt, created_at) VALUES (?, ?, ?, ?);";
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getSalt());
            pstmt.setString(4, user.getCreatedAt());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    user.setId(id);
                    return id;
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Finds a user by username (case-insensitive search).
     *
     * @param username username to find
     * @return Optional containing User if found
     * @throws SQLException on database error
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, salt, created_at FROM users WHERE LOWER(username) = LOWER(?);";
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("salt"),
                            rs.getString("created_at")
                    );
                    return Optional.of(user);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a user by their unique ID.
     *
     * @param id user ID
     * @return Optional containing User if found
     * @throws SQLException on database error
     */
    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT id, username, password_hash, salt, created_at FROM users WHERE id = ?;";
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("salt"),
                            rs.getString("created_at")
                    );
                    return Optional.of(user);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all users.
     *
     * @return list of all users
     * @throws SQLException on database error
     */
    public List<User> getAll() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, salt, created_at FROM users ORDER BY username ASC;";
        Connection conn = DBConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("salt"),
                        rs.getString("created_at")
                ));
            }
        }
        return list;
    }
}
