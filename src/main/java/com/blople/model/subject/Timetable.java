package com.blople.model.subject;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 시간표 항목 모델 (FR-09). timetable_entry 테이블과 매핑.
 *
 * <p>하나의 과목(subjectId)이 특정 요일·시간대에 배치되는 한 칸을 의미한다.
 *
 * <p>요일 규약: 0=일 … 6=토 (프로젝트 공통 규약, timetable_entry 기준).
 * 시간 형식: "HH:mm".
 *
 * <p>다른 도메인 모델(Semester, Subject)과 동일하게
 * 신규 생성은 public 생성자, DB 복원은 {@link #reconstruct} 정적 메서드를 쓴다.
 */
public class Timetable {

    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private String id;
    private String subjectId;
    private int    dayOfWeek;   // 0=일 … 6=토
    private String startTime;   // "HH:mm"
    private String endTime;     // "HH:mm"

    /** 신규 생성용 — id 자동 부여. */
    public Timetable(String subjectId, int dayOfWeek, String startTime, String endTime) {
        this.id        = UUID.randomUUID().toString();
        this.subjectId = subjectId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime   = endTime;
    }

    /** DAO 복원용 — Mapper/DAO 가 모든 영속 상태를 그대로 주입. */
    public static Timetable reconstruct(String id, String subjectId, int dayOfWeek,
                                        String startTime, String endTime) {
        Timetable t = new Timetable(subjectId, dayOfWeek, startTime, endTime);
        t.id = id;
        return t;
    }

    // ===== getter / setter =====
    public String getId()                       { return id; }
    public String getSubjectId()                { return subjectId; }
    public void   setSubjectId(String id)       { this.subjectId = id; }
    public int    getDayOfWeek()                { return dayOfWeek; }
    public void   setDayOfWeek(int day)         { this.dayOfWeek = day; }
    public String getStartTime()                { return startTime; }
    public void   setStartTime(String t)        { this.startTime = t; }
    public String getEndTime()                  { return endTime; }
    public void   setEndTime(String t)          { this.endTime = t; }

    public LocalTime startAsLocalTime() { return LocalTime.parse(startTime, TIME_FMT); }
    public LocalTime endAsLocalTime()   { return LocalTime.parse(endTime,   TIME_FMT); }

    /**
     * 같은 요일에 시간대가 겹치는지 검사.
     * 요일이 다르면 충돌 없음. 자기 자신(같은 id)과는 충돌하지 않는다.
     */
    public boolean hasConflictWith(Timetable other) {
        if (other == null || other.id.equals(this.id)) return false;
        if (other.dayOfWeek != this.dayOfWeek) return false;

        LocalTime aS = this.startAsLocalTime();
        LocalTime aE = this.endAsLocalTime();
        LocalTime bS = other.startAsLocalTime();
        LocalTime bE = other.endAsLocalTime();

        // [aS, aE) 와 [bS, bE) 가 겹치면 충돌
        return aS.isBefore(bE) && bS.isBefore(aE);
    }

    @Override
    public String toString() {
        return "Timetable{day=%d, %s~%s, subjectId=%s}".formatted(
                dayOfWeek, startTime, endTime, subjectId);
    }
}
