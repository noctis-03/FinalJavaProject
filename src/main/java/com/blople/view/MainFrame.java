package com.blople.view;

import com.blople.common.Constants;
import com.blople.controller.CalendarController;
import com.blople.database.DatabaseManager;
import com.blople.database.TransactionManager;
import com.blople.database.dao.BlockDAO;
import com.blople.database.dao.ScheduleBlockDAO;
import com.blople.model.calendar.CalendarModel;
import com.blople.service.CalendarService;
import com.blople.view.calendar.CalendarPagePanel;

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
 * 상단 NavigationPanel + 캘린더/과목 페이지를 CardLayout 으로 전환한다.
 *
 * <p>v0.2 부터 캘린더 페이지는 실제 {@link CalendarPagePanel} + {@link CalendarController} 로 구동된다.
 */
public class MainFrame extends JFrame {

    public static final String CARD_CALENDAR = "CALENDAR";
    public static final String CARD_SUBJECT  = "SUBJECT";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel   = new JPanel(cardLayout);
    private final NavigationPanel navigationPanel;

    private final CalendarPagePanel calendarPagePanel;
    private final CalendarController calendarController;

    public MainFrame(DatabaseManager dbManager) {
        super(Constants.APP_NAME + " v" + Constants.APP_VERSION);

        // === 기본 설정 ===
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(Constants.MAIN_WINDOW_WIDTH, Constants.MAIN_WINDOW_HEIGHT);
        setMinimumSize(new Dimension(Constants.MIN_WINDOW_WIDTH, Constants.MIN_WINDOW_HEIGHT));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // === 의존성 조립 (캘린더) ===
        TransactionManager tx        = new TransactionManager(dbManager);
        BlockDAO blockDAO            = new BlockDAO(dbManager);
        ScheduleBlockDAO scheduleDAO = new ScheduleBlockDAO(dbManager, blockDAO, tx);
        CalendarModel model          = new CalendarModel();
        CalendarService calendarSvc  = new CalendarService(model, scheduleDAO);

        calendarPagePanel  = new CalendarPagePanel();
        calendarController = new CalendarController(calendarSvc, calendarPagePanel);

        // === 상단 네비게이션 ===
        navigationPanel = new NavigationPanel();
        navigationPanel.setOnCalendarTabClick(e -> showCalendarPage());
        navigationPanel.setOnSubjectTabClick (e -> showSubjectPage());

        // === 컨텐츠 영역 (CardLayout) ===
        contentPanel.add(calendarPagePanel, CARD_CALENDAR);
        contentPanel.add(createPlaceholder("📚 과목 페이지 (FR-03)\n구현 예정 (v0.3)"), CARD_SUBJECT);

        add(navigationPanel, BorderLayout.NORTH);
        add(contentPanel,    BorderLayout.CENTER);

        // 초기 화면: 캘린더
        showCalendarPage();

        // 컨트롤러 초기화 (콜백 연결 + 초기 렌더 + 이벤트 구독)
        calendarController.initialize();
    }

    public void showCalendarPage() {
        navigationPanel.setActiveTab(NavigationPanel.TAB_CALENDAR);
        cardLayout.show(contentPanel, CARD_CALENDAR);
    }

    public void showSubjectPage() {
        navigationPanel.setActiveTab(NavigationPanel.TAB_SUBJECT);
        cardLayout.show(contentPanel, CARD_SUBJECT);
    }

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
