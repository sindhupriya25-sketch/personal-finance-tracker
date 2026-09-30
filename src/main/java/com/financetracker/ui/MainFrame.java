package com.financetracker.ui;

import com.financetracker.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application window featuring sidebar navigation, top header with period selection,
 * active user profile, and synchronized views.
 */
public class MainFrame extends JFrame {
    private final User currentUser;
    private final CardLayout cardLayout;
    private final JPanel contentArea;

    private DashboardPanel dashboardPanel;
    private TransactionPanel transactionPanel;
    private BudgetPanel budgetPanel;
    private CategoriesPanel categoriesPanel;
    private ReportsPanel reportsPanel;
    private ChartsPanel chartsPanel;
    private AlertsPanel alertsPanel;

    private YearMonth currentMonth;
    private JLabel monthLabel;
    private final Map<String, JButton> navButtons = new HashMap<>();
    private String activeNavKey = "DASHBOARD";

    public MainFrame(User user) {
        this.currentUser = user;
        this.currentMonth = YearMonth.now();
        this.cardLayout = new CardLayout();
        this.contentArea = new JPanel(cardLayout);

        setTitle("Personal Finance Tracker - " + user.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1150, 720));
        setPreferredSize(new Dimension(1280, 800));

        initComponents();
        showPanel("DASHBOARD");
        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BACKGROUND);
        setContentPane(root);

        // 1. Sidebar Navigation (West)
        root.add(createSidebar(), BorderLayout.WEST);

        // 2. Center Panel (Top Header + Content Area)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(UITheme.BACKGROUND);

        // Top Header
        centerPanel.add(createTopHeader(), BorderLayout.NORTH);

        // View Panels
        dashboardPanel = new DashboardPanel(currentUser, this);
        transactionPanel = new TransactionPanel(currentUser, this);
        budgetPanel = new BudgetPanel(currentUser, this);
        categoriesPanel = new CategoriesPanel(currentUser, this);
        reportsPanel = new ReportsPanel(currentUser, this);
        chartsPanel = new ChartsPanel(currentUser, this);
        alertsPanel = new AlertsPanel(currentUser, this);

        contentArea.setBackground(UITheme.BACKGROUND);
        contentArea.add(dashboardPanel, "DASHBOARD");
        contentArea.add(transactionPanel, "TRANSACTIONS");
        contentArea.add(budgetPanel, "BUDGETS");
        contentArea.add(categoriesPanel, "CATEGORIES");
        contentArea.add(reportsPanel, "REPORTS");
        contentArea.add(chartsPanel, "CHARTS");
        contentArea.add(alertsPanel, "ALERTS");

        centerPanel.add(contentArea, BorderLayout.CENTER);
        root.add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(new EmptyBorder(24, 16, 24, 16));

        // Brand Title
        JLabel brandTitle = new JLabel("Finance Tracker");
        brandTitle.setFont(UITheme.FONT_TITLE);
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandSubtitle = new JLabel("Smart Budget & Wealth");
        brandSubtitle.setFont(UITheme.FONT_SMALL);
        brandSubtitle.setForeground(UITheme.TEXT_MUTED);
        brandSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(brandTitle);
        sidebar.add(Box.createVerticalStrut(2));
        sidebar.add(brandSubtitle);
        sidebar.add(Box.createVerticalStrut(28));

        // Navigation Items
        addNavItem(sidebar, "Dashboard", "DASHBOARD");
        addNavItem(sidebar, "Transactions", "TRANSACTIONS");
        addNavItem(sidebar, "Budgets", "BUDGETS");
        addNavItem(sidebar, "Categories", "CATEGORIES");
        addNavItem(sidebar, "Reports & Export", "REPORTS");
        addNavItem(sidebar, "Visual Analytics", "CHARTS");
        addNavItem(sidebar, "Alerts History", "ALERTS");

        sidebar.add(Box.createVerticalGlue());

        // Logout Button
        JButton logoutBtn = new JButton("Sign Out (" + currentUser.getUsername() + ")");
        logoutBtn.setFont(UITheme.FONT_SMALL_BOLD);
        logoutBtn.setForeground(new Color(248, 113, 113));
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(153, 27, 27), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutBtn.addActionListener(e -> logout());
        sidebar.add(logoutBtn);

        return sidebar;
    }

    private void addNavItem(JPanel container, String title, String key) {
        JButton btn = new JButton(title);
        btn.setFont(UITheme.FONT_BODY_BOLD);
        btn.setForeground(UITheme.TEXT_LIGHT);
        btn.setBackground(UITheme.SIDEBAR_BG);
        btn.setBorder(new EmptyBorder(10, 14, 10, 14));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);

        btn.addActionListener(e -> showPanel(key));

        navButtons.put(key, btn);
        container.add(btn);
        container.add(Box.createVerticalStrut(6));
    }

    private JPanel createTopHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.CARD_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
                new EmptyBorder(12, 24, 12, 24)
        ));

        // Month Selector on Left
        JPanel monthSelector = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        monthSelector.setBackground(UITheme.CARD_BG);

        JButton prevBtn = new JButton("<");
        prevBtn.setFont(UITheme.FONT_BODY_BOLD);
        prevBtn.setFocusPainted(false);
        prevBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        prevBtn.addActionListener(e -> changeMonth(-1));

        monthLabel = new JLabel(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        monthLabel.setFont(UITheme.FONT_HEADER);
        monthLabel.setForeground(UITheme.TEXT_MAIN);
        monthLabel.setBorder(new EmptyBorder(0, 8, 0, 8));

        JButton nextBtn = new JButton(">");
        nextBtn.setFont(UITheme.FONT_BODY_BOLD);
        nextBtn.setFocusPainted(false);
        nextBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextBtn.addActionListener(e -> changeMonth(1));

        JButton todayBtn = UITheme.createSecondaryButton("Current Month");
        todayBtn.addActionListener(e -> {
            currentMonth = YearMonth.now();
            updateMonthDisplay();
        });

        monthSelector.add(prevBtn);
        monthSelector.add(monthLabel);
        monthSelector.add(nextBtn);
        monthSelector.add(todayBtn);

        header.add(monthSelector, BorderLayout.WEST);

        // User Info on Right
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        userPanel.setBackground(UITheme.CARD_BG);

        JLabel userBadge = new JLabel("User: " + currentUser.getUsername());
        userBadge.setFont(UITheme.FONT_BODY_BOLD);
        userBadge.setForeground(UITheme.PRIMARY_DARK);
        userBadge.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.PRIMARY, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));

        userPanel.add(userBadge);
        header.add(userPanel, BorderLayout.EAST);

        return header;
    }

    private void changeMonth(int delta) {
        currentMonth = currentMonth.plusMonths(delta);
        updateMonthDisplay();
    }

    private void updateMonthDisplay() {
        monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        dashboardPanel.setMonth(currentMonth);
        budgetPanel.setMonth(currentMonth);
        chartsPanel.setMonth(currentMonth);
    }

    public void showPanel(String key) {
        activeNavKey = key;
        cardLayout.show(contentArea, key);

        // Update nav buttons active appearance
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            if (entry.getKey().equals(key)) {
                entry.getValue().setBackground(UITheme.SIDEBAR_ACTIVE);
                entry.getValue().setForeground(Color.WHITE);
            } else {
                entry.getValue().setBackground(UITheme.SIDEBAR_BG);
                entry.getValue().setForeground(UITheme.TEXT_LIGHT);
            }
        }
    }

    /**
     * Broadcasts a data change notification to all panels so views update automatically.
     */
    public void notifyDataChanged() {
        dashboardPanel.refreshData();
        transactionPanel.refreshData();
        budgetPanel.refreshData();
        categoriesPanel.refreshData();
        reportsPanel.refreshReport();
        chartsPanel.refreshCharts();
        alertsPanel.refreshAlerts();
    }

    private void logout() {
        int opt = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to sign out?",
                "Sign Out",
                JOptionPane.YES_NO_OPTION);

        if (opt == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame login = new LoginFrame();
                login.setVisible(true);
            });
        }
    }
}
