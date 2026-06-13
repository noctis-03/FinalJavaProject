package com.blople;

import com.blople.database.DatabaseManager;
import com.blople.view.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class App {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Look & Feel 설정 실패: " + e.getMessage());
        }

        DatabaseManager dbManager = DatabaseManager.getInstance();
        dbManager.initialize();

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(dbManager);
            frame.setVisible(true);
        });
    }
}
