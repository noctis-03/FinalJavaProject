package com.blople.view;

import com.blople.service.WeatherService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NavigationPanel extends JPanel {

    public static final String TAB_CALENDAR = "CALENDAR";
    public static final String TAB_SUBJECT  = "SUBJECT";

    private static final Color BG_COLOR     = new Color(0xF7F7F9);
    private static final Color ACTIVE_BG    = new Color(0x4D96FF);
    private static final Color ACTIVE_FG    = Color.WHITE;
    private static final Color INACTIVE_BG  = new Color(0xE8E8EC);
    private static final Color INACTIVE_FG  = new Color(0x333333);
    private static final Color DANGER_BG    = new Color(0xFFE4E4);
    private static final Color DANGER_FG    = new Color(0xB42318);

    private static final Color CARD_BG         = Color.WHITE;
    private static final Color CARD_BORDER     = new Color(0xE3E5EB);
    private static final Color CARD_BORDER_NOW = new Color(0xBBD4FF);
    private static final Color TEXT_MAIN       = new Color(0x222831);
    private static final Color TEXT_SUB        = new Color(0x7A7F8A);
    private static final Color TEXT_ACCENT     = new Color(0x4D96FF);

    private static final int REFRESH_INTERVAL_MS = 30 * 60 * 1000;

    private final JButton calendarTabButton = new JButton("📅 캘린더");
    private final JButton subjectTabButton  = new JButton("📚 과목");
    private final JButton resetAppButton    = new JButton("⟲ 초기화");
    private final JLabel  appTitle          = new JLabel("Blople");

    private final JLabel nowIcon = new JLabel("·");
    private final JLabel nowTemp = new JLabel("--°");

    private final JLabel tmrIcon  = new JLabel("·");
    private final JLabel tmrRange = new JLabel("--° / --°");

    private final JLabel updatedLabel = new JLabel(" ");
    private final JButton refreshBtn  = new JButton("⟳ 갱신");

    private final WeatherService weatherService = new WeatherService();
    private final Timer refreshTimer;

    public NavigationPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xDDDDDD)));
        setPreferredSize(new Dimension(0, 76));

        add(buildLeft(),  BorderLayout.WEST);
        add(buildRight(), BorderLayout.EAST);

        setActiveTab(TAB_CALENDAR);

        refreshWeather();
        refreshTimer = new Timer(REFRESH_INTERVAL_MS, e -> refreshWeather());
        refreshTimer.setRepeats(true);
        refreshTimer.start();
    }

    private JPanel buildLeft() {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 18));
        left.setOpaque(false);

        appTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        appTitle.setForeground(new Color(0x333333));
        appTitle.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 24));

        styleTabButton(calendarTabButton);
        styleTabButton(subjectTabButton);
        styleDangerButton(resetAppButton);
        resetAppButton.setToolTipText("모든 학기/과목/과제/캔버스 데이터를 삭제합니다");

        left.add(appTitle);
        left.add(calendarTabButton);
        left.add(subjectTabButton);
        left.add(resetAppButton);
        return left;
    }

    private JPanel buildRight() {
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 14));
        right.setOpaque(false);
        right.add(buildWeatherWidget());
        return right;
    }

    private JPanel buildWeatherWidget() {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.X_AXIS));

        wrap.add(buildRefreshColumn());
        wrap.add(Box.createHorizontalStrut(8));

        JPanel nowCard = buildCard("현재  ·  대전", true, nowIcon, nowTemp);
        wrap.add(nowCard);
        wrap.add(Box.createHorizontalStrut(6));

        JPanel tmrCard = buildCard("내일", false, tmrIcon, tmrRange);
        wrap.add(tmrCard);
        return wrap;
    }

    private JPanel buildRefreshColumn() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        updatedLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        updatedLabel.setForeground(TEXT_SUB);
        updatedLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        updatedLabel.setHorizontalAlignment(JLabel.RIGHT);

        refreshBtn.setFocusPainted(false);
        refreshBtn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        refreshBtn.setToolTipText("새로고침");
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.setMargin(new java.awt.Insets(1, 6, 1, 6));
        refreshBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
        refreshBtn.addActionListener(e -> refreshWeather());

        col.add(Box.createVerticalGlue());
        col.add(updatedLabel);
        col.add(Box.createVerticalStrut(3));
        col.add(refreshBtn);
        col.add(Box.createVerticalGlue());
        return col;
    }

    private JPanel buildCard(String header, boolean accent,
                             JLabel iconLabel, JLabel bigLabel) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(6, 0));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent ? CARD_BORDER_NOW : CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(3, 8, 3, 10)));
        card.setPreferredSize(new Dimension(accent ? 200 : 150, 46));

        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        iconLabel.setHorizontalAlignment(JLabel.CENTER);
        iconLabel.setPreferredSize(new Dimension(26, 36));
        card.add(iconLabel, BorderLayout.WEST);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JLabel head = new JLabel(header);
        head.setFont(new Font("SansSerif", Font.BOLD, 9));
        head.setForeground(accent ? TEXT_ACCENT : TEXT_SUB);

        bigLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        bigLabel.setForeground(TEXT_MAIN);

        col.add(Box.createVerticalGlue());
        col.add(head);
        col.add(Box.createVerticalStrut(2));
        col.add(bigLabel);
        col.add(Box.createVerticalGlue());
        card.add(col, BorderLayout.CENTER);
        return card;
    }

    private void refreshWeather() {
        nowIcon.setText("·");
        nowTemp.setText("불러오는 중…");
        tmrIcon.setText("·");
        tmrRange.setText("--° / --°");

        weatherService.fetchDaejeonAsync(snap -> SwingUtilities.invokeLater(() -> {
            if (!snap.isSuccess()) {
                nowIcon.setText("⚠");
                nowTemp.setText("오류");
                tmrIcon.setText("·");
                tmrRange.setText("");
                return;
            }

            WeatherService.DayForecast today = snap.getToday();
            nowIcon.setText(WeatherService.icon(snap.getCurrentCode()));
            if (today != null
                    && !Double.isNaN(today.tempMin()) && !Double.isNaN(today.tempMax())) {
                nowTemp.setText("%.0f°  (%.0f° / %.0f°)".formatted(
                        snap.getCurrentTemp(), today.tempMin(), today.tempMax()));
            } else {
                nowTemp.setText("%.0f°".formatted(snap.getCurrentTemp()));
            }

            WeatherService.DayForecast tm = snap.getTomorrow();
            if (tm != null && !Double.isNaN(tm.tempMin()) && !Double.isNaN(tm.tempMax())) {
                tmrIcon.setText(WeatherService.icon(tm.weatherCode()));
                tmrRange.setText("%.0f° / %.0f°".formatted(tm.tempMin(), tm.tempMax()));
            } else {
                tmrIcon.setText("·");
                tmrRange.setText("예보 없음");
            }

            updatedLabel.setText(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm 갱신")));
        }));
    }

    private void styleTabButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 36));
    }

    private void styleDangerButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(96, 36));
        btn.setBackground(DANGER_BG);
        btn.setForeground(DANGER_FG);
        btn.setOpaque(true);
    }

    public void setActiveTab(String tabName) {
        boolean calendarActive = TAB_CALENDAR.equals(tabName);
        applyTabStyle(calendarTabButton, calendarActive);
        applyTabStyle(subjectTabButton, !calendarActive);
    }

    private void applyTabStyle(JButton btn, boolean active) {
        btn.setBackground(active ? ACTIVE_BG : INACTIVE_BG);
        btn.setForeground(active ? ACTIVE_FG : INACTIVE_FG);
        btn.setOpaque(true);
    }

    public void setOnCalendarTabClick(ActionListener listener) {
        calendarTabButton.addActionListener(listener);
    }

    public void setOnSubjectTabClick(ActionListener listener) {
        subjectTabButton.addActionListener(listener);
    }

    /** "프로그램 초기화" 버튼 클릭 콜백. */
    public void setOnResetAppClick(ActionListener listener) {
        // 기존 리스너 제거 후 새로 등록 (중복 방지)
        for (var l : resetAppButton.getActionListeners()) resetAppButton.removeActionListener(l);
        if (listener != null) resetAppButton.addActionListener(listener);
    }

    public void dispose() {
        if (refreshTimer != null) refreshTimer.stop();
        weatherService.shutdown();
    }
}
