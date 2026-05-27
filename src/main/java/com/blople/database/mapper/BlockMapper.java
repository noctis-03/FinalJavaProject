package com.blople.database.mapper;

import com.blople.model.block.ScheduleBlock;
import com.blople.model.enums.BlockType;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ResultSet → 도메인 Block 객체 매핑.
 *
 * <p>현재는 ScheduleBlock 만 처리하며, 새 블록 유형이 추가될 때
 * 본 클래스에 정적 메서드로 함께 모은다.
 *
 * <p>JOIN 결과를 매핑한다는 가정이므로, 호출자는 SELECT 시
 * <pre>
 *   SELECT b.*, sb.* FROM block b JOIN schedule_block sb ON b.id = sb.block_id
 * </pre>
 * 형태로 두 테이블을 모두 가져와야 한다.
 */
public final class BlockMapper {

    private BlockMapper() { /* utility */ }

    public static ScheduleBlock mapScheduleBlock(ResultSet rs) throws SQLException {
        // block 공통 필드
        String id        = rs.getString("id");
        String name      = rs.getString("name");
        String color     = rs.getString("color");
        String memo      = rs.getString("memo");
        String createdAt = rs.getString("created_at");
        String updatedAt = rs.getString("updated_at");

        // schedule_block 전용 필드
        String startDt        = rs.getString("start_datetime");
        String endDt          = rs.getString("end_datetime");
        String subjectId      = rs.getString("subject_id");
        boolean isRecurring   = rs.getInt("is_recurring") == 1;
        String recurrenceRule = rs.getString("recurrence_rule");

        // 새 인스턴스를 만든 뒤 영속 상태(id, timestamps)를 복원
        ScheduleBlock block = new ScheduleBlock(name, color, startDt, endDt);
        block.restoreId(id);
        block.restoreCreatedAt(createdAt);
        block.restoreUpdatedAt(updatedAt);

        block.setMemo(memo);
        block.setSubjectId(subjectId);
        block.setRecurring(isRecurring);
        block.setRecurrenceRule(recurrenceRule);

        // setter 호출 과정에서 updatedAt 이 현재 시각으로 덮였으므로 한 번 더 복원
        block.restoreUpdatedAt(updatedAt);

        return block;
    }

    public static BlockType parseType(String s) {
        return s == null ? null : BlockType.valueOf(s);
    }
}
