package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.model.canvas.CanvasState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 캔버스 배치 상태 DAO (FR-08). canvas_state 단일 테이블.
 */
public class CanvasStateDAO extends BaseDAO<CanvasState, String> {

    public CanvasStateDAO(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override protected String getTableName()    { return "canvas_state"; }
    @Override protected String getIdColumnName() { return "id"; }

    @Override
    protected CanvasState mapRow(ResultSet rs) throws SQLException {
        return CanvasState.reconstruct(
                rs.getString("id"),
                rs.getString("subject_id"),
                rs.getString("block_id"),
                rs.getDouble("pos_x"),
                rs.getDouble("pos_y"),
                rs.getDouble("width"),
                rs.getDouble("height"),
                rs.getInt("z_order"));
    }

    public void insert(CanvasState c) {
        String sql = "INSERT INTO canvas_state (id, subject_id, block_id, pos_x, pos_y, width, height, z_order) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        executeUpdate(sql, c.getId(), c.getSubjectId(), c.getBlockId(),
                c.getPosX(), c.getPosY(), c.getWidth(), c.getHeight(), c.getZOrder());
    }

    public void update(CanvasState c) {
        String sql = "UPDATE canvas_state SET pos_x=?, pos_y=?, width=?, height=?, z_order=? WHERE id=?";
        executeUpdate(sql, c.getPosX(), c.getPosY(), c.getWidth(), c.getHeight(),
                c.getZOrder(), c.getId());
    }

    public List<CanvasState> findBySubject(String subjectId) {
        return queryList("SELECT * FROM canvas_state WHERE subject_id = ? ORDER BY z_order", subjectId);
    }

    public Optional<CanvasState> findByBlock(String blockId) {
        return querySingle("SELECT * FROM canvas_state WHERE block_id = ?", blockId);
    }

    public boolean deleteByBlock(String blockId) {
        return executeUpdate("DELETE FROM canvas_state WHERE block_id = ?", blockId) > 0;
    }
}
