package com.blople.model.subject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 학기 모델. semester 테이블과 매핑.
 *
 * <p>날짜 필드는 "yyyy-MM-dd" 문자열로 저장한다.
 */
public class Semester {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TS_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String id;
    private String name;
    private String startDate;   // yyyy-MM-dd
    private String endDate;
    private boolean active;
    private String createdAt;

    public Semester(String name, LocalDate startDate, LocalDate endDate) {
        this.id        = UUID.randomUUID().toString();
        this.name      = name;
        this.startDate = startDate != null ? startDate.format(DATE_FMT) : null;
        this.endDate   = endDate   != null ? endDate.format(DATE_FMT)   : null;
        this.active    = false;
        this.createdAt = LocalDateTime.now().format(TS_FMT);
    }

    /** DAO 복원용 — Mapper 가 모든 영속 상태를 그대로 주입. */
    public static Semester reconstruct(String id, String name,
                                       String startDate, String endDate,
                                       boolean active, String createdAt) {
        Semester s = new Semester(name, null, null);
        s.id        = id;
        s.startDate = startDate;
        s.endDate   = endDate;
        s.active    = active;
        s.createdAt = createdAt;
        return s;
    }

    // ===== getter / setter =====
    public String getId()                       { return id; }
    public String getName()                     { return name; }
    public void   setName(String name)          { this.name = name; }
    public String getStartDate()                { return startDate; }
    public void   setStartDate(String d)        { this.startDate = d; }
    public String getEndDate()                  { return endDate; }
    public void   setEndDate(String d)          { this.endDate = d; }
    public boolean isActive()                   { return active; }
    public void   setActive(boolean active)     { this.active = active; }
    public String getCreatedAt()                { return createdAt; }

    public LocalDate getStartDateAsLocalDate() {
        return startDate != null ? LocalDate.parse(startDate, DATE_FMT) : null;
    }
    public LocalDate getEndDateAsLocalDate() {
        return endDate != null ? LocalDate.parse(endDate, DATE_FMT) : null;
    }

    @Override
    public String toString() {
        return "Semester{name=" + name + ", " + startDate + "~" + endDate + (active ? ", ACTIVE" : "") + "}";
    }
}
