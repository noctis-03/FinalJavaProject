package com.blople;

import com.blople.common.EventBus;
import com.blople.database.DatabaseManager;
import com.blople.database.TransactionManager;
import com.blople.database.dao.BlockDAO;
import com.blople.database.dao.ScheduleBlockDAO;
import com.blople.model.block.ScheduleBlock;
import com.blople.model.calendar.CalendarModel;
import com.blople.model.enums.ViewMode;
import com.blople.service.CalendarService;
import com.blople.view.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.time.LocalDate;
import java.util.List;

public class App {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Look & Feel 설정 실패: " + e.getMessage());
        }

        DatabaseManager dbManager = DatabaseManager.getInstance();
        dbManager.initialize();

        // ⭐ 임시: CalendarService 스모크 테스트
        runCalendarServiceSmokeTest(dbManager);

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    private static void runCalendarServiceSmokeTest(DatabaseManager dbManager) {
        System.out.println("\n========== CalendarService Smoke Test ==========");

        // 의존성 조립
        TransactionManager tx       = new TransactionManager(dbManager);
        BlockDAO blockDAO           = new BlockDAO(dbManager);
        ScheduleBlockDAO scheduleDAO = new ScheduleBlockDAO(dbManager, blockDAO, tx);

        // 이전 테스트 잔여 데이터 정리
        scheduleDAO.findAll().forEach(b -> scheduleDAO.deleteById(b.getId()));

        CalendarModel   model   = new CalendarModel();
        CalendarService service = new CalendarService(model, scheduleDAO);

        // EventBus 구독 — 어떤 이벤트가 발행되는지 콘솔로 확인
        EventBus bus = EventBus.getInstance();
        bus.subscribe(EventBus.SCHEDULE_BLOCK_CREATED, p ->
                System.out.println("  📡 CREATED 이벤트: " + p));
        bus.subscribe(EventBus.SCHEDULE_BLOCK_MOVED, p ->
                System.out.println("  📡 MOVED 이벤트: "   + p));
        bus.subscribe(EventBus.SCHEDULE_BLOCK_DELETED, p ->
                System.out.println("  📡 DELETED 이벤트: " + p));
        bus.subscribe(EventBus.CALENDAR_VIEW_CHANGED, p ->
                System.out.println("  📡 VIEW_CHANGED: "   + p));

        // 1) 일정 추가
        LocalDate testDate = LocalDate.of(2026, 5, 15);
        ScheduleBlock b1 = new ScheduleBlock("자바 강의",  "#4D96FF",
                testDate + " 09:00:00", testDate + " 10:30:00");
        ScheduleBlock b2 = new ScheduleBlock("운영체제 강의", "#FF6B6B",
                testDate + " 11:00:00", testDate + " 12:30:00");
        service.addScheduleBlock(b1);
        service.addScheduleBlock(b2);
        System.out.println("[1] 추가 완료 → 5/15 일정 = "
                + service.getBlocksForDate(testDate).size() + "건");

        // 2) 충돌 감지 — b3 는 b1 과 겹친다
        ScheduleBlock b3 = new ScheduleBlock("팀플", "#6BCB77",
                testDate + " 09:30:00", testDate + " 10:00:00");
        List<ScheduleBlock> conflicts = service.detectConflicts(b3);
        System.out.println("[2] b3 충돌 감지 = " + conflicts.size()
                + "건 (예상 1건: " + (conflicts.isEmpty() ? "없음" : conflicts.get(0).getName()) + ")");

        // 3) 블록 이동
        service.moveBlock(b1.getId(),
                testDate + " 13:00:00", testDate + " 14:30:00");
        ScheduleBlock moved = service.findById(b1.getId()).orElseThrow();
        System.out.println("[3] b1 이동 후 시작 시각 = " + moved.getStartDatetime());

        // 4) 뷰 모드 전환
        service.switchViewMode(ViewMode.MONTH);
        System.out.println("[4] 5월 전체 일정 수 = "
                + service.getBlocksForMonth(2026, 5).size());

        // 5) 다음 달 이동
        service.navigateNext();
        System.out.println("[5] 6월 전체 일정 수 = "
                + service.getBlocksForMonth(2026, 6).size());

        // 6) 삭제
        service.removeBlock(b1.getId());
        service.removeBlock(b2.getId());
        System.out.println("[6] 삭제 후 5월 일정 수 = "
                + service.getBlocksForMonth(2026, 5).size());

        bus.clear();
        System.out.println("========== Smoke Test 종료 ==========\n");
    }
}
