package com.blople.controller;

import com.blople.common.EventBus;
import com.blople.dnd.CalendarDropTarget;
import com.blople.model.automation.CalendarAutomationRule;
import com.blople.model.automation.ExecutionLog;
import com.blople.model.block.PresetBlock;
import com.blople.model.block.ScheduleBlock;
import com.blople.model.calendar.CalendarModel;
import com.blople.model.enums.ViewMode;
import com.blople.service.CalendarAutomationService;
import com.blople.service.CalendarService;
import com.blople.service.PresetBlockService;
import com.blople.view.calendar.CalendarPagePanel;
import com.blople.view.calendar.PresetBlockCreateDialog;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 캘린더 페이지 컨트롤러 (FR-01, FR-02, FR-11, FR-12).
 */
public class CalendarController {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("yyyy년 M월");
    private static final DateTimeFormatter TS_FMT_LOCAL = ScheduleBlock.TS_FORMATTER;

    private final CalendarService service;
    private final PresetBlockService presetService;
    private final CalendarAutomationService automationService;
    private final CalendarPagePanel view;
    private final CalendarModel model;
    private final EventBus eventBus = EventBus.getInstance();

    private CalendarDropTarget calendarDropTarget;

    public CalendarController(CalendarService service,
                              PresetBlockService presetService,
                              CalendarAutomationService automationService,
                              CalendarPagePanel view) {
        this.service = service;
        this.presetService = presetService;
        this.automationService = automationService;
        this.view = view;
        this.model = service.getCalendarModel();
    }

    /** MainFrame 조립 후 1회 호출 — 콜백 연결 + 드롭 타겟 등록 + 초기 렌더 + 이벤트 구독. */
    public void initialize() {
        wireCallbacks();
        subscribeEvents();
        registerDropTarget();
        syncViewState();
        refreshView();
        reloadPresets();
        reloadAutomations();
    }

    private void wireCallbacks() {
        view.setOnPrev(this::onNavigatePrev);
        view.setOnNext(this::onNavigateNext);
        view.setOnToday(this::onToday);
        view.setOnViewToggle(this::onViewToggle);
        view.setOnBlockClick(this::onBlockClick);
        view.setOnDateClick(this::onDateClick);

        view.setOnPresetCreate(this::onPresetCreate);
        view.setOnPresetPlace(this::onPresetPlace);
        view.setOnPresetDelete(this::onPresetDelete);

        // 자동화 (FR-12)
        view.setOnAutomationAdd(this::onAutomationAdd);
        view.setOnAutomationRuleChange(this::onAutomationRuleChange);
        view.setOnAutomationRun(this::onAutomationRun);
        view.setOnAutomationDelete(this::onAutomationDelete);
    }

    private void subscribeEvents() {
        eventBus.subscribe(EventBus.SCHEDULE_BLOCK_CREATED, p -> refreshOnEdt());
        eventBus.subscribe(EventBus.SCHEDULE_BLOCK_UPDATED, p -> refreshOnEdt());
        eventBus.subscribe(EventBus.SCHEDULE_BLOCK_MOVED,   p -> refreshOnEdt());
        eventBus.subscribe(EventBus.SCHEDULE_BLOCK_DELETED, p -> refreshOnEdt());
    }

    private void registerDropTarget() {
        calendarDropTarget = new CalendarDropTarget(view.getWeekViewPanel());
        calendarDropTarget.setOnDrop(this::onPresetDropped);
    }

    // ===== 네비게이션 / 뷰 =====
    private void onNavigatePrev() {
        service.navigatePrev();
        syncViewState();
        refreshView();
    }

    private void onNavigateNext() {
        service.navigateNext();
        syncViewState();
        refreshView();
    }

    private void onToday() {
        service.goToDate(LocalDate.now());
        syncViewState();
        refreshView();
    }

    private void onViewToggle() {
        ViewMode next = (model.getCurrentViewMode() == ViewMode.WEEK)
                ? ViewMode.MONTH : ViewMode.WEEK;
        service.switchViewMode(next);
        syncViewState();
        refreshView();
    }

