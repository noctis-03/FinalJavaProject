package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.database.TransactionManager;
import com.blople.database.mapper.PresetBlockMapper;
import com.blople.model.block.PresetBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 미리블록 템플릿 DAO (FR-02).
 *
 * <p>block + preset_block 두 테이블에 걸쳐 저장/수정/삭제하므로
 * {@link TransactionManager} 로 원자성을 보장한다. 조회는 JOIN.
 */
public class PresetBlockDAO extends BaseDAO<PresetBlock, String> {

    private final BlockDAO blockDAO;
    private final TransactionManager txManager;

    public PresetBlockDAO(DatabaseManager dbManager,
                          BlockDAO blockDAO,
                          TransactionManager txManager) {
        super(dbManager);
        this.blockDAO  = blockDAO;
        this.txManager = txManager;
    }

    @Override protected String getTableName()    { return "preset_block"; }
    @Override protected String getIdColumnName() { return "block_id"; }

    @Override
    protected PresetBlock mapRow(ResultSet rs) throws SQLException {
        return PresetBlockMapper.map(rs);
    }

    private static final String SELECT_JOIN =
            "SELECT b.id, b.name, b.color, b.memo, b.created_at, b.updated_at, " +
                    "       p.duration_minutes, p.category " +
                    "  FROM block b " +
                    "  JOIN preset_block p ON b.id = p.block_id ";

    // ===== CRUD =====
    public void insert(PresetBlock p) {
        txManager.runInTransaction(conn -> {
            blockDAO.insertBlockRow(conn, p);
            insertPresetRow(conn, p);
        });
    }

    public void update(PresetBlock p) {
        p.touch();
        txManager.runInTransaction(conn -> {
            blockDAO.updateBlockRow(conn, p);
            updatePresetRow(conn, p);
        });
    }

    public boolean deleteById(String blockId) {
        return txManager.executeInTransaction(conn ->
                blockDAO.deleteBlockRow(conn, blockId));
    }

    // ===== Read =====
    @Override
    public Optional<PresetBlock> findById(String id) {
        return querySingle(SELECT_JOIN + " WHERE b.id = ?", id);
    }

    @Override
    public List<PresetBlock> findAll() {
        return queryList(SELECT_JOIN + " ORDER BY b.created_at ASC");
    }

    public Optional<PresetBlock> findByName(String name) {
        return querySingle(SELECT_JOIN + " WHERE b.name = ?", name);
    }

    // ===== 내부 — preset_block 테이블 전용 =====
    private void insertPresetRow(Connection conn, PresetBlock p) throws SQLException {
        String sql = "INSERT INTO preset_block (block_id, duration_minutes, category) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setInt   (2, p.getDurationMinutes());
            ps.setString(3, p.getCategory() == null ? null : p.getCategory().name());
            ps.executeUpdate();
        }
    }

    private void updatePresetRow(Connection conn, PresetBlock p) throws SQLException {
        String sql = "UPDATE preset_block SET duration_minutes = ?, category = ? WHERE block_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt   (1, p.getDurationMinutes());
            ps.setString(2, p.getCategory() == null ? null : p.getCategory().name());
            ps.setString(3, p.getId());
            ps.executeUpdate();
        }
    }

    @Override
    protected List<PresetBlock> queryList(String sql, Object... params) {
        return super.queryList(sql, params);
    }

    @Override
    protected Optional<PresetBlock> querySingle(String sql, Object... params) {
        return super.querySingle(sql, params);
    }
}
