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
 * <p>책임:
 * <ul>
 *   <li>과제 블록 CRUD (block + assignment)</li>
 *   <li>마감일 → 캘린더 마감 표시 일정 블록 자동 생성/갱신/삭제 (SYS-01)</li>
 *   <li>마감 임박/지난 과제 조회</li>
 * </ul>
 *
 * <p>마감 표시는 deadline 시각에 시작하는 짧은 일정 블록으로 표현하며,
 * memo 에 "[ASSIGNMENT_DEADLINE:과제blockId]" 표식을 넣어 추적한다.
 */
public class AssignmentService {

    public static final DateTimeFormatter TS_FMT = ScheduleBlock.TS_FORMATTER;
    private static final String DEADLINE_TAG_PREFIX = "[ASSIGNMENT_DEADLINE:";
    private static final int DEADLINE_MARK_MINUTES = 30;

    private final AssignmentDAO assignmentDAO;
    private final ScheduleBlockDAO scheduleBlockDAO;
    private final EventBus eventBus = EventBus.getInstance();

    public AssignmentService(AssignmentDAO assignmentDAO, ScheduleBlockDAO scheduleBlockDAO) {
        this.assignmentDAO = assignmentDAO;
        this.scheduleBlockDAO = scheduleBlockDAO;
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
        // 형식 검증
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
        String start = dl.format(TS_FMT);
        String end   = dl.plusMinutes(DEADLINE_MARK_MINUTES).format(TS_FMT);

        ScheduleBlock mark = new ScheduleBlock("📌 마감: " + a.getName(), a.getColor(), start, end);
        mark.setSubjectId(a.getSubjectId());
        mark.setMemo(DEADLINE_TAG_PREFIX + a.getId() + "]");
        scheduleBlockDAO.insert(mark);
        eventBus.publish(EventBus.SCHEDULE_BLOCK_CREATED, mark);
    }

    /** 해당 과제와 연결된 마감 표시 일정 블록을 찾아 삭제. */
    private void removeDeadlineMark(String assignmentBlockId) {
        String tag = DEADLINE_TAG_PREFIX + assignmentBlockId + "]";
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
