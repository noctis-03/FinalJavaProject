package com.blople.view;

import com.blople.common.Constants;
import com.blople.controller.CalendarController;
import com.blople.controller.SubjectController;
import com.blople.database.DatabaseManager;
import com.blople.database.TransactionManager;
import com.blople.database.dao.AssignmentDAO;
import com.blople.database.dao.AutomationBlockDAO;
import com.blople.database.dao.AutomationRuleDAO;
import com.blople.database.dao.BlockDAO;
import com.blople.database.dao.CalendarAutomationDAO;
import com.blople.database.dao.CanvasStateDAO;
import com.blople.database.dao.ExecutionLogDAO;
import com.blople.database.dao.FileBlockDAO;
import com.blople.database.dao.LinkBlockDAO;
import com.blople.database.dao.PresetBlockDAO;
import com.blople.database.dao.ScheduleBlockDAO;
import com.blople.database.dao.SemesterDAO;
import com.blople.database.dao.SubjectDAO;
import com.blople.database.dao.TextBlockDAO;
import com.blople.database.dao.TimetableDAO;
import com.blople.model.calendar.CalendarModel;
import com.blople.service.AssignmentService;
import com.blople.service.AutomationService;
import com.blople.service.CalendarAutomationService;
import com.blople.service.CalendarService;
import com.blople.service.CanvasService;
import com.blople.service.FileBlockService;
import com.blople.service.PresetBlockService;
import com.blople.service.SubjectService;
import com.blople.service.TimetableService;
import com.blople.view.calendar.CalendarPagePanel;
import com.blople.view.subject.SubjectPagePanel;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

public class MainFrame extends JFrame {

    public static final String CARD_CALENDAR = "CALENDAR";
    public static final String CARD_SUBJECT  = "SUBJECT";

    private final DatabaseManager dbManager;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel   = new JPanel(cardLayout);
    private final NavigationPanel navigationPanel;

    private final CalendarPagePanel  calendarPagePanel;
    private final CalendarController calendarController;

    private final SubjectPagePanel  subjectPagePanel;
    private final SubjectController  subjectController;

