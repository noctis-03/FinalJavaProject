package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.database.TransactionManager;
import com.blople.database.mapper.LinkBlockMapper;
import com.blople.model.block.LinkBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 링크 블록 DAO (FR-07).
 *
 * <p>block + link_block 두 테이블에 걸쳐 저장/수정/삭제하므로
 * {@link TransactionManager} 로 원자성을 보장한다. 조회는 JOIN.
 */
public class LinkBlockDAO extends BaseDAO<LinkBlock, String> {

    private final BlockDAO blockDAO;
    private final TransactionManager txManager;

    public LinkBlockDAO(DatabaseManager dbManager,
                        BlockDAO blockDAO,
                        TransactionManager txManager) {
        super(dbManager);
        this.blockDAO  = blockDAO;
        this.txManager = txManager;
    }

    @Override protected String getTableName()    { return "link_block"; }
    @Override protected String getIdColumnName() { return "block_id"; }

    @Override
    protected LinkBlock mapRow(ResultSet rs) throws SQLException {
        return LinkBlockMapper.map(rs);
    }

    private static final String SELECT_JOIN =
            "SELECT b.id, b.name, b.color, b.memo, b.created_at, b.updated_at, " +
                    "       l.subject_id, l.url, l.display_name, l.link_type " +
                    "  FROM block b " +
                    "  JOIN link_block l ON b.id = l.block_id ";

    // ===== CRUD =====
    public void insert(LinkBlock l) {
        txManager.runInTransaction(conn -> {
            blockDAO.insertBlockRow(conn, l);
            insertLinkRow(conn, l);
        });
    }

    public void update(LinkBlock l) {
        l.touch();
        txManager.runInTransaction(conn -> {
            blockDAO.updateBlockRow(conn, l);
            updateLinkRow(conn, l);
        });
    }

    public boolean deleteById(String blockId) {
        return txManager.executeInTransaction(conn ->
                blockDAO.deleteBlockRow(conn, blockId));
    }

    // ===== Read =====
    @Override
    public Optional<LinkBlock> findById(String id) {
        return querySingle(SELECT_JOIN + " WHERE b.id = ?", id);
    }

    @Override
    public List<LinkBlock> findAll() {
        return queryList(SELECT_JOIN + " ORDER BY b.created_at ASC");
    }

    public List<LinkBlock> findBySubject(String subjectId) {
        return queryList(SELECT_JOIN + " WHERE l.subject_id = ? ORDER BY b.created_at ASC", subjectId);
    }

    // ===== 내부 — link_block 테이블 전용 =====
    private void insertLinkRow(Connection conn, LinkBlock l) throws SQLException {
        String sql = """
            INSERT INTO link_block (block_id, subject_id, url, display_name, link_type)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, l.getId());
            ps.setString(2, l.getSubjectId());
            ps.setString(3, l.getUrl());
            ps.setString(4, l.getDisplayName());
            ps.setString(5, l.getLinkType());
            ps.executeUpdate();
        }
    }

    private void updateLinkRow(Connection conn, LinkBlock l) throws SQLException {
        String sql = """
            UPDATE link_block
               SET subject_id = ?, url = ?, display_name = ?, link_type = ?
             WHERE block_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, l.getSubjectId());
            ps.setString(2, l.getUrl());
            ps.setString(3, l.getDisplayName());
            ps.setString(4, l.getLinkType());
            ps.setString(5, l.getId());
            ps.executeUpdate();
        }
    }

    @Override
    protected List<LinkBlock> queryList(String sql, Object... params) {
        return super.queryList(sql, params);
    }

    @Override
    protected Optional<LinkBlock> querySingle(String sql, Object... params) {
        return super.querySingle(sql, params);
    }
}
