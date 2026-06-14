package com.blople.service;

import com.blople.common.Constants;
import com.blople.common.EventBus;
import com.blople.database.dao.AssignmentDAO;
import com.blople.database.dao.ScheduleBlockDAO;
import com.blople.model.block.AssignmentBlock;
import com.blople.model.block.ScheduleBlock;
import com.blople.model.enums.AssignmentStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 과제 관리 서비스 (FR-04, FR-10).
 *
 * <p>v0.8 변경:
 * <ul>
 *   <li>마감 표시(ScheduleBlock)의 길이를 30분 → 1분으로 단축.
 *       23:59 마감 시 다음 날까지 블록이 걸쳐 표시되던 버그를 해결한다.</li>
 *   <li>일정 블록의 memo 에 {@code [ASSIGNMENT_DEADLINE:과제blockId]} 표식이 있으면
 *       "과제 마감 표시"로 식별 가능 → MonthViewPanel 의 과목 일정 필터링에 사용한다.</li>
 * </ul>
 */
public class AssignmentService {

    public static final DateTimeFormatter TS_FMT = ScheduleBlock.TS_FORMATTER;
    public  static final String DEADLINE_TAG_PREFIX = "[ASSIGNMENT_DEADLINE:";
    public  static final String DEADLINE_TAG_SUFFIX = "]";

    /** 캘린더 위 "마감 표시" 블록의 길이(분). 너무 길면 다음 날로 넘쳐 표시되므로 1분으로 짧게 둔다. */
    private static final int DEADLINE_MARK_MINUTES = 1;

    private final AssignmentDAO assignmentDAO;
    private final ScheduleBlockDAO scheduleBlockDAO;
    private final EventBus eventBus = EventBus.getInstance();

    public AssignmentService(AssignmentDAO assignmentDAO, ScheduleBlockDAO scheduleBlockDAO) {
        this.assignmentDAO = assignmentDAO;
        this.scheduleBlockDAO = scheduleBlockDAO;
    }

    /** memo 가 과제 마감 표시 태그를 가지고 있는지. */
    public static boolean isDeadlineMark(ScheduleBlock b) {
        if (b == null) return false;
        String memo = b.getMemo();
        return memo != null && memo.startsWith(DEADLINE_TAG_PREFIX) && memo.endsWith(DEADLINE_TAG_SUFFIX);
    }

    // ===== 조회 =====
    public List<AssignmentBlock> getBySubject(String subjectId) {
        return assignmentDAO.findBySubject(subjectId);
    }

    public Optional<AssignmentBlock> findById(String blockId) {
        return assignmentDAO.findById(blockId);
    }

    // ===== CRUD =====
    public AssignmentBlock createAssignment(String subjectId, String name, String color,
                                            String deadline, String description) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("과제명을 입력해주세요.");
        if (deadline == null || deadline.isBlank())
            throw new IllegalArgumentException("마감일을 설정해주세요.");
        LocalDateTime.parse(deadline, TS_FMT);

        AssignmentBlock a = new AssignmentBlock(subjectId, name, color, deadline);
        a.setDescription(description);
        assignmentDAO.insert(a);

        syncDeadlineToCalendar(a);
        eventBus.publish(EventBus.ASSIGNMENT_CREATED, a);
        return a;
    }

    public void updateStatus(String blockId, AssignmentStatus status) {
        AssignmentBlock a = assignmentDAO.findById(blockId).orElseThrow(
                () -> new IllegalArgumentException("과제를 찾을 수 없음: " + blockId));
        a.setStatus(status);
        assignmentDAO.update(a);
        eventBus.publish(EventBus.ASSIGNMENT_UPDATED, a);
    }

    public void updateDeadline(String blockId, String newDeadline) {
        AssignmentBlock a = assignmentDAO.findById(blockId).orElseThrow(
                () -> new IllegalArgumentException("과제를 찾을 수 없음: " + blockId));
        LocalDateTime.parse(newDeadline, TS_FMT);
        a.setDeadline(newDeadline);
        assignmentDAO.update(a);

        removeDeadlineMark(blockId);
        syncDeadlineToCalendar(a);
        eventBus.publish(EventBus.ASSIGNMENT_UPDATED, a);
    }

    public void deleteAssignment(String blockId) {
        removeDeadlineMark(blockId);
        boolean ok = assignmentDAO.deleteById(blockId);
        if (ok) eventBus.publish(EventBus.ASSIGNMENT_DELETED, blockId);
    }

    // ===== 마감일 ↔ 캘린더 연동 (SYS-01) =====
    private void syncDeadlineToCalendar(AssignmentBlock a) {
        LocalDateTime dl = LocalDateTime.parse(a.getDeadline(), TS_FMT);
        LocalDateTime endDl = dl.plusMinutes(DEADLINE_MARK_MINUTES);

        // 안전망: 종료가 다음 날로 넘어가면, 같은 날의 23:59 로 클램프.
        if (!endDl.toLocalDate().equals(dl.toLocalDate())) {
            endDl = dl.toLocalDate().atTime(23, 59);
            if (!endDl.isAfter(dl)) {
                endDl = dl; // 시작과 같으면 0분 표시
            }
        }

        String start = dl.format(TS_FMT);
        String end   = endDl.format(TS_FMT);

        ScheduleBlock mark = new ScheduleBlock("📌 마감: " + a.getName(), a.getColor(), start, end);
        mark.setSubjectId(a.getSubjectId());
        mark.setMemo(DEADLINE_TAG_PREFIX + a.getId() + DEADLINE_TAG_SUFFIX);
        scheduleBlockDAO.insert(mark);
        eventBus.publish(EventBus.SCHEDULE_BLOCK_CREATED, mark);
    }

    /** 해당 과제와 연결된 마감 표시 일정 블록을 찾아 삭제. */
    private void removeDeadlineMark(String assignmentBlockId) {
        String tag = DEADLINE_TAG_PREFIX + assignmentBlockId + DEADLINE_TAG_SUFFIX;
        for (ScheduleBlock b : scheduleBlockDAO.findAll()) {
            if (tag.equals(b.getMemo())) {
                scheduleBlockDAO.deleteById(b.getId());
                eventBus.publish(EventBus.SCHEDULE_BLOCK_DELETED, b.getId());
            }
        }
    }

    // ===== 마감 임박/지난 =====
    public List<AssignmentBlock> getApproachingDeadlines(String subjectId) {
        return getBySubject(subjectId).stream()
                .filter(a -> a.isApproachingDeadline(Constants.DEADLINE_APPROACHING_DAYS))
                .toList();
    }
}
