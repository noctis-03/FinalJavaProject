package com.blople.database.mapper;

import com.blople.model.block.AssignmentBlock;
import com.blople.model.enums.AssignmentStatus;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ResultSet → AssignmentBlock 매핑 (FR-04).
 *
 * <p>block + assignment 두 테이블을 JOIN 한 결과를 가정한다.
 */
public final class AssignmentMapper {

    private AssignmentMapper() { /* utility */ }

    public static AssignmentBlock map(ResultSet rs) throws SQLException {
        // block 공통
        String id        = rs.getString("id");
        String name      = rs.getString("name");
        String color     = rs.getString("color");
        String memo      = rs.getString("memo");
        String createdAt = rs.getString("created_at");
        String updatedAt = rs.getString("updated_at");

        // assignment 전용
        String subjectId    = rs.getString("subject_id");
        String deadline     = rs.getString("deadline");
        String description  = rs.getString("description");
        String scoreWeight  = rs.getString("score_weight");
        String submitMethod = rs.getString("submit_method");
        String statusStr    = rs.getString("status");
        String workDate     = rs.getString("work_date");
        String workStart    = rs.getString("work_start_time");
        String workEnd      = rs.getString("work_end_time");

        AssignmentBlock a = new AssignmentBlock(subjectId, name, color, deadline);
        a.restoreId(id);
        a.restoreCreatedAt(createdAt);
        a.restoreUpdatedAt(updatedAt);

        a.setMemo(memo);
        a.setDescription(description);
        a.setScoreWeight(scoreWeight);
        a.setSubmitMethod(submitMethod);
        a.setStatus(parseStatus(statusStr));
        a.setWorkDate(workDate);
        a.setWorkStartTime(workStart);
        a.setWorkEndTime(workEnd);

        // setter 들이 updatedAt 을 덮었으므로 한 번 더 복원
        a.restoreUpdatedAt(updatedAt);
        return a;
    }

    private static AssignmentStatus parseStatus(String s) {
        if (s == null) return AssignmentStatus.NOT_STARTED;
        try {
            return AssignmentStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            return AssignmentStatus.NOT_STARTED;
        }
    }
}
