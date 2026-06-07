package com.blople.view;

import com.blople.common.Constants;
import com.blople.controller.CalendarController;
import com.blople.controller.SubjectController;
import com.blople.database.DatabaseManager;
import com.blople.database.TransactionManager;
import com.blople.database.dao.AssignmentDAO;
import com.blople.database.dao.BlockDAO;
import com.blople.database.dao.FileBlockDAO;
import com.blople.database.dao.PresetBlockDAO;
import com.blople.database.dao.ScheduleBlockDAO;
import com.blople.database.dao.SemesterDAO;
import com.blople.database.dao.SubjectDAO;
import com.blople.database.dao.TimetableDAO;
import com.blople.model.calendar.CalendarModel;
import com.blople.service.AssignmentService;
import com.blople.service.CalendarService;
import com.blople.service.FileBlockService;
import com.blople.service.PresetBlockService;
import com.blople.service.SubjectService;
import com.blople.service.TimetableService;
import com.blople.view.calendar.CalendarPagePanel;
import com.blople.view.subject.SubjectPagePanel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

/**
 * 메인 윈도우 프레임.
 * 상단 NavigationPanel + 캘린더/과목 페이지를 CardLayout 으로 전환한다.
 *
 * <p>v0.2: 캘린더 = {@link CalendarPagePanel} + {@link CalendarController}
 * <p>v0.3: 과목/시간표 = {@link SubjectPagePanel} + {@link SubjectController}
 * <p>v0.4: 과제 블록(AssignmentService) + 파일 블록(FileBlockService)
 * <p>v0.5: 미리블록(PresetBlockService)
 */
public class MainFrame extends JFrame {

    public static final String CARD_CALENDAR = "CALENDAR";
    public static final String CARD_SUBJECT  = "SUBJECT";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel   = new JPanel(cardLayout);
    private final NavigationPanel navigationPanel;

    private final CalendarPagePanel  calendarPagePanel;
    private final CalendarController calendarController;

    private final SubjectPagePanel   subjectPagePanel;
    private final SubjectController   subjectController;

    public MainFrame(DatabaseManager dbManager) {
        super(Constants.APP_NAME + " v" + Constants.APP_VERSION);

        // === 기본 설정 ===
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(Constants.MAIN_WINDOW_WIDTH, Constants.MAIN_WINDOW_HEIGHT);
        setMinimumSize(new Dimension(Constants.MIN_WINDOW_WIDTH, Constants.MIN_WINDOW_HEIGHT));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // === 공용 인프라 ===
        TransactionManager tx        = new TransactionManager(dbManager);
        BlockDAO blockDAO            = new BlockDAO(dbManager);
        ScheduleBlockDAO scheduleDAO = new ScheduleBlockDAO(dbManager, blockDAO, tx);

        // === 의존성 조립 (캘린더 + 미리블록) ===
        CalendarModel model         = new CalendarModel();
        CalendarService calendarSvc = new CalendarService(model, scheduleDAO);

        PresetBlockDAO     presetDAO = new PresetBlockDAO(dbManager, blockDAO, tx);
        PresetBlockService presetSvc = new PresetBlockService(presetDAO, calendarSvc);

        calendarPagePanel  = new CalendarPagePanel();
        calendarController = new CalendarController(calendarSvc, presetSvc, calendarPagePanel);

        // === 의존성 조립 (과목 / 시간표 / 과제 / 파일) ===
        SemesterDAO   semesterDAO   = new SemesterDAO(dbManager);
        SubjectDAO    subjectDAO    = new SubjectDAO(dbManager);
        TimetableDAO  timetableDAO  = new TimetableDAO(dbManager);
        AssignmentDAO assignmentDAO = new AssignmentDAO(dbManager, blockDAO, tx);
        FileBlockDAO  fileBlockDAO  = new FileBlockDAO(dbManager, blockDAO, tx);

        SubjectService    subjectSvc    = new SubjectService(semesterDAO, subjectDAO);
        TimetableService  timetableSvc  = new TimetableService(timetableDAO, scheduleDAO, subjectSvc);
        AssignmentService assignmentSvc = new AssignmentService(assignmentDAO, scheduleDAO);
        FileBlockService  fileBlockSvc  = new FileBlockService(fileBlockDAO);

        subjectPagePanel  = new SubjectPagePanel();
        subjectController = new SubjectController(
                subjectSvc, timetableSvc, assignmentSvc, fileBlockSvc, subjectPagePanel);

        // === 상단 네비게이션 ===
        navigationPanel = new NavigationPanel();
        navigationPanel.setOnCalendarTabClick(e -> showCalendarPage());
        navigationPanel.setOnSubjectTabClick (e -> showSubjectPage());

        // === 컨텐츠 영역 (CardLayout) ===
        contentPanel.add(calendarPagePanel, CARD_CALENDAR);
        contentPanel.add(subjectPagePanel,  CARD_SUBJECT);

        add(navigationPanel, BorderLayout.NORTH);
        add(contentPanel,    BorderLayout.CENTER);

        // 초기 화면: 캘린더
        showCalendarPage();

        // === 컨트롤러 초기화 (콜백 연결 + 초기 렌더 + 이벤트 구독) ===
        calendarController.initialize();
        subjectController.initialize();
    }

    public void showCalendarPage() {
        navigationPanel.setActiveTab(NavigationPanel.TAB_CALENDAR);
        cardLayout.show(contentPanel, CARD_CALENDAR);
    }

    public void showSubjectPage() {
        navigationPanel.setActiveTab(NavigationPanel.TAB_SUBJECT);
        cardLayout.show(contentPanel, CARD_SUBJECT);
    }
}
