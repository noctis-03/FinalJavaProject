package com.blople;

import com.blople.database.DatabaseManager;
import com.blople.database.TransactionManager;
import com.blople.database.dao.BlockDAO;
import com.blople.database.dao.ScheduleBlockDAO;
import com.blople.model.block.ScheduleBlock;
import com.blople.view.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.List;

public class App {

    public static void main(String[] args) {
        // Look & Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Look & Feel 설정 실패: " + e.getMessage());
        }

        // DB 초기화
        DatabaseManager dbManager = DatabaseManager.getInstance();
        dbManager.initialize();

        // ⭐ 임시 DAO 동작 검증 (다음 단계에서 제거)
        runDaoSmokeTest(dbManager);

        // Swing UI
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    /** DAO 동작이 정상인지 빠르게 확인하는 스모크 테스트. */
    private static void runDaoSmokeTest(DatabaseManager dbManager) {
        System.out.println("\n========== DAO Smoke Test 시작 ==========");

        TransactionManager txManager  = new TransactionManager(dbManager);
        BlockDAO           blockDAO   = new BlockDAO(dbManager);
        ScheduleBlockDAO   scheduleDAO = new ScheduleBlockDAO(dbManager, blockDAO, txManager);

        // 1) INSERT
        ScheduleBlock block = new ScheduleBlock(
                "자바프로그래밍 강의",
                "#4D96FF",
                "2026-05-15 09:00:00",
                "2026-05-15 10:30:00"
        );
        block.setMemo("정보관 401호");
        scheduleDAO.insert(block);
        System.out.println("[1] INSERT 완료 → id=" + block.getId());

        // 2) SELECT (by id)
        var found = scheduleDAO.findById(block.getId());
        System.out.println("[2] SELECT 결과 → " + found.orElse(null));

        // 3) UPDATE
        block.setName("자바프로그래밍 강의 (수정됨)");
        block.setEndDatetime("2026-05-15 11:00:00");
        scheduleDAO.update(block);
        var updated = scheduleDAO.findById(block.getId()).orElseThrow();
        System.out.println("[3] UPDATE 완료 → name=" + updated.getName()
                + ", 종료=" + updated.getEndDatetime());

        // 4) SELECT (range)
        List<ScheduleBlock> inRange = scheduleDAO.findInRange(
                "2026-05-15 00:00:00", "2026-05-16 00:00:00");
        System.out.println("[4] 5/15 하루치 일정 개수 = " + inRange.size());

        // 5) DELETE
        boolean removed = scheduleDAO.deleteById(block.getId());
        System.out.println("[5] DELETE 결과 = " + removed
                + ", 남은 전체 일정 = " + scheduleDAO.findAll().size());

        System.out.println("========== DAO Smoke Test 종료 ==========\n");
    }
}
