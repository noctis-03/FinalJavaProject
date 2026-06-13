package com.blople.model.subject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Semester {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TS_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 1학기 / 2학기 구분. */
    public enum Term {
        FIRST(1, "1학기", 3, 6),   // 3월 ~ 6월 말
        SECOND(2, "2학기", 9, 12); // 9월 ~ 12월 말

        public final int number;
        public final String label;
        public final int startMonth;
        public final int endMonth;

        Term(int number, String label, int startMonth, int endMonth) {
            this.number = number;
            this.label = label;
            this.startMonth = startMonth;
            this.endMonth = endMonth;
        }

        public static Term ofNumber(int n) {
            return n == 2 ? SECOND : FIRST;
        }

        /** 이 학기가 today 를 포함하는 연도를 반환 (없으면 올해). */
        public LocalDate defaultStart(int year) {
            return LocalDate.of(year, startMonth, 1);
        }

        public LocalDate defaultEnd(int year) {
            // 해당 월의 마지막 날
            LocalDate first = LocalDate.of(year, endMonth, 1);
            return first.withDayOfMonth(first.lengthOfMonth());
        }
    }

    private String id;
    private String name;
    private String startDate;
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

    /** 학기 이름에서 Term 추정 (예: "2026-1학기" → FIRST). */
    public Term detectTerm() {
        if (name != null && name.contains("2학기")) return Term.SECOND;
        return Term.FIRST;
    }

    @Override
    public String toString() {
        return "Semester{name=" + name + ", " + startDate + "~" + endDate
                + (active ? ", ACTIVE" : "") + "}";
    }
}
