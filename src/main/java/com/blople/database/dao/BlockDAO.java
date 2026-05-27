package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.model.block.Block;
import com.blople.model.enums.BlockType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * block 공통 테이블 전용 DAO.
 *
 * <p>실제 도메인 객체 매핑은 하지 않고, 다른 블록 DAO (ScheduleBlockDAO 등) 에서
 * "공통 행" 을 먼저 저장/삭제할 때 호출하는 용도이다.
 *
 * <p>즉, 일정 블록을 저장할 때는
 * <pre>
 *   blockDAO.insertBlockRow(conn, block);          // block 테이블
 *   scheduleBlockDAO.insertScheduleRow(conn, sb);  // schedule_block 테이블
 * </pre>
 * 처럼 한 트랜잭션 안에서 호출한다.
 */
public class BlockDAO extends BaseDAO<Block, String> {

    public BlockDAO(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override protected String getTableName()    { return "block"; }
    @Override protected String getIdColumnName() { return "id"; }

    /**
     * block 공통 테이블은 도메인 객체로 직접 매핑하지 않는다.
     * BaseDAO 의 findById/findAll 을 그대로 호출하면 예외가 난다 — 호출 금지.
     */
    @Override
    protected Block mapRow(ResultSet rs) {
        throw new UnsupportedOperationException(
                "BlockDAO 는 공통 행 저장 전용이며, 도메인 매핑은 ScheduleBlockDAO 등 서브 DAO 에서 한다.");
    }

    /** block 공통 행 INSERT — 호출자가 제공한 Connection 사용. */
    public void insertBlockRow(Connection conn, Block block) throws SQLException {
        String sql = """
            INSERT INTO block (id, block_type, name, color, memo, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, block.getId());
            ps.setString(2, block.getBlockType().name());
            ps.setString(3, block.getName());
            ps.setString(4, block.getColor());
            ps.setString(5, block.getMemo());
            ps.setString(6, block.getCreatedAt());
            ps.setString(7, block.getUpdatedAt());
            ps.executeUpdate();
        }
    }

    /** block 공통 행 UPDATE. */
    public void updateBlockRow(Connection conn, Block block) throws SQLException {
        String sql = """
            UPDATE block
               SET name = ?, color = ?, memo = ?, updated_at = ?
             WHERE id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, block.getName());
            ps.setString(2, block.getColor());
            ps.setString(3, block.getMemo());
            ps.setString(4, block.getUpdatedAt());
            ps.setString(5, block.getId());
            ps.executeUpdate();
        }
    }

    /**
     * block 공통 행 DELETE.
     * ON DELETE CASCADE 가 걸려 있으므로 서브 테이블 행도 자동 삭제된다.
     */
    public boolean deleteBlockRow(Connection conn, String blockId) throws SQLException {
        String sql = "DELETE FROM block WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, blockId);
            return ps.executeUpdate() > 0;
        }
    }

    /** 유형별 ID 목록만 가져오는 헬퍼 (디버깅·통계용). */
    public List<String> findIdsByType(BlockType type) {
        String sql = "SELECT id FROM block WHERE block_type = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<String> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getString(1));
                return ids;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findIdsByType 실패", e);
        }
    }
}
