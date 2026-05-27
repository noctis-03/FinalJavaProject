package com.blople.model.block;

import com.blople.model.enums.AssignmentStatus;
import com.blople.model.enums.BlockType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 과제 블록 (FR-04).
 * 과목에 종속되며 캘린더에 마감일을 자동 연동한다.
 */
public class AssignmentBlock extends Block {

    private String subjectId;
    private String deadline;        // "yyyy-MM-dd HH:mm:ss"
    private String description;
    private String scoreWeight;
    private String submitMethod;
    private AssignmentStatus status;
    private String workDate;
    private String workStartTime;
    private String workEndTime;

    public AssignmentBlock(String subjectId, String name, String color, String deadline) {
        super(BlockType.ASSIGNMENT, name, color);
        this.subjectId = subjectId;
        this.deadline = deadline;
        this.status = AssignmentStatus.NOT_STARTED;
    }

    public String getSubjectId()                                 { return subjectId; }
    public void setSubjectId(String subjectId)                   { this.subjectId = subjectId; touch(); }
    public String getDeadline()                                  { return deadline; }
    public void setDeadline(String deadline)                     { this.deadline = deadline; touch(); }
    public String getDescription()                               { return description; }
    public void setDescription(String description)               { this.description = description; touch(); }
    public String getScoreWeight()                               { return scoreWeight; }
    public void setScoreWeight(String scoreWeight)               { this.scoreWeight = scoreWeight; touch(); }
    public String getSubmitMethod()                              { return submitMethod; }
    public void setSubmitMethod(String submitMethod)             { this.submitMethod = submitMethod; touch(); }
    public AssignmentStatus getStatus()                          { return status; }
    public void setStatus(AssignmentStatus status)               { this.status = status; touch(); }
    public String getWorkDate()                                  { return workDate; }
    public void setWorkDate(String workDate)                     { this.workDate = workDate; touch(); }
    public String getWorkStartTime()                             { return workStartTime; }
    public void setWorkStartTime(String workStartTime)           { this.workStartTime = workStartTime; touch(); }
    public String getWorkEndTime()                               { return workEndTime; }
    public void setWorkEndTime(String workEndTime)               { this.workEndTime = workEndTime; touch(); }

    public boolean isOverdue() {
        if (deadline == null) return false;
        LocalDateTime d = LocalDateTime.parse(deadline, TS_FORMATTER);
        return d.isBefore(LocalDateTime.now()) && status != AssignmentStatus.DONE;
    }

    public boolean isApproachingDeadline(int withinDays) {
        if (deadline == null || status == AssignmentStatus.DONE) return false;
        LocalDate today = LocalDate.now();
        LocalDate dDay  = LocalDateTime.parse(deadline, TS_FORMATTER).toLocalDate();
        long days = ChronoUnit.DAYS.between(today, dDay);
        return days >= 0 && days <= withinDays;
    }
}
