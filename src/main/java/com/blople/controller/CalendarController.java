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
import com.blople.view.calendar.ScheduleBlockEditDialog;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
        view.setOnBlockDoubleClick(this::onBlockEdit);
        view.setOnBlockDelete(this::onBlockDeleteByObject);
        view.setOnBlockMove(this::onBlockMoveByDrag);
        view.setOnEmptySlotDoubleClick(this::onEmptySlotDoubleClick);
        view.setOnDateClick(this::onDateClick);

        view.setOnPresetCreate(this::onPresetCreate);
        view.setOnPresetPlace(this::onPresetPlace);
        view.setOnPresetDelete(this::onPresetDelete);

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
        eventBus.subscribe(EventBus.SUBJECT_CHANGED,        p -> refreshOnEdt());
    }

    private void registerDropTarget() {
        calendarDropTarget = new CalendarDropTarget(view.getWeekViewPanel());
        calendarDropTarget.setOnDrop(this::onPresetDropped);
    }

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
        // 단일 클릭은 선택만(상세 표시는 더블클릭). 안내 토스트 대신 무동작.
    }

    private void onBlockEdit(ScheduleBlock block) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        ScheduleBlockEditDialog dialog = new ScheduleBlockEditDialog(owner, block);
        dialog.setOnSave(form -> {
            try {
                ScheduleBlock updated = service.findById(block.getId()).orElseThrow();
                updated.setName(form.name());
                updated.setColor(form.colorHex());
                updated.setStartDatetime(form.startDatetime());
                updated.setEndDatetime(form.endDatetime());
                service.updateBlock(updated);
            } catch (CalendarService.ScheduleConflictException ex) {
                JOptionPane.showMessageDialog(owner, ex.getMessage(),
                        "시간 충돌", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(owner, ex.getMessage(),
                        "수정 실패", JOptionPane.WARNING_MESSAGE);
            }
        });
        dialog.setOnDelete(() -> service.removeBlock(block.getId()));
        dialog.setVisible(true);
    }

    private void onBlockDeleteByObject(ScheduleBlock block) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        int ok = JOptionPane.showConfirmDialog(owner,
                "이 일정을 삭제할까요?\n" + block.getName(),
                "일정 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        service.removeBlock(block.getId());
    }

    private void onBlockMoveByDrag(String blockId, LocalDateTime newStart, LocalDateTime newEnd) {
        try {
            service.moveBlock(blockId,
                    newStart.format(TS_FMT_LOCAL),
                    newEnd.format(TS_FMT_LOCAL));
        } catch (CalendarService.ScheduleConflictException ex) {
            JOptionPane.showMessageDialog(view, ex.getMessage(),
                    "시간 충돌", JOptionPane.WARNING_MESSAGE);
            refreshOnEdt(); // 원래 위치로 되돌리기
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(view, ex.getMessage(),
                    "이동 실패", JOptionPane.WARNING_MESSAGE);
            refreshOnEdt();
        }
    }

    private void onEmptySlotDoubleClick(LocalDateTime when) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        // 기본 1시간짜리 빈 일정으로 편집창 열기
        ScheduleBlock draft = new ScheduleBlock("새 일정", "#4D96FF",
                when.format(TS_FMT_LOCAL), when.plusHours(1).format(TS_FMT_LOCAL));
        ScheduleBlockEditDialog dialog = new ScheduleBlockEditDialog(owner, draft);
        dialog.setOnSave(form -> {
            try {
                ScheduleBlock nb = new ScheduleBlock(form.name(), form.colorHex(),
                        form.startDatetime(), form.endDatetime());
                service.addScheduleBlock(nb);
            } catch (CalendarService.ScheduleConflictException ex) {
                JOptionPane.showMessageDialog(owner, ex.getMessage(),
                        "시간 충돌", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(owner, ex.getMessage(),
                        "생성 실패", JOptionPane.WARNING_MESSAGE);
            }
        });
        dialog.setVisible(true);
    }

    private void onDateClick(LocalDate date) {
        service.switchViewMode(ViewMode.WEEK);
        service.goToDate(date);
        syncViewState();
        refreshView();
    }

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
                reloadAutomations();
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
        } catch (CalendarService.ScheduleConflictException ex) {
            JOptionPane.showMessageDialog(view, ex.getMessage(),
                    "시간 충돌", JOptionPane.WARNING_MESSAGE);
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

    private void onPresetDropped(String presetBlockId, LocalDateTime when) {
        try {
            String start = when.format(TS_FMT_LOCAL);
            presetService.placeOnCalendar(presetBlockId, start);
        } catch (CalendarService.ScheduleConflictException ex) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            "시간 충돌", JOptionPane.WARNING_MESSAGE));
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(view, ex.getMessage(),
                            "배치 실패", JOptionPane.WARNING_MESSAGE));
        }
    }

    private void reloadAutomations() {
        List<CalendarAutomationRule> rules = automationService.getAllRules();
        view.setAutomationRules(rules);
    }

    private void onAutomationAdd() {
        automationService.createRule("캘린더 자동화");
        reloadAutomations();
    }

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
