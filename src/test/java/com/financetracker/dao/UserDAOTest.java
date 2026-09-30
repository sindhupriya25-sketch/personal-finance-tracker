package com.financetracker.dao;

import com.financetracker.BaseDBTest;
import com.financetracker.model.User;
import com.financetracker.service.PasswordUtil;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserDAO operations.
 */
public class UserDAOTest extends BaseDBTest {

    private final UserDAO userDAO = new UserDAO();

    @Test
    public void testCreateAndFindUser() throws SQLException {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword("pass123", salt);
        User user = new User("alice", hash, salt);

        int id = userDAO.create(user);
        assertTrue(id > 0);

        Optional<User> byName = userDAO.findByUsername("alice");
        assertTrue(byName.isPresent());
        assertEquals("alice", byName.get().getUsername());
        assertEquals(hash, byName.get().getPasswordHash());

        Optional<User> byId = userDAO.findById(id);
        assertTrue(byId.isPresent());
        assertEquals("alice", byId.get().getUsername());
    }

    @Test
    public void testDuplicateUsernameThrowsException() throws SQLException {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword("pass123", salt);
        User user1 = new User("duplicate_user", hash, salt);
        userDAO.create(user1);

        User user2 = new User("duplicate_user", hash, salt);
        assertThrows(SQLException.class, () -> userDAO.create(user2));
    }

    @Test
    public void testGetAllUsers() throws SQLException {
        String salt = PasswordUtil.generateSalt();
        userDAO.create(new User("user_one", "hash1", salt));
        userDAO.create(new User("user_two", "hash2", salt));

        List<User> users = userDAO.getAll();
        assertTrue(users.size() >= 2);
    }
}
