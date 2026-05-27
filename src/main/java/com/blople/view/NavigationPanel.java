package com.blople.view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;

/**
 * 상단 네비게이션 바.
 * '캘린더' / '과목' 탭 전환과 앱 타이틀, (추후) 날씨 위젯 자리를 가진다.
 */
public class NavigationPanel extends JPanel {

    public static final String TAB_CALENDAR = "CALENDAR";
    public static final String TAB_SUBJECT  = "SUBJECT";

    private static final Color BG_COLOR         = new Color(0xF7F7F9);
    private static final Color ACTIVE_BG        = new Color(0x4D96FF);
    private static final Color ACTIVE_FG        = Color.WHITE;
    private static final Color INACTIVE_BG      = new Color(0xE8E8EC);
    private static final Color INACTIVE_FG      = new Color(0x333333);

    private final JButton calendarTabButton = new JButton("📅 캘린더");
    private final JButton subjectTabButton  = new JButton("📚 과목");
    private final JLabel  appTitle          = new JLabel("Blople");
    private final JLabel  weatherPlaceholder = new JLabel("☀ 날씨 (v0.7+)");

    public NavigationPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xDDDDDD)));
        setPreferredSize(new Dimension(0, 56));

        appTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        appTitle.setForeground(new Color(0x333333));
        appTitle.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 24));

        styleTabButton(calendarTabButton);
        styleTabButton(subjectTabButton);

        weatherPlaceholder.setForeground(new Color(0x888888));
        weatherPlaceholder.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));

        add(appTitle);
        add(calendarTabButton);
        add(subjectTabButton);
        add(weatherPlaceholder);

        setActiveTab(TAB_CALENDAR);
    }

    private void styleTabButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 36));
    }

    public void setActiveTab(String tabName) {
        boolean calendarActive = TAB_CALENDAR.equals(tabName);
        applyTabStyle(calendarTabButton, calendarActive);
        applyTabStyle(subjectTabButton, !calendarActive);
    }

    private void applyTabStyle(JButton btn, boolean active) {
        btn.setBackground(active ? ACTIVE_BG   : INACTIVE_BG);
        btn.setForeground(active ? ACTIVE_FG   : INACTIVE_FG);
        btn.setOpaque(true);
    }

    public void setOnCalendarTabClick(ActionListener listener) {
        calendarTabButton.addActionListener(listener);
    }

    public void setOnSubjectTabClick(ActionListener listener) {
        subjectTabButton.addActionListener(listener);
    }
}
