package com.blople.controller;

import com.blople.common.EventBus;
import com.blople.model.block.ScheduleBlock;
import com.blople.model.calendar.CalendarModel;
import com.blople.model.enums.ViewMode;
import com.blople.service.CalendarService;
import com.blople.view.calendar.CalendarPagePanel;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 캘린더 페이지 컨트롤러 (FR-01).
 *
 * <p>책임:
 * <ul>
 *   <li>View(버튼/클릭) 이벤트 → Service 호출</li>
 *   <li>EventBus 구독 → 데이터 변경 시 View 자동 갱신</li>
 *   <li>CalendarModel 의 현재 상태를 View 표시(날짜 라벨 등)에 반영</li>
 * </ul>
 */
public class CalendarController {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("yyyy년 M월");

    private final CalendarService service;
    private final CalendarPagePanel view;
    private final CalendarModel model;
    private final EventBus eventBus = EventBus.getInstance();

    public CalendarController(CalendarService service, CalendarPagePanel view) {
        this.service = service;
        this.view = view;
        this.model = service.getCalendarModel();
    }

    /** MainFrame 조립 후 1회 호출 — 콜백 연결 + 초기 렌더 + 이벤트 구독. */
    public void initialize() {
        wireCallbacks();
        subscribeEvents();
        syncViewState();
        refreshView();
    }

    private void wireCallbacks() {
        view.setOnPrev(this::onNavigatePrev);
        view.setOnNext(this::onNavigateNext);
        view.setOnToday(this::onToday);
        view.setOnViewToggle(this::onViewToggle);
        view.setOnBlockClick(this::onBlockClick);
        view.setOnDateClick(this::onDateClick);
    }

    private void subscribeEvents() {
        // 데이터가 바뀌면 어느 경로로 바뀌든 View 를 다시 그린다.
        eventBus.subscribe(EventBus.SCHEDULE_BLOCK_CREATED, p -> refreshOnEdt());
        eventBus.subscribe(EventBus.SCHEDULE_BLOCK_UPDATED, p -> refreshOnEdt());
        eventBus.subscribe(EventBus.SCHEDULE_BLOCK_MOVED,   p -> refreshOnEdt());
        eventBus.subscribe(EventBus.SCHEDULE_BLOCK_DELETED, p -> refreshOnEdt());
    }

    // ===== View 이벤트 핸들러 =====
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
        // v0.2 에서는 간단한 정보 표시. 편집 다이얼로그는 후속 단계.
        String msg = "일정: %s\n%s ~ %s".formatted(
                block.getName(), block.getStartDatetime(), block.getEndDatetime());
        JOptionPane.showMessageDialog(view, msg, "일정 상세", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onDateClick(LocalDate date) {
        // 월간 뷰에서 날짜 클릭 → 그 주의 주간 뷰로 전환
        service.switchViewMode(ViewMode.WEEK);
        service.goToDate(date);
        syncViewState();
        refreshView();
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
