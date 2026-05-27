package com.blople.model.calendar;

import com.blople.common.Constants;
import com.blople.model.block.ScheduleBlock;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * 주간 뷰 모델.
 *
 * <p>"기준 날짜" 가 속한 주의 일~토 범위와 8:00~23:00 의 30분 단위 슬롯을 보관한다.
 */
public class WeekViewModel {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TS_FMT   = ScheduleBlock.TS_FORMATTER;

    private LocalDate weekStartDate; // 일요일
    private LocalDate weekEndDate;   // 토요일
    private final List<TimeSlot> timeSlots = new ArrayList<>();
    private List<ScheduleBlock> weekBlocks = new ArrayList<>();

    public WeekViewModel(LocalDate baseDate) {
        setBaseDate(baseDate);
        buildTimeSlots();
    }

    /** 기준 날짜가 포함된 주(일~토)로 범위를 갱신. */
    public void setBaseDate(LocalDate baseDate) {
        this.weekStartDate = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        this.weekEndDate   = weekStartDate.plusDays(6);
    }

    private void buildTimeSlots() {
        timeSlots.clear();
        for (int day = 0; day < 7; day++) {
            for (int hour = Constants.CALENDAR_START_HOUR; hour < Constants.CALENDAR_END_HOUR; hour++) {
                timeSlots.add(new TimeSlot(day,
                        String.format("%02d:00", hour),
                        String.format("%02d:30", hour)));
                timeSlots.add(new TimeSlot(day,
                        String.format("%02d:30", hour),
                        String.format("%02d:00", hour + 1)));
            }
        }
    }

    // ===== 조회 =====
    public LocalDate getWeekStartDate()       { return weekStartDate; }
    public LocalDate getWeekEndDate()         { return weekEndDate;   }
    public List<TimeSlot> getTimeSlots()      { return timeSlots; }
    public List<ScheduleBlock> getWeekBlocks(){ return weekBlocks; }

    public void setWeekBlocks(List<ScheduleBlock> blocks) {
        this.weekBlocks = (blocks != null) ? blocks : new ArrayList<>();
    }

    /** 특정 요일·시간에 걸치는 블록만 추리기. */
    public List<ScheduleBlock> getBlocksForDayAndTime(int dayOfWeek, String hhmm) {
        List<ScheduleBlock> result = new ArrayList<>();
        LocalDate targetDate = weekStartDate.plusDays(dayOfWeek);
        LocalTime targetTime = LocalTime.parse(hhmm, TimeSlot.TIME_FMT);
        LocalDateTime targetDt = LocalDateTime.of(targetDate, targetTime);

        for (ScheduleBlock b : weekBlocks) {
            LocalDateTime s = LocalDateTime.parse(b.getStartDatetime(), TS_FMT);
            LocalDateTime e = LocalDateTime.parse(b.getEndDatetime(),   TS_FMT);
            if (!targetDt.isBefore(s) && targetDt.isBefore(e)) {
                result.add(b);
            }
        }
        return result;
    }

    /** 다음 주로 이동. */
    public void navigateNext() { setBaseDate(weekStartDate.plusDays(7)); }
    /** 이전 주로 이동. */
    public void navigatePrev() { setBaseDate(weekStartDate.minusDays(7)); }

    public String getWeekStartDateText() { return weekStartDate.format(DATE_FMT); }
    public String getWeekEndDateText()   { return weekEndDate.format(DATE_FMT);   }
}
