package com.blople.database.mapper;

import com.blople.model.block.FileBlock;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ResultSet → FileBlock 매핑 (FR-05).
 *
 * <p>block + file_block 두 테이블을 JOIN 한 결과를 가정한다.
 */
public final class FileBlockMapper {

    private FileBlockMapper() { /* utility */ }

    public static FileBlock map(ResultSet rs) throws SQLException {
        // block 공통
        String id        = rs.getString("id");
        String name      = rs.getString("name");
        String color     = rs.getString("color");
        String memo      = rs.getString("memo");
        String createdAt = rs.getString("created_at");
        String updatedAt = rs.getString("updated_at");

        // file_block 전용
        String subjectId = rs.getString("subject_id");
        String filePath  = rs.getString("file_path");
        String fileName  = rs.getString("file_name");
        String fileType  = rs.getString("file_type");
        long   fileSize  = rs.getLong("file_size");

        FileBlock f = new FileBlock(subjectId, name, color, filePath, fileName, fileType, fileSize);
        f.restoreId(id);
        f.restoreCreatedAt(createdAt);
        f.restoreUpdatedAt(updatedAt);
        f.setMemo(memo);
        f.restoreUpdatedAt(updatedAt); // setMemo 가 덮은 것 복원
        return f;
    }
}
