package com.financetracker.ui;

import com.financetracker.model.User;
import com.financetracker.service.UserService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.sql.SQLException;

/**
 * Authentication window providing both User Login and Registration views.
 */
public class LoginFrame extends JFrame {
    private final UserService userService;
    private final CardLayout cardLayout;
    private final JPanel cardsPanel;

    // Login fields
    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;
    private JLabel loginErrorLabel;

    // Register fields
    private JTextField regUsernameField;
    private JPasswordField regPasswordField;
    private JPasswordField regConfirmPasswordField;
    private JLabel regErrorLabel;

    public LoginFrame() {
        this.userService = new UserService();
        this.cardLayout = new CardLayout();
        this.cardsPanel = new JPanel(cardLayout);

        setTitle("Personal Finance Tracker - Authentication");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        initComponents();
        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BACKGROUND);
        root.setBorder(new EmptyBorder(30, 40, 30, 40));
        setContentPane(root);

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UITheme.BACKGROUND);
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel appTitle = new JLabel("Personal Finance Tracker");
        appTitle.setFont(UITheme.FONT_TITLE);
        appTitle.setForeground(UITheme.TEXT_MAIN);
        appTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subTitle = new JLabel("Take complete control of your budgets & expenses");
        subTitle.setFont(UITheme.FONT_BODY);
        subTitle.setForeground(UITheme.TEXT_MUTED);
        subTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(appTitle);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subTitle);
        headerPanel.add(Box.createVerticalStrut(20));
        root.add(headerPanel, BorderLayout.NORTH);

        // Cards Panel
        cardsPanel.setBackground(UITheme.BACKGROUND);
        cardsPanel.add(createLoginCard(), "LOGIN");
        cardsPanel.add(createRegisterCard(), "REGISTER");
        root.add(cardsPanel, BorderLayout.CENTER);
    }

    private JPanel createLoginCard() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(360, 320));

        JLabel cardTitle = new JLabel("Sign In to Your Account");
        cardTitle.setFont(UITheme.FONT_HEADER);
        cardTitle.setForeground(UITheme.TEXT_MAIN);
        cardTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(cardTitle);
        card.add(Box.createVerticalStrut(16));

        // Username
        JLabel uLabel = new JLabel("Username");
        uLabel.setFont(UITheme.FONT_SMALL_BOLD);
        uLabel.setForeground(UITheme.TEXT_MUTED);
        uLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(uLabel);
        card.add(Box.createVerticalStrut(4));

        loginUsernameField = new JTextField(20);
        loginUsernameField.setFont(UITheme.FONT_BODY);
        loginUsernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        card.add(loginUsernameField);
        card.add(Box.createVerticalStrut(12));

        // Password
        JLabel pLabel = new JLabel("Password");
        pLabel.setFont(UITheme.FONT_SMALL_BOLD);
        pLabel.setForeground(UITheme.TEXT_MUTED);
        pLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(pLabel);
        card.add(Box.createVerticalStrut(4));

        loginPasswordField = new JPasswordField(20);
        loginPasswordField.setFont(UITheme.FONT_BODY);
        loginPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        card.add(loginPasswordField);
        card.add(Box.createVerticalStrut(8));

        // Error message
        loginErrorLabel = new JLabel(" ");
        loginErrorLabel.setFont(UITheme.FONT_SMALL);
        loginErrorLabel.setForeground(UITheme.DANGER);
        loginErrorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(loginErrorLabel);
        card.add(Box.createVerticalStrut(12));

        // Sign in Button
        JButton loginBtn = UITheme.createPrimaryButton("Sign In");
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.addActionListener(e -> handleLogin());
        loginPasswordField.addActionListener(e -> handleLogin());
        loginUsernameField.addActionListener(e -> handleLogin());
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(12));

        // Switch to register link
        JButton switchBtn = new JButton("Don't have an account? Register here");
        switchBtn.setFont(UITheme.FONT_SMALL);
        switchBtn.setForeground(UITheme.PRIMARY);
        switchBtn.setBorderPainted(false);
        switchBtn.setContentAreaFilled(false);
        switchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        switchBtn.addActionListener(e -> {
            loginErrorLabel.setText(" ");
            cardLayout.show(cardsPanel, "REGISTER");
        });
        card.add(switchBtn);

        return card;
    }

    private JPanel createRegisterCard() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(360, 390));

        JLabel cardTitle = new JLabel("Create New Account");
        cardTitle.setFont(UITheme.FONT_HEADER);
        cardTitle.setForeground(UITheme.TEXT_MAIN);
        cardTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(cardTitle);
        card.add(Box.createVerticalStrut(14));

        // Username
        JLabel uLabel = new JLabel("Username (min 3 chars)");
        uLabel.setFont(UITheme.FONT_SMALL_BOLD);
        uLabel.setForeground(UITheme.TEXT_MUTED);
        uLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(uLabel);
        card.add(Box.createVerticalStrut(4));

        regUsernameField = new JTextField(20);
        regUsernameField.setFont(UITheme.FONT_BODY);
        regUsernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        card.add(regUsernameField);
        card.add(Box.createVerticalStrut(10));

        // Password
        JLabel pLabel = new JLabel("Password (min 4 chars)");
        pLabel.setFont(UITheme.FONT_SMALL_BOLD);
        pLabel.setForeground(UITheme.TEXT_MUTED);
        pLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(pLabel);
        card.add(Box.createVerticalStrut(4));

        regPasswordField = new JPasswordField(20);
        regPasswordField.setFont(UITheme.FONT_BODY);
        regPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        card.add(regPasswordField);
        card.add(Box.createVerticalStrut(10));

        // Confirm Password
        JLabel cpLabel = new JLabel("Confirm Password");
        cpLabel.setFont(UITheme.FONT_SMALL_BOLD);
        cpLabel.setForeground(UITheme.TEXT_MUTED);
        cpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(cpLabel);
        card.add(Box.createVerticalStrut(4));

        regConfirmPasswordField = new JPasswordField(20);
        regConfirmPasswordField.setFont(UITheme.FONT_BODY);
        regConfirmPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        card.add(regConfirmPasswordField);
        card.add(Box.createVerticalStrut(6));

        // Error message
        regErrorLabel = new JLabel(" ");
        regErrorLabel.setFont(UITheme.FONT_SMALL);
        regErrorLabel.setForeground(UITheme.DANGER);
        regErrorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(regErrorLabel);
        card.add(Box.createVerticalStrut(10));

        // Register Button
        JButton regBtn = UITheme.createPrimaryButton("Create Account");
        regBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        regBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        regBtn.addActionListener(e -> handleRegister());
        regConfirmPasswordField.addActionListener(e -> handleRegister());
        card.add(regBtn);
        card.add(Box.createVerticalStrut(10));

        // Switch back to login link
        JButton switchBtn = new JButton("Already have an account? Sign In");
        switchBtn.setFont(UITheme.FONT_SMALL);
        switchBtn.setForeground(UITheme.PRIMARY);
        switchBtn.setBorderPainted(false);
        switchBtn.setContentAreaFilled(false);
        switchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        switchBtn.addActionListener(e -> {
            regErrorLabel.setText(" ");
            cardLayout.show(cardsPanel, "LOGIN");
        });
        card.add(switchBtn);

        return card;
    }

    private void handleLogin() {
        loginErrorLabel.setText(" ");
        String username = loginUsernameField.getText().trim();
        String password = new String(loginPasswordField.getPassword());

        try {
            User user = userService.authenticate(username, password);
            dispose();
            SwingUtilities.invokeLater(() -> {
                MainFrame mainFrame = new MainFrame(user);
                mainFrame.setVisible(true);
            });
        } catch (IllegalArgumentException e) {
            loginErrorLabel.setText(e.getMessage());
        } catch (SQLException e) {
            loginErrorLabel.setText("Database error: " + e.getMessage());
        }
    }

    private void handleRegister() {
        regErrorLabel.setText(" ");
        String username = regUsernameField.getText().trim();
        String password = new String(regPasswordField.getPassword());
        String confirmPassword = new String(regConfirmPasswordField.getPassword());

        try {
            User user = userService.register(username, password, confirmPassword);
            JOptionPane.showMessageDialog(this,
                    "Account '" + user.getUsername() + "' created successfully! Logging you in...",
                    "Registration Successful",
                    JOptionPane.INFORMATION_MESSAGE);

            dispose();
            SwingUtilities.invokeLater(() -> {
                MainFrame mainFrame = new MainFrame(user);
                mainFrame.setVisible(true);
            });
        } catch (IllegalArgumentException e) {
            regErrorLabel.setText(e.getMessage());
        } catch (SQLException e) {
            regErrorLabel.setText("Database error: " + e.getMessage());
        }
    }
}
