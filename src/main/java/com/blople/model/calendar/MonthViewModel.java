package com.blople.model.calendar;

import com.blople.model.block.ScheduleBlock;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 월간 뷰 모델.
 */
public class MonthViewModel {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TS_FMT   = ScheduleBlock.TS_FORMATTER;

    private int year;
    private int month;
    private List<ScheduleBlock> monthBlocks = new ArrayList<>();

    public MonthViewModel(int year, int month) {
        this.year = year;
        this.month = month;
    }

    public int getYear()  { return year; }
    public int getMonth() { return month; }

    public List<ScheduleBlock> getMonthBlocks() { return monthBlocks; }

    public void setMonthBlocks(List<ScheduleBlock> blocks) {
        this.monthBlocks = (blocks != null) ? blocks : new ArrayList<>();
    }

    public int getDaysInMonth() {
        return YearMonth.of(year, month).lengthOfMonth();
    }

    /** 해당 월 1일의 요일 (0=일 … 6=토). 달력 좌측 빈 칸 계산용. */
    public int getFirstDayOfWeek() {
        DayOfWeek dow = LocalDate.of(year, month, 1).getDayOfWeek();
        // java.time: MON=1 … SUN=7. 본 프로젝트 규약: SUN=0 … SAT=6
        return dow == DayOfWeek.SUNDAY ? 0 : dow.getValue();
    }

    public List<ScheduleBlock> getBlocksForDate(String dateText) {
        LocalDate target = LocalDate.parse(dateText, DATE_FMT);
        List<ScheduleBlock> result = new ArrayList<>();
        for (ScheduleBlock b : monthBlocks) {
            LocalDateTime s = LocalDateTime.parse(b.getStartDatetime(), TS_FMT);
            LocalDateTime e = LocalDateTime.parse(b.getEndDatetime(),   TS_FMT);
            // 해당 날짜와 겹치면 포함
            if (!target.isAfter(e.toLocalDate()) && !target.isBefore(s.toLocalDate())) {
                result.add(b);
            }
        }
        return result;
    }

    public void navigateNext() {
        if (month == 12) { year++; month = 1;  }
        else             { month++; }
    }

    public void navigatePrev() {
        if (month == 1) { year--; month = 12; }
        else            { month--; }
    }
}
