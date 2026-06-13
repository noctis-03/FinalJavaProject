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

public class CalendarService {

    public static final DateTimeFormatter TS_FMT   = ScheduleBlock.TS_FORMATTER;
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 충돌 검사를 잠시 끄고 일정을 일괄 추가해야 할 때 사용 (예: 시간표 → 반복 일정 자동 생성). */
    private boolean conflictCheckEnabled = true;

    private final CalendarModel    calendarModel;
    private final ScheduleBlockDAO scheduleBlockDAO;
    private final EventBus         eventBus = EventBus.getInstance();

    public CalendarService(CalendarModel calendarModel, ScheduleBlockDAO scheduleBlockDAO) {
        this.calendarModel    = calendarModel;
        this.scheduleBlockDAO = scheduleBlockDAO;
        reloadCacheForCurrentView();
    }

    public void setConflictCheckEnabled(boolean enabled) { this.conflictCheckEnabled = enabled; }
    public boolean isConflictCheckEnabled()              { return conflictCheckEnabled; }

    public void switchViewMode(ViewMode mode) {
        calendarModel.setCurrentViewMode(mode);
        reloadCacheForCurrentView();
        eventBus.publish(EventBus.CALENDAR_VIEW_CHANGED, mode);
    }

    public void navigateNext() {
        if (calendarModel.getCurrentViewMode() == ViewMode.WEEK) {
            calendarModel.setCurrentDate(calendarModel.getCurrentDate().plusDays(7));
        } else {
            calendarModel.setCurrentDate(calendarModel.getCurrentDate().plusMonths(1));
        }
        reloadCacheForCurrentView();
        eventBus.publish(EventBus.CALENDAR_DATE_CHANGED, calendarModel.getCurrentDate());
    }

    public void navigatePrev() {
        if (calendarModel.getCurrentViewMode() == ViewMode.WEEK) {
            calendarModel.setCurrentDate(calendarModel.getCurrentDate().minusDays(7));
        } else {
            calendarModel.setCurrentDate(calendarModel.getCurrentDate().minusMonths(1));
        }
        reloadCacheForCurrentView();
        eventBus.publish(EventBus.CALENDAR_DATE_CHANGED, calendarModel.getCurrentDate());
    }

    public void goToDate(LocalDate date) {
        calendarModel.setCurrentDate(date);
        reloadCacheForCurrentView();
        eventBus.publish(EventBus.CALENDAR_DATE_CHANGED, date);
    }

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

    public void addScheduleBlock(ScheduleBlock block) {
        validate(block);
        ensureNoConflict(block);
        scheduleBlockDAO.insert(block);
        calendarModel.addBlock(block);
        eventBus.publish(EventBus.SCHEDULE_BLOCK_CREATED, block);
    }

    public void moveBlock(String blockId, String newStartDatetime, String newEndDatetime) {
        ScheduleBlock target = scheduleBlockDAO.findById(blockId).orElseThrow(
                () -> new IllegalArgumentException("일정 블록을 찾을 수 없음: " + blockId));
        target.setStartDatetime(newStartDatetime);
        target.setEndDatetime(newEndDatetime);
        validate(target);
        ensureNoConflict(target);
        scheduleBlockDAO.update(target);
        reloadCacheForCurrentView();
        eventBus.publish(EventBus.SCHEDULE_BLOCK_MOVED, target);
    }

    public void resizeBlock(String blockId, String newEndDatetime) {
        ScheduleBlock target = scheduleBlockDAO.findById(blockId).orElseThrow(
                () -> new IllegalArgumentException("일정 블록을 찾을 수 없음: " + blockId));
        target.setEndDatetime(newEndDatetime);
        validate(target);
        ensureNoConflict(target);
        scheduleBlockDAO.update(target);
        reloadCacheForCurrentView();
        eventBus.publish(EventBus.SCHEDULE_BLOCK_UPDATED, target);
    }

    public void updateBlock(ScheduleBlock updated) {
        validate(updated);
        ensureNoConflict(updated);
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

    /** 충돌이 있으면 IllegalStateException 으로 거부 (요구 #8). */
    private void ensureNoConflict(ScheduleBlock target) {
        if (!conflictCheckEnabled) return;
        List<ScheduleBlock> conflicts = detectConflicts(target);
        if (!conflicts.isEmpty()) {
            String names = conflicts.stream()
                    .map(ScheduleBlock::getName)
                    .limit(3)
                    .reduce((a, b) -> a + ", " + b).orElse("");
            throw new ScheduleConflictException(
                    "이미 같은 시간대에 일정이 있습니다: " + names
                            + (conflicts.size() > 3 ? " 외 " + (conflicts.size() - 3) + "건" : ""),
                    conflicts);
        }
    }

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

    /** 충돌 발생 시 던지는 예외. */
    public static class ScheduleConflictException extends RuntimeException {
        private final List<ScheduleBlock> conflicts;
        public ScheduleConflictException(String message, List<ScheduleBlock> conflicts) {
            super(message);
            this.conflicts = conflicts;
        }
        public List<ScheduleBlock> getConflicts() { return conflicts; }
    }
}