    private void onBlockClick(ScheduleBlock block) {
        String msg = "일정: %s\n%s ~ %s".formatted(
                block.getName(), block.getStartDatetime(), block.getEndDatetime());
        JOptionPane.showMessageDialog(view, msg, "일정 상세", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onDateClick(LocalDate date) {
        service.switchViewMode(ViewMode.WEEK);
        service.goToDate(date);
        syncViewState();
        refreshView();
    }

    // ===== 미리블록 (FR-02) =====
    private void reloadPresets() {
        List<PresetBlock> presets = presetService.getAll();
        view.setPresets(presets);
    }

    private void onPresetCreate() {
        Window owner = SwingUtilities.getWindowAncestor(view);
        PresetBlockCreateDialog dialog = new PresetBlockCreateDialog(owner);
        dialog.setOnCreate(form -> {
            try {
                presetService.create(form.name(), form.colorHex(),
                        form.durationMinutes(), form.category());
                reloadPresets();
                reloadAutomations(); // 미리블록이 생기면 자동화 콤보도 갱신
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(owner, ex.getMessage(),
                        "생성 실패", JOptionPane.WARNING_MESSAGE);
            }
        });
        dialog.setVisible(true);
    }

    private void onPresetPlace(PresetBlock preset) {
        LocalDate date = model.getCurrentDate();
        String start = date.atTime(9, 0).format(TS_FMT_LOCAL);
        try {
            presetService.placeOnCalendar(preset.getId(), start);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(view, ex.getMessage(),
                    "배치 실패", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onPresetDelete(PresetBlock preset) {
        int ok = JOptionPane.showConfirmDialog(view,
                "미리블록 '" + preset.getName() + "' 을(를) 삭제할까요?\n(이미 배치된 일정에는 영향 없음)",
                "미리블록 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        presetService.delete(preset.getId());
        reloadPresets();
        reloadAutomations();
    }

    // ===== 드래그앤드롭 배치 (FR-11) =====
    private void onPresetDropped(String presetBlockId, LocalDateTime when) {
        try {
            String start = when.format(TS_FMT_LOCAL);
            presetService.placeOnCalendar(presetBlockId, start);
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            "배치 실패", JOptionPane.WARNING_MESSAGE));
        }
    }

    // ===== 자동화 (FR-12) =====
    private void reloadAutomations() {
        List<CalendarAutomationRule> rules = automationService.getAllRules();
        view.setAutomationRules(rules);
    }

    private void onAutomationAdd() {
        automationService.createRule("캘린더 자동화");
        reloadAutomations();
    }

    /** 콤보(요일/미리블록) 변경 시 즉시 저장. */
    private void onAutomationRuleChange(CalendarAutomationRule rule) {
        automationService.updateRule(rule);
    }

    private void onAutomationRun(CalendarAutomationRule rule) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        ExecutionLog log = automationService.execute(rule.getId());
        int type = log.isSuccess()
                ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE;
        JOptionPane.showMessageDialog(owner, log.getResultMessage(),
                log.isSuccess() ? "자동화 실행 완료" : "자동화 실행 실패", type);
        // 성공 시 등록된 일정은 SCHEDULE_BLOCK_CREATED 이벤트로 캘린더 자동 갱신됨
    }

    private void onAutomationDelete(CalendarAutomationRule rule) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        int ok = JOptionPane.showConfirmDialog(owner,
                "이 자동화를 삭제할까요?",
                "자동화 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        automationService.deleteRule(rule.getId());
        reloadAutomations();
    }

    // ===== View 상태 동기화 =====
    private void syncViewState() {
        ViewMode mode = model.getCurrentViewMode();
        LocalDate cur = model.getCurrentDate();

        view.setViewMode(mode);
        view.updateViewToggleLabel(mode);

        if (mode == ViewMode.WEEK) {
            view.setWeekBaseDate(cur);
            view.updateDateLabel(weekLabel(cur));
        } else {
            YearMonth ym = YearMonth.from(cur);
            view.setMonthYearMonth(ym.getYear(), ym.getMonthValue());
            view.updateDateLabel(cur.format(MONTH_LABEL));
        }
    }

    private String weekLabel(LocalDate date) {
        var ws = model.getWeekView().getWeekStartDate();
        var we = model.getWeekView().getWeekEndDate();
        return "%d년 %d.%d ~ %d.%d".formatted(
                ws.getYear(), ws.getMonthValue(), ws.getDayOfMonth(),
                we.getMonthValue(), we.getDayOfMonth());
    }

    // ===== 렌더 =====
    public void refreshView() {
        List<ScheduleBlock> blocks = model.getScheduleBlocks();
        view.refresh(blocks);
    }

    private void refreshOnEdt() {
        if (SwingUtilities.isEventDispatchThread()) {
            service.reloadCacheForCurrentView();
            refreshView();
        } else {
            SwingUtilities.invokeLater(() -> {
                service.reloadCacheForCurrentView();
                refreshView();
            });
        }
    }
}
