package com.financetracker;

import com.formdev.flatlaf.FlatLightLaf;
import com.financetracker.db.DatabaseInitializer;
import com.financetracker.ui.LoginFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Main application entrypoint for Personal Finance Tracker.
 */
public class Main {

    public static void main(String[] args) {
        // 1. Initialize Modern FlatLaf Look and Feel
        try {
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.width", 10);
            FlatLightLaf.setup();
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf theme, falling back to system L&F: " + e.getMessage());
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        // 2. Initialize Database & Tables
        DatabaseInitializer.initializeDatabase();

        // 3. Launch UI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
