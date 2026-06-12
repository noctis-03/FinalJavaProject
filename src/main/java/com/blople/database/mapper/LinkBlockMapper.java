package com.blople.database.mapper;

import com.blople.model.block.LinkBlock;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ResultSet → LinkBlock 매핑 (FR-07).
 *
 * <p>block + link_block 두 테이블을 JOIN 한 결과를 가정한다.
 */
public final class LinkBlockMapper {

    private LinkBlockMapper() { /* utility */ }

    public static LinkBlock map(ResultSet rs) throws SQLException {
        // block 공통
        String id        = rs.getString("id");
        String color     = rs.getString("color");
        String memo      = rs.getString("memo");
        String createdAt = rs.getString("created_at");
        String updatedAt = rs.getString("updated_at");

        // link_block 전용
        String subjectId   = rs.getString("subject_id");
        String url         = rs.getString("url");
        String displayName = rs.getString("display_name");
        String linkType    = rs.getString("link_type");

        LinkBlock l = new LinkBlock(subjectId, url, displayName);
        l.restoreId(id);
        l.restoreCreatedAt(createdAt);
        l.restoreUpdatedAt(updatedAt);
        if (color != null) l.setColor(color);
        l.setMemo(memo);
        l.setLinkType(linkType);
        // setter 들이 updatedAt 을 덮었으므로 한 번 더 복원
        l.restoreUpdatedAt(updatedAt);
        return l;
    }
}
