package com.blople.service;

import com.blople.common.EventBus;
import com.blople.database.dao.ScheduleBlockDAO;
import com.blople.model.block.ScheduleBlock;
import com.blople.model.calendar.CalendarModel;
import com.blople.model.calendar.WeekViewModel;
import com.blople.model.enums.ViewMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 캘린더 비즈니스 로직 서비스 (FR-01 핵심).
 *
 * <p>책임:
 * <ul>
 *   <li>{@link CalendarModel} 의 상태 관리 (뷰 모드 전환, 날짜 이동)</li>
 *   <li>{@link ScheduleBlockDAO} 를 통한 일정 블록 CRUD</li>
 *   <li>일정 충돌 감지</li>
 *   <li>변경 시 {@link EventBus} 에 이벤트 발행 → View 자동 갱신</li>
 * </ul>
 *
 * <p>View 갱신은 EventBus 를 통한 단방향 알림으로만 일어난다.
 */
public class CalendarService {

    public static final DateTimeFormatter TS_FMT   = ScheduleBlock.TS_FORMATTER;
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CalendarModel    calendarModel;
    private final ScheduleBlockDAO scheduleBlockDAO;
    private final EventBus         eventBus = EventBus.getInstance();

    public CalendarService(CalendarModel calendarModel, ScheduleBlockDAO scheduleBlockDAO) {
        this.calendarModel    = calendarModel;
        this.scheduleBlockDAO = scheduleBlockDAO;
        reloadCacheForCurrentView();
    }

    // ============================================================
    //  뷰 / 네비게이션
    // ============================================================
    public void switchViewMode(ViewMode mode) {
        calendarModel.setCurrentViewMode(mode);
        reloadCacheForCurrentView();
        eventBus.publish(EventBus.CALENDAR_VIEW_CHANGED, mode);
    }

    public void navigateNext() {
        if (calendarModel.getCurrentViewMode() == ViewMode.WEEK) {
            calendarModel.setCurrentDate(calendarModel.getCurrentDate().plusDays(7));
        } else {
            LocalDate cur = calendarModel.getCurrentDate();
            calendarModel.setCurrentDate(cur.plusMonths(1));
        }
        reloadCacheForCurrentView();
        eventBus.publish(EventBus.CALENDAR_DATE_CHANGED, calendarModel.getCurrentDate());
    }

    public void navigatePrev() {
        if (calendarModel.getCurrentViewMode() == ViewMode.WEEK) {
            calendarModel.setCurrentDate(calendarModel.getCurrentDate().minusDays(7));
        } else {
            LocalDate cur = calendarModel.getCurrentDate();
            calendarModel.setCurrentDate(cur.minusMonths(1));
        }
        reloadCacheForCurrentView();
        eventBus.publish(EventBus.CALENDAR_DATE_CHANGED, calendarModel.getCurrentDate());
    }

    public void goToDate(LocalDate date) {
        calendarModel.setCurrentDate(date);
        reloadCacheForCurrentView();
        eventBus.publish(EventBus.CALENDAR_DATE_CHANGED, date);
    }

    // ============================================================
    //  조회
    // ============================================================
    public List<ScheduleBlock> getBlocksForDate(LocalDate date) {
        String from = date.atStartOfDay().format(TS_FMT);
        String to   = date.plusDays(1).atStartOfDay().format(TS_FMT);
        return scheduleBlockDAO.findInRange(from, to);
    }

    public List<ScheduleBlock> getBlocksForWeek(LocalDate weekStartSunday) {
        String from = weekStartSunday.atStartOfDay().format(TS_FMT);
        String to   = weekStartSunday.plusDays(7).atStartOfDay().format(TS_FMT);
        return scheduleBlockDAO.findInRange(from, to);
    }

    public List<ScheduleBlock> getBlocksForMonth(int year, int month) {
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate next  = first.plusMonths(1);
        return scheduleBlockDAO.findInRange(
                first.atStartOfDay().format(TS_FMT),
                next.atStartOfDay().format(TS_FMT));
    }

    public Optional<ScheduleBlock> findById(String blockId) {
        return scheduleBlockDAO.findById(blockId);
    }

