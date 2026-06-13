package com.blople.model.automation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

/**
 * 캘린더 페이지 자동화 규칙 (FR-12).
 *
 * <p>"[이번 주/다음 주]의 [요일]요일 [시]시에 [미리블록] 일정을 추가한다"
 * 를 표현하는 경량 모델. command/slot 중간 단계는 DAO 가 만들어 끼우며,
 * 본 모델은 핵심 값(주 오프셋·요일·시각·미리블록)만 직접 들고 있는다.
 *
 * <p>context 는 항상 CALENDAR, command_type 은 REGISTER_SCHEDULE 로 고정.
 */
public class CalendarAutomationRule {

    public static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String id;
    private String name;
    private boolean active;
    private String createdAt;

    // 핵심 슬롯 값
    private int weekOffset;          // 0 = 이번 주, 1 = 다음 주
    private int dayOfWeek;           // 0=일 … 6=토 (미설정 = -1)
    private int startHour;           // 시작 시각(시), 기본 9
    private String presetBlockId;    // 등록할 미리블록 id (없으면 미완성)

    /** 신규 생성 — 빈 규칙(슬롯 미설정). */
    public CalendarAutomationRule(String name) {
        this.id         = UUID.randomUUID().toString();
        this.name       = (name == null || name.isBlank()) ? "캘린더 자동화" : name;
        this.active     = true;
        this.createdAt  = LocalDateTime.now().format(TS_FMT);
        this.weekOffset = 1;   // 기본 다음 주
        this.dayOfWeek  = -1;  // 미설정
        this.startHour  = 9;   // 기본 9시
        this.presetBlockId = null;
    }

    /** DAO 복원용. */
    public static CalendarAutomationRule reconstruct(String id, String name,
                                                     boolean active, String createdAt,
                                                     int startHour) {
        CalendarAutomationRule r = new CalendarAutomationRule(name);
        r.id        = id;
        r.active    = active;
        r.createdAt = createdAt;
        r.startHour = startHour;
        return r;
    }

    public String getId()                       { return id; }
    public String getName()                      { return name; }
    public void   setName(String name)           { this.name = name; }
    public boolean isActive()                    { return active; }
    public void   setActive(boolean active)      { this.active = active; }
    public String getCreatedAt()                 { return createdAt; }

    public int  getWeekOffset()                  { return weekOffset; }
    /** 0=이번 주, 1=다음 주. 음수는 0으로 보정. */
    public void setWeekOffset(int weekOffset)    { this.weekOffset = Math.max(0, weekOffset); }
    public int  getDayOfWeek()                   { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek)      { this.dayOfWeek = dayOfWeek; }
    public int  getStartHour()                   { return startHour; }
    public void setStartHour(int startHour)      { this.startHour = Math.max(0, Math.min(23, startHour)); }
    public String getPresetBlockId()             { return presetBlockId; }
    public void   setPresetBlockId(String id)    { this.presetBlockId = id; }

    /** 요일·미리블록이 모두 채워졌는지(실행 가능 여부). */
    public boolean isComplete() {
        return dayOfWeek >= 0 && dayOfWeek <= 6
                && presetBlockId != null && !presetBlockId.isBlank();
    }

    /**
     * 오늘 기준 "이번 주 일요일 + weekOffset 주 + dayOfWeek" 날짜의 startHour 시 시작 시각.
     * @return "yyyy-MM-dd HH:mm:ss". 미완성이면 null.
     */
    public String resolveStartDatetime() {
        if (!isComplete()) return null;
        LocalDate today = LocalDate.now();
        LocalDate thisSunday = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
        LocalDate target = thisSunday.plusWeeks(weekOffset).plusDays(dayOfWeek);
        return target.atTime(startHour, 0).format(TS_FMT);
    }

    public String weekLabel() { return weekOffset == 0 ? "이번 주" : "다음 주"; }

    public String dayName() {
        if (dayOfWeek < 0 || dayOfWeek > 6) return "( ? )";
        return new String[]{"일", "월", "화", "수", "목", "금", "토"}[dayOfWeek];
    }

    @Override
    public String toString() {
        return "CalendarAutomationRule{%s %s요일 %d시, preset=%s}"
                .formatted(weekLabel(), dayName(), startHour, presetBlockId);
    }
}
