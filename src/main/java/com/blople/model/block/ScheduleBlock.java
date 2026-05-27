package com.blople.model.block;

import com.blople.model.enums.BlockType;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 일정 블록 (FR-01). 캘린더 위에 배치되는 단일 일정.
 */
public class ScheduleBlock extends Block {

    private String startDatetime;   // "yyyy-MM-dd HH:mm:ss"
    private String endDatetime;
    private String subjectId;       // nullable — 일반 일정인 경우
    private boolean recurring;
    private String recurrenceRule;  // RRULE 문자열 (선택)

    public ScheduleBlock(String name, String color,
                         String startDatetime, String endDatetime) {
        super(BlockType.SCHEDULE, name, color);
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.recurring = false;
    }

    public String getStartDatetime()                       { return startDatetime; }
    public void setStartDatetime(String startDatetime)     { this.startDatetime = startDatetime; touch(); }
    public String getEndDatetime()                         { return endDatetime; }
    public void setEndDatetime(String endDatetime)         { this.endDatetime = endDatetime; touch(); }
    public String getSubjectId()                           { return subjectId; }
    public void setSubjectId(String subjectId)             { this.subjectId = subjectId; touch(); }
    public boolean isRecurring()                           { return recurring; }
    public void setRecurring(boolean recurring)            { this.recurring = recurring; touch(); }
    public String getRecurrenceRule()                      { return recurrenceRule; }
    public void setRecurrenceRule(String rule)             { this.recurrenceRule = rule; touch(); }

    public int getDurationMinutes() {
        LocalDateTime s = LocalDateTime.parse(startDatetime, TS_FORMATTER);
        LocalDateTime e = LocalDateTime.parse(endDatetime, TS_FORMATTER);
        return (int) Duration.between(s, e).toMinutes();
    }

    /** 동일 시간대 충돌 여부 검사. */
    public boolean hasConflictWith(ScheduleBlock other) {
        if (other == null || other.getId().equals(getId())) return false;
        LocalDateTime aS = LocalDateTime.parse(this.startDatetime, TS_FORMATTER);
        LocalDateTime aE = LocalDateTime.parse(this.endDatetime,   TS_FORMATTER);
        LocalDateTime bS = LocalDateTime.parse(other.startDatetime, TS_FORMATTER);
        LocalDateTime bE = LocalDateTime.parse(other.endDatetime,   TS_FORMATTER);
        return aS.isBefore(bE) && bS.isBefore(aE);
    }
}
