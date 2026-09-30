package com.financetracker.dao;

import com.financetracker.BaseDBTest;
import com.financetracker.model.Category;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import com.financetracker.model.User;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CategoryDAO operations.
 */
public class CategoryDAOTest extends BaseDBTest {

    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final UserDAO userDAO = new UserDAO();

    @Test
    public void testSeedCategoriesExist() throws SQLException {
        List<Category> all = categoryDAO.getAll();
        assertFalse(all.isEmpty());

        Optional<Category> salary = categoryDAO.getByName("Salary");
        assertTrue(salary.isPresent());
        assertEquals(TransactionType.INCOME, salary.get().getType());

        Optional<Category> food = categoryDAO.getByName("Food");
        assertTrue(food.isPresent());
        assertEquals(TransactionType.EXPENSE, food.get().getType());
    }

    @Test
    public void testCreateAndRenameCategory() throws SQLException {
        Category custom = new Category("Custom Hobbies", TransactionType.EXPENSE);
        int id = categoryDAO.create(custom);
        assertTrue(id > 0);

        custom.setName("Hobbies & Games");
        boolean updated = categoryDAO.update(custom);
        assertTrue(updated);

        Optional<Category> fetched = categoryDAO.getById(id);
        assertTrue(fetched.isPresent());
        assertEquals("Hobbies & Games", fetched.get().getName());
    }

    @Test
    public void testDeleteCategoryNotInUse() throws SQLException {
        Category cat = new Category("Temp Category", TransactionType.EXPENSE);
        int id = categoryDAO.create(cat);

        boolean deleted = categoryDAO.delete(id);
        assertTrue(deleted);
        assertTrue(categoryDAO.getById(id).isEmpty());
    }

    @Test
    public void testDeleteCategoryInUseThrowsException() throws SQLException {
        User user = new User("cat_user", "hash", "salt");
        int userId = userDAO.create(user);

        Category cat = categoryDAO.getByName("Food").orElseThrow();
        Transaction tx = new Transaction(userId, cat.getId(), TransactionType.EXPENSE, 50.0, "Lunch", LocalDate.now());
        transactionDAO.create(tx);

        assertThrows(SQLException.class, () -> categoryDAO.delete(cat.getId()));
    }
}
