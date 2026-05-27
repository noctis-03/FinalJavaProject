package com.blople.view;

import com.blople.common.Constants;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;

/**
 * 메인 윈도우 프레임.
 * 상단 NavigationPanel 과 캘린더/과목 페이지를 CardLayout 으로 전환한다.
 *
 * v0.1 단계에서는 실제 페이지 패널 대신 자리표시자(placeholder)를 사용.
 */
public class MainFrame extends JFrame {

    public static final String CARD_CALENDAR = "CALENDAR";
    public static final String CARD_SUBJECT  = "SUBJECT";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel   = new JPanel(cardLayout);
    private final NavigationPanel navigationPanel;

    public MainFrame() {
        super(Constants.APP_NAME + " v" + Constants.APP_VERSION);

        // === 기본 설정 ===
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(Constants.MAIN_WINDOW_WIDTH, Constants.MAIN_WINDOW_HEIGHT);
        setMinimumSize(new Dimension(Constants.MIN_WINDOW_WIDTH, Constants.MIN_WINDOW_HEIGHT));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // === 상단 네비게이션 ===
        navigationPanel = new NavigationPanel();
        navigationPanel.setOnCalendarTabClick(e -> showCalendarPage());
        navigationPanel.setOnSubjectTabClick (e -> showSubjectPage());

        // === 컨텐츠 영역 (CardLayout) ===
        contentPanel.add(createPlaceholder("📅 캘린더 페이지 (FR-01)\n구현 예정 (v0.2)"), CARD_CALENDAR);
        contentPanel.add(createPlaceholder("📚 과목 페이지 (FR-03)\n구현 예정 (v0.3)"),   CARD_SUBJECT);

        add(navigationPanel, BorderLayout.NORTH);
        add(contentPanel,    BorderLayout.CENTER);

        // 초기 화면: 캘린더
        showCalendarPage();
    }

    public void showCalendarPage() {
        navigationPanel.setActiveTab(NavigationPanel.TAB_CALENDAR);
        cardLayout.show(contentPanel, CARD_CALENDAR);
    }

    public void showSubjectPage() {
        navigationPanel.setActiveTab(NavigationPanel.TAB_SUBJECT);
        cardLayout.show(contentPanel, CARD_SUBJECT);
    }

    /** v0.1 단계 임시 자리표시자 패널. */
    private JPanel createPlaceholder(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("<html><div style='text-align:center;'>"
                + message.replace("\n", "<br>") + "</div></html>",
                SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 22));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
}
