package com.blople.service;

import com.blople.common.EventBus;
import com.blople.database.dao.ScheduleBlockDAO;
import com.blople.database.dao.TimetableDAO;
import com.blople.model.block.ScheduleBlock;
import com.blople.model.subject.Semester;
import com.blople.model.subject.Subject;
import com.blople.model.subject.Timetable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 시간표 서비스.
 *
 * <p>핵심 책임:
 * <ul>
 *   <li>timetable_entry CRUD</li>
 *   <li>시간표 항목이 추가/수정/삭제될 때 캘린더의 반복 일정 블록을
 *       해당 학기 전체 기간에 걸쳐 일괄 생성/재생성/삭제</li>
 *   <li>같은 요일·시간대 충돌 감지</li>
 * </ul>
 */
public class TimetableService {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TS_FMT   = ScheduleBlock.TS_FORMATTER;
    public static final DateTimeFormatter TIME_FMT = Timetable.TIME_FMT;

    private final TimetableDAO     timetableDAO;
    private final ScheduleBlockDAO scheduleBlockDAO;
    private final SubjectService   subjectService;
    private final EventBus         eventBus = EventBus.getInstance();

    public TimetableService(TimetableDAO timetableDAO,
                            ScheduleBlockDAO scheduleBlockDAO,
                            SubjectService subjectService) {
        this.timetableDAO     = timetableDAO;
        this.scheduleBlockDAO = scheduleBlockDAO;
        this.subjectService   = subjectService;
    }

    // ============================================================
    //  조회
    // ============================================================
    public List<Timetable> getBySubject(String subjectId) {
        return timetableDAO.findBySubject(subjectId);
    }

    public List<Timetable> getBySemester(String semesterId) {
        return timetableDAO.findBySemester(semesterId);
    }

    /** 같은 학기 내 모든 시간표 항목과 충돌하는지 검사. */
    public List<Timetable> detectConflicts(Timetable candidate, String semesterId) {
        List<Timetable> conflicts = new ArrayList<>();
        for (Timetable t : getBySemester(semesterId)) {
            if (candidate.hasConflictWith(t)) conflicts.add(t);
        }
        return conflicts;
    }

    // ============================================================
    //  CRUD
    // ============================================================
    public Timetable addEntry(String subjectId, int dayOfWeek,
                              String startTime, String endTime) {
        validateRange(startTime, endTime);

        Timetable t = new Timetable(subjectId, dayOfWeek, startTime, endTime);
        timetableDAO.insert(t);

        regenerateRecurringBlocksForSubject(subjectId);
        eventBus.publish(EventBus.SUBJECT_CHANGED, "timetable-changed");
        return t;
    }

    public void updateEntry(Timetable entry) {
        validateRange(entry.getStartTime(), entry.getEndTime());
        timetableDAO.update(entry);

        regenerateRecurringBlocksForSubject(entry.getSubjectId());
        eventBus.publish(EventBus.SUBJECT_CHANGED, "timetable-changed");
    }

    public void deleteEntry(String entryId) {
        Optional<Timetable> opt = timetableDAO.findById(entryId);
        if (opt.isEmpty()) return;
        String subjectId = opt.get().getSubjectId();

        timetableDAO.delete(entryId);
        regenerateRecurringBlocksForSubject(subjectId);
        eventBus.publish(EventBus.SUBJECT_CHANGED, "timetable-changed");
    }

    // ============================================================
    //  캘린더 반복 블록 생성 (핵심)
    // ============================================================

    /**
     * 한 과목의 모든 시간표 항목 → 캘린더 반복 일정 블록 재생성.
     *
     * <p>절차:
     * <ol>
     *   <li>해당 과목의 기존 schedule_block (subject_id 일치 + is_recurring = 1) 모두 삭제</li>
     *   <li>해당 학기 기간 동안 각 시간표 항목에 대해 주 1회씩 INSERT</li>
     * </ol>
     */
    public void regenerateRecurringBlocksForSubject(String subjectId) {
        // 1) 기존 반복 블록 삭제
        List<ScheduleBlock> existing = scheduleBlockDAO.findBySubject(subjectId);
        for (ScheduleBlock b : existing) {
            if (b.isRecurring()) {
                scheduleBlockDAO.deleteById(b.getId());
            }
        }

        // 2) 학기 기간 결정
        Optional<Subject> subjOpt = subjectService.getSubjectById(subjectId);
        if (subjOpt.isEmpty()) return;
        Subject subject = subjOpt.get();

        LocalDate semesterStart, semesterEnd;
        Optional<Semester> semOpt = subjectService.getActiveSemester();
        if (semOpt.isPresent() && semOpt.get().getId().equals(subject.getSemesterId())
                && semOpt.get().getStartDateAsLocalDate() != null
                && semOpt.get().getEndDateAsLocalDate() != null) {
            semesterStart = semOpt.get().getStartDateAsLocalDate();
            semesterEnd   = semOpt.get().getEndDateAsLocalDate();
        } else {
            // fallback: 오늘부터 16주
            semesterStart = LocalDate.now();
            semesterEnd   = semesterStart.plusWeeks(16);
        }

        // 3) 시간표 항목 각각에 대해 주 1회 블록 생성
        for (Timetable t : getBySubject(subjectId)) {
            generateWeeklyBlocks(subject, t, semesterStart, semesterEnd);
        }
    }

    private void generateWeeklyBlocks(Subject subject, Timetable entry,
                                      LocalDate semesterStart, LocalDate semesterEnd) {
        DayOfWeek targetDow = toJavaDow(entry.getDayOfWeek());
        LocalTime startT = LocalTime.parse(entry.getStartTime(), TIME_FMT);
        LocalTime endT   = LocalTime.parse(entry.getEndTime(),   TIME_FMT);

        LocalDate cursor = semesterStart.with(TemporalAdjusters.nextOrSame(targetDow));
        while (!cursor.isAfter(semesterEnd)) {
            LocalDateTime start = LocalDateTime.of(cursor, startT);
            LocalDateTime end   = LocalDateTime.of(cursor, endT);

            ScheduleBlock block = new ScheduleBlock(
                    subject.getName(),
                    subject.getColor(),
                    start.format(TS_FMT),
                    end.format(TS_FMT));
            block.setSubjectId(subject.getId());
            block.setRecurring(true);
            block.setRecurrenceRule("FREQ=WEEKLY");  // 단순 표기용
            scheduleBlockDAO.insert(block);

            cursor = cursor.plusWeeks(1);
        }
    }

    /** 0=일 … 6=토 → java.time.DayOfWeek (MON=1 … SUN=7) */
    private DayOfWeek toJavaDow(int projDow) {
        return projDow == 0 ? DayOfWeek.SUNDAY : DayOfWeek.of(projDow);
    }

    private void validateRange(String startTime, String endTime) {
        LocalTime s = LocalTime.parse(startTime, TIME_FMT);
        LocalTime e = LocalTime.parse(endTime,   TIME_FMT);
        if (!s.isBefore(e)) {
            throw new IllegalArgumentException("시작 시간이 종료 시간보다 늦거나 같습니다.");
        }
    }
}
