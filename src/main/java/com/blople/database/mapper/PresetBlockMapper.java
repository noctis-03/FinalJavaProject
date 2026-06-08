package com.blople.database.mapper;

import com.blople.model.block.PresetBlock;
import com.blople.model.enums.Category;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ResultSet → PresetBlock 매핑 (FR-02).
 *
 * <p>block + preset_block 두 테이블을 JOIN 한 결과를 가정한다.
 */
public final class PresetBlockMapper {

    private PresetBlockMapper() { /* utility */ }

    public static PresetBlock map(ResultSet rs) throws SQLException {
        String id        = rs.getString("id");
        String name      = rs.getString("name");
        String color     = rs.getString("color");
        String memo      = rs.getString("memo");
        String createdAt = rs.getString("created_at");
        String updatedAt = rs.getString("updated_at");

        int duration     = rs.getInt("duration_minutes");
        String catStr    = rs.getString("category");

        PresetBlock p = new PresetBlock(name, color, duration, parseCategory(catStr));
        p.restoreId(id);
        p.restoreCreatedAt(createdAt);
        p.restoreUpdatedAt(updatedAt);
        p.setMemo(memo);
        p.restoreUpdatedAt(updatedAt);
        return p;
    }

    private static Category parseCategory(String s) {
        if (s == null) return Category.OTHER;
        try {
            return Category.valueOf(s);
        } catch (IllegalArgumentException e) {
            return Category.OTHER;
        }
    }
}