    // ============================================================
    //  CRUD
    // ============================================================
    public void addScheduleBlock(ScheduleBlock block) {
        validate(block);
        scheduleBlockDAO.insert(block);
        calendarModel.addBlock(block);
        eventBus.publish(EventBus.SCHEDULE_BLOCK_CREATED, block);
        // TODO: AutoSaveManager.markDirty()  — AutoSaveManager 구현 후 연결
    }

    public void moveBlock(String blockId, String newStartDatetime, String newEndDatetime) {
        ScheduleBlock target = scheduleBlockDAO.findById(blockId).orElseThrow(
                () -> new IllegalArgumentException("일정 블록을 찾을 수 없음: " + blockId));
        target.setStartDatetime(newStartDatetime);
        target.setEndDatetime(newEndDatetime);
        validate(target);
        scheduleBlockDAO.update(target);
        reloadCacheForCurrentView();
        eventBus.publish(EventBus.SCHEDULE_BLOCK_MOVED, target);
    }

    public void resizeBlock(String blockId, String newEndDatetime) {
        ScheduleBlock target = scheduleBlockDAO.findById(blockId).orElseThrow(
                () -> new IllegalArgumentException("일정 블록을 찾을 수 없음: " + blockId));
        target.setEndDatetime(newEndDatetime);
        validate(target);
        scheduleBlockDAO.update(target);
        reloadCacheForCurrentView();
        eventBus.publish(EventBus.SCHEDULE_BLOCK_UPDATED, target);
    }

    public void updateBlock(ScheduleBlock updated) {
        validate(updated);
        scheduleBlockDAO.update(updated);
        reloadCacheForCurrentView();
        eventBus.publish(EventBus.SCHEDULE_BLOCK_UPDATED, updated);
    }

    public void removeBlock(String blockId) {
        boolean ok = scheduleBlockDAO.deleteById(blockId);
        if (ok) {
            calendarModel.removeBlock(blockId);
            eventBus.publish(EventBus.SCHEDULE_BLOCK_DELETED, blockId);
        }
    }

    // ============================================================
    //  충돌 감지
    // ============================================================

    /**
     * 주어진 블록과 같은 시간대에 겹치는 다른 블록 목록을 반환.
     * (자기 자신은 제외)
     */
    public List<ScheduleBlock> detectConflicts(ScheduleBlock target) {
        LocalDate date = LocalDateTime.parse(target.getStartDatetime(), TS_FMT).toLocalDate();
        List<ScheduleBlock> sameDay = getBlocksForDate(date);
        List<ScheduleBlock> conflicts = new ArrayList<>();
        for (ScheduleBlock b : sameDay) {
            if (!b.getId().equals(target.getId()) && target.hasConflictWith(b)) {
                conflicts.add(b);
            }
        }
        return conflicts;
    }

    // ============================================================
    //  내부 헬퍼
    // ============================================================

    private void validate(ScheduleBlock block) {
        if (block.getName() == null || block.getName().isBlank()) {
            throw new IllegalArgumentException("일정명을 입력해주세요.");
        }
        LocalDateTime s = LocalDateTime.parse(block.getStartDatetime(), TS_FMT);
        LocalDateTime e = LocalDateTime.parse(block.getEndDatetime(),   TS_FMT);
        if (!s.isBefore(e)) {
            throw new IllegalArgumentException("시작 시간이 종료 시간보다 늦거나 같습니다.");
        }
    }

    /** 현재 뷰모드/기준 날짜 범위의 블록을 다시 읽어 모델 캐시를 갱신. */
    public void reloadCacheForCurrentView() {
        List<ScheduleBlock> blocks;
        if (calendarModel.getCurrentViewMode() == ViewMode.WEEK) {
            WeekViewModel wv = calendarModel.getWeekView();
            blocks = getBlocksForWeek(wv.getWeekStartDate());
        } else {
            YearMonth ym = YearMonth.from(calendarModel.getCurrentDate());
            blocks = getBlocksForMonth(ym.getYear(), ym.getMonthValue());
        }
        calendarModel.setScheduleBlocks(blocks);
    }

    public CalendarModel getCalendarModel() {
        return calendarModel;
    }
}
