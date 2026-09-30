package com.financetracker.service;

import com.financetracker.dao.UserDAO;
import com.financetracker.model.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service managing user registration and authentication workflows.
 */
public class UserService {
    private final UserDAO userDAO;

    public UserService() {
        this(new UserDAO());
    }

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Registers a new user with password validation and cryptographic hashing.
     *
     * @param username username chosen
     * @param password raw password
     * @param confirmPassword confirmation password
     * @return newly created User
     * @throws IllegalArgumentException on validation failure
     * @throws SQLException on database error or duplicate username
     */
    public User register(String username, String password, String confirmPassword) throws IllegalArgumentException, SQLException {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long.");
        }
        String cleanUsername = username.trim();
        if (!cleanUsername.matches("^[a-zA-Z0-9_.-]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, underscores, and dots.");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters long.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        if (userDAO.findByUsername(cleanUsername).isPresent()) {
            throw new IllegalArgumentException("Username '" + cleanUsername + "' is already taken.");
        }

        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(password, salt);

        User user = new User(0, cleanUsername, passwordHash, salt, LocalDateTime.now().toString());
        int generatedId = userDAO.create(user);
        user.setId(generatedId);
        return user;
    }

    /**
     * Authenticates a user by username and password.
     *
     * @param username username
     * @param password raw password
     * @return User if authentication succeeds
     * @throws IllegalArgumentException if credentials invalid or account not found
     * @throws SQLException on database error
     */
    public User authenticate(String username, String password) throws IllegalArgumentException, SQLException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Please enter both username and password.");
        }

        Optional<User> optUser = userDAO.findByUsername(username.trim());
        if (optUser.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        User user = optUser.get();
        boolean valid = PasswordUtil.verifyPassword(password, user.getSalt(), user.getPasswordHash());
        if (!valid) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        return user;
    }
}
