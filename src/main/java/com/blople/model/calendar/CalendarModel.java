package com.blople.model.calendar;

import com.blople.model.block.ScheduleBlock;
import com.blople.model.enums.ViewMode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 캘린더 전체 상태 모델 — 현재 뷰 모드, 기준 날짜, 메모리 상의 블록 캐시를 보관.
 *
 * <p>DB 와의 동기화는 {@link com.blople.service.CalendarService} 가 담당하며,
 * 본 모델은 View 가 즉시 참조 가능한 메모리 캐시 역할만 한다.
 */
public class CalendarModel {

    private ViewMode currentViewMode;
    private LocalDate currentDate;

    private final WeekViewModel  weekView;
    private final MonthViewModel monthView;

    /** 메모리 캐시 — 최근 조회 범위의 모든 일정 블록. */
    private List<ScheduleBlock> scheduleBlocks = new ArrayList<>();

    public CalendarModel() {
        this.currentDate     = LocalDate.now();
        this.currentViewMode = ViewMode.WEEK;
        this.weekView        = new WeekViewModel(currentDate);
        this.monthView       = new MonthViewModel(currentDate.getYear(), currentDate.getMonthValue());
    }

    // ===== getter / setter =====
    public ViewMode getCurrentViewMode()              { return currentViewMode; }
    public void setCurrentViewMode(ViewMode mode)     { this.currentViewMode = mode; }
    public LocalDate getCurrentDate()                 { return currentDate; }

    public void setCurrentDate(LocalDate date) {
        this.currentDate = date;
        weekView.setBaseDate(date);
        // 월간 뷰도 동기화
        if (date.getYear() != monthView.getYear() || date.getMonthValue() != monthView.getMonth()) {
            // 단순 재설정
            int y = date.getYear(), m = date.getMonthValue();
            while (monthView.getYear() != y || monthView.getMonth() != m) {
                if (monthView.getYear() < y ||
                        (monthView.getYear() == y && monthView.getMonth() < m)) {
                    monthView.navigateNext();
                } else {
                    monthView.navigatePrev();
                }
            }
        }
    }

    public WeekViewModel  getWeekView()  { return weekView; }
    public MonthViewModel getMonthView() { return monthView; }

    public List<ScheduleBlock> getScheduleBlocks() { return scheduleBlocks; }

    public void setScheduleBlocks(List<ScheduleBlock> blocks) {
        this.scheduleBlocks = (blocks != null) ? blocks : new ArrayList<>();
        // 하위 뷰모델도 동기화
        weekView.setWeekBlocks(this.scheduleBlocks);
        monthView.setMonthBlocks(this.scheduleBlocks);
    }

    public void addBlock(ScheduleBlock block)   { scheduleBlocks.add(block); }
    public void removeBlock(String blockId) {
        scheduleBlocks.removeIf(b -> b.getId().equals(blockId));
    }
}
