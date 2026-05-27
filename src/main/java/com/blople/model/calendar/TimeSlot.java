package com.blople.model.calendar;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 캘린더 주간 뷰의 한 셀(요일 × 시간).
 * 30분 단위 슬롯이 기본이다.
 */
public class TimeSlot {

    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final int dayOfWeek;     // 0=일 … 6=토 (DB 와 동일)
    private final String startTime;  // "HH:mm"
    private final String endTime;    // "HH:mm"
    private boolean occupied;

    public TimeSlot(int dayOfWeek, String startTime, String endTime) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime   = endTime;
        this.occupied  = false;
    }

    public int    getDayOfWeek()  { return dayOfWeek; }
    public String getStartTime()  { return startTime; }
    public String getEndTime()    { return endTime; }
    public boolean isOccupied()   { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }

    public LocalTime startAsLocalTime() { return LocalTime.parse(startTime, TIME_FMT); }
    public LocalTime endAsLocalTime()   { return LocalTime.parse(endTime,   TIME_FMT); }

    @Override
    public String toString() {
        return "TimeSlot{day=" + dayOfWeek + ", " + startTime + "~" + endTime + "}";
    }
}
