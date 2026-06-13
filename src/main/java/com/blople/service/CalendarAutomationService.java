package com.blople.service;

import com.blople.common.EventBus;
import com.blople.database.dao.CalendarAutomationDAO;
import com.blople.database.dao.ExecutionLogDAO;
import com.blople.database.dao.PresetBlockDAO;
import com.blople.model.automation.CalendarAutomationRule;
import com.blople.model.automation.ExecutionLog;
import com.blople.model.block.PresetBlock;
import com.blople.model.block.ScheduleBlock;

import java.util.List;
import java.util.Optional;

/**
 * 캘린더 페이지 자동화 서비스 (FR-12).
 *
 * <p>"다음 주의 [요일]요일에 [미리블록] 일정을 추가한다" 규칙을 관리·실행한다.
 * 책임:
 * <ul>
 *   <li>캘린더 자동화 규칙 생성/수정/삭제/조회 (영속 저장)</li>
 *   <li>실행 — 규칙이 가리키는 다음 주 해당 요일 09:00 에 미리블록 일정 등록</li>
 *   <li>실행 결과를 execution_log 에 기록</li>
 * </ul>
 *
 * <p>일정 등록은 {@link PresetBlockService#placeOnCalendar(String, String)} 를 재사용하므로,
 * 등록된 일정은 SCHEDULE_BLOCK_CREATED 이벤트로 캘린더에 즉시 반영된다.
 */
public class CalendarAutomationService {

    private final CalendarAutomationDAO calendarAutomationDAO;
    private final ExecutionLogDAO       executionLogDAO;
    private final PresetBlockDAO        presetBlockDAO;
    private final PresetBlockService    presetBlockService;
    private final EventBus eventBus = EventBus.getInstance();

    public CalendarAutomationService(CalendarAutomationDAO calendarAutomationDAO,
                                     ExecutionLogDAO executionLogDAO,
                                     PresetBlockDAO presetBlockDAO,
                                     PresetBlockService presetBlockService) {
        this.calendarAutomationDAO = calendarAutomationDAO;
        this.executionLogDAO       = executionLogDAO;
        this.presetBlockDAO        = presetBlockDAO;
        this.presetBlockService    = presetBlockService;
    }

    // ===== 조회 =====
    public List<CalendarAutomationRule> getAllRules() {
        return calendarAutomationDAO.findAllCalendar();
    }

    public Optional<CalendarAutomationRule> getRule(String ruleId) {
        return calendarAutomationDAO.findById(ruleId);
    }

    // ===== 생성/수정/삭제 =====

    /** 빈 규칙 생성(요일·미리블록 미설정, 다음 주 기본). */
    public CalendarAutomationRule createRule(String name) {
        CalendarAutomationRule rule = new CalendarAutomationRule(name);
        calendarAutomationDAO.insert(rule);
        eventBus.publish(EventBus.AUTOMATION_EXECUTED, "calendar-automation-created");
        return rule;
    }

    /** 슬롯 값(요일·미리블록·주 오프셋)·이름·활성 상태 갱신. */
    public void updateRule(CalendarAutomationRule rule) {
        calendarAutomationDAO.update(rule);
        eventBus.publish(EventBus.AUTOMATION_EXECUTED, "calendar-automation-updated");
    }

    public void deleteRule(String ruleId) {
        boolean ok = calendarAutomationDAO.deleteById(ruleId);
        if (ok) eventBus.publish(EventBus.AUTOMATION_EXECUTED, "calendar-automation-deleted");
    }

    // ===== 실행 =====

    /**
     * 규칙 실행 — 다음 주 해당 요일 09:00 에 미리블록 일정을 등록한다.
     * 미완성/오류 시 예외 없이 실패 로그로 남기고 반환한다.
     *
     * @return 실행 이력(성공 메시지 또는 실패 사유 포함)
     */
    public ExecutionLog execute(String ruleId) {
        Optional<CalendarAutomationRule> opt = calendarAutomationDAO.findById(ruleId);
        if (opt.isEmpty()) {
            return logFail(ruleId, "규칙을 찾을 수 없습니다.");
        }
        CalendarAutomationRule rule = opt.get();

        if (!rule.isComplete()) {
            return logFail(ruleId, "요일과 미리블록을 모두 선택해야 실행할 수 있습니다.");
        }

        Optional<PresetBlock> presetOpt = presetBlockDAO.findById(rule.getPresetBlockId());
        if (presetOpt.isEmpty()) {
            return logFail(ruleId, "연결된 미리블록이 삭제되었습니다.");
        }

        String start = rule.resolveStartDatetime();
        if (start == null) {
            return logFail(ruleId, "실행 시각을 계산할 수 없습니다.");
        }

        try {
            ScheduleBlock created = presetBlockService.placeOnCalendar(rule.getPresetBlockId(), start);
            String msg = "%s %s에 '%s' 일정을 추가했습니다.".formatted(
                    dateLabel(start), timeLabel(start), created.getName());
            return logSuccess(ruleId, msg);
        } catch (Exception e) {
            return logFail(ruleId, "일정 등록 중 오류: " + e.getMessage());
        }
    }

    public List<ExecutionLog> getHistory(String ruleId) {
        return executionLogDAO.findByRule(ruleId);
    }

    // ===== 내부 헬퍼 =====
    private ExecutionLog logSuccess(String ruleId, String message) {
        ExecutionLog log = new ExecutionLog(ruleId, ExecutionLog.STATUS_SUCCESS, message);
        executionLogDAO.insert(log);
        eventBus.publish(EventBus.AUTOMATION_EXECUTED, log);
        return log;
    }

    private ExecutionLog logFail(String ruleId, String message) {
        ExecutionLog log = new ExecutionLog(ruleId, ExecutionLog.STATUS_FAILED, message);
        executionLogDAO.insert(log);
        eventBus.publish(EventBus.AUTOMATION_EXECUTED, log);
        return log;
    }

    /** "yyyy-MM-dd HH:mm:ss" → "M월 d일(요일)" 형태 라벨. */
    private static String dateLabel(String ts) {
        java.time.LocalDateTime dt =
                java.time.LocalDateTime.parse(ts, CalendarAutomationRule.TS_FMT);
        String[] dow = {"월", "화", "수", "목", "금", "토", "일"};
        // DayOfWeek: MON=1..SUN=7 → 배열 인덱스 0..6
        String d = dow[dt.getDayOfWeek().getValue() - 1];
        return "%d월 %d일(%s)".formatted(dt.getMonthValue(), dt.getDayOfMonth(), d);
    }

    private static String timeLabel(String ts) {
        return ts.length() >= 16 ? ts.substring(11, 16) : ts;
    }
}