    public MainFrame(DatabaseManager dbManager) {
        super(Constants.APP_NAME + " v" + Constants.APP_VERSION);
        this.dbManager = dbManager;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(Constants.MAIN_WINDOW_WIDTH, Constants.MAIN_WINDOW_HEIGHT);
        setMinimumSize(new Dimension(Constants.MIN_WINDOW_WIDTH, Constants.MIN_WINDOW_HEIGHT));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        TransactionManager tx        = new TransactionManager(dbManager);
        BlockDAO blockDAO            = new BlockDAO(dbManager);
        ScheduleBlockDAO scheduleDAO = new ScheduleBlockDAO(dbManager, blockDAO, tx);

        CalendarModel model         = new CalendarModel();
        CalendarService calendarSvc = new CalendarService(model, scheduleDAO);

        PresetBlockDAO     presetDAO = new PresetBlockDAO(dbManager, blockDAO, tx);
        PresetBlockService presetSvc = new PresetBlockService(presetDAO, calendarSvc);

        ExecutionLogDAO         executionLogDAO     = new ExecutionLogDAO(dbManager);
        CalendarAutomationDAO   calAutomationDAO    = new CalendarAutomationDAO(dbManager, tx);
        CalendarAutomationService calAutomationSvc  = new CalendarAutomationService(
                calAutomationDAO, executionLogDAO, presetDAO, presetSvc);

        com.blople.database.dao.ExternalLinkDAO externalLinkDAO =
                new com.blople.database.dao.ExternalLinkDAO(dbManager);
        com.blople.service.ExternalLinkService externalLinkSvc =
                new com.blople.service.ExternalLinkService(externalLinkDAO);
        externalLinkSvc.seedDefaultsIfEmpty();

        calendarPagePanel  = new CalendarPagePanel(externalLinkSvc);
        calendarController = new CalendarController(
                calendarSvc, presetSvc, calAutomationSvc, calendarPagePanel);

        SemesterDAO    semesterDAO    = new SemesterDAO(dbManager);
        SubjectDAO     subjectDAO     = new SubjectDAO(dbManager);
        TimetableDAO   timetableDAO   = new TimetableDAO(dbManager);
        AssignmentDAO  assignmentDAO  = new AssignmentDAO(dbManager, blockDAO, tx);
        FileBlockDAO   fileBlockDAO   = new FileBlockDAO(dbManager, blockDAO, tx);
        TextBlockDAO   textBlockDAO   = new TextBlockDAO(dbManager, blockDAO, tx);
        LinkBlockDAO   linkBlockDAO   = new LinkBlockDAO(dbManager, blockDAO, tx);
        CanvasStateDAO canvasStateDAO = new CanvasStateDAO(dbManager);

        AutomationRuleDAO  automationRuleDAO  = new AutomationRuleDAO(dbManager, tx);
        AutomationBlockDAO automationBlockDAO = new AutomationBlockDAO(dbManager, blockDAO, tx);

        SubjectService    subjectSvc    = new SubjectService(semesterDAO, subjectDAO);
        TimetableService  timetableSvc  = new TimetableService(timetableDAO, scheduleDAO, subjectSvc);
        AssignmentService assignmentSvc = new AssignmentService(assignmentDAO, scheduleDAO);
        FileBlockService  fileBlockSvc  = new FileBlockService(fileBlockDAO);
        CanvasService     canvasSvc     = new CanvasService(
                textBlockDAO, linkBlockDAO, fileBlockDAO, canvasStateDAO);
        AutomationService automationSvc = new AutomationService(
                automationBlockDAO, automationRuleDAO, executionLogDAO,
                canvasStateDAO, fileBlockDAO, linkBlockDAO);

        subjectPagePanel  = new SubjectPagePanel();
        subjectController = new SubjectController(
                subjectSvc, timetableSvc, assignmentSvc, fileBlockSvc,
                canvasSvc, automationSvc, subjectPagePanel);

        navigationPanel = new NavigationPanel();
        navigationPanel.setOnCalendarTabClick(e -> showCalendarPage());
        navigationPanel.setOnSubjectTabClick (e -> showSubjectPage());
        navigationPanel.setOnResetAppClick   (e -> onResetApp());

        contentPanel.add(calendarPagePanel, CARD_CALENDAR);
        contentPanel.add(subjectPagePanel,  CARD_SUBJECT);

        add(navigationPanel, BorderLayout.NORTH);
        add(contentPanel,    BorderLayout.CENTER);

        showCalendarPage();

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

    /**
     * 프로그램 전체 초기화 (요구 #6).
     * 모든 사용자 데이터를 비우고 메인 프레임을 재시작한다.
     */
    private void onResetApp() {
        int ok = JOptionPane.showConfirmDialog(this,
                "정말로 모든 데이터를 초기화할까요?\n"
                        + "학기/과목/시간표/과제/캔버스/자동화/외부 링크가 전부 삭제되며\n"
                        + "이 작업은 되돌릴 수 없습니다.",
                "프로그램 초기화", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        // 한번 더 확인
        int ok2 = JOptionPane.showConfirmDialog(this,
                "최종 확인: 정말 진행하시겠습니까?",
                "프로그램 초기화", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok2 != JOptionPane.YES_OPTION) return;

        try {
            dbManager.wipeAllData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "초기화 실패: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "초기화가 완료되었습니다.\n프로그램을 다시 시작합니다.",
                "프로그램 초기화", JOptionPane.INFORMATION_MESSAGE);

        // 현재 창 닫고 새 MainFrame 띄우기
        navigationPanel.dispose();
        dispose();
        SwingUtilities.invokeLater(() -> {
            MainFrame fresh = new MainFrame(dbManager);
            fresh.setVisible(true);
        });
    }
}
