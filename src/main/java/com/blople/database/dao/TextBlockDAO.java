package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.database.TransactionManager;
import com.blople.model.block.TextBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 텍스트 블록 DAO (FR-06). block + text_block 두 테이블 처리.
 */
public class TextBlockDAO extends BaseDAO<TextBlock, String> {

    private final BlockDAO blockDAO;
    private final TransactionManager txManager;

    public TextBlockDAO(DatabaseManager dbManager, BlockDAO blockDAO, TransactionManager txManager) {
        super(dbManager);
        this.blockDAO = blockDAO;
        this.txManager = txManager;
    }

    @Override protected String getTableName()    { return "text_block"; }
    @Override protected String getIdColumnName() { return "block_id"; }

    @Override
    protected TextBlock mapRow(ResultSet rs) throws SQLException {
        String id        = rs.getString("id");
        String name      = rs.getString("name");
        String memo      = rs.getString("memo");
        String createdAt = rs.getString("created_at");
        String updatedAt = rs.getString("updated_at");

        String subjectId = rs.getString("subject_id");
        String title     = rs.getString("title");
        String content   = rs.getString("content");
        String bgColor   = rs.getString("background_color");

        TextBlock t = new TextBlock(subjectId, title, content);
        t.restoreId(id);
        t.restoreCreatedAt(createdAt);
        t.restoreUpdatedAt(updatedAt);
        t.setMemo(memo);
        if (bgColor != null) t.setBackgroundColor(bgColor);
        t.restoreUpdatedAt(updatedAt);
        return t;
    }

    private static final String SELECT_JOIN =
            "SELECT b.id, b.name, b.color, b.memo, b.created_at, b.updated_at, " +
                    "       t.subject_id, t.title, t.content, t.background_color " +
                    "  FROM block b JOIN text_block t ON b.id = t.block_id ";

    public void insert(TextBlock t) {
        txManager.runInTransaction(conn -> {
            blockDAO.insertBlockRow(conn, t);
            insertTextRow(conn, t);
        });
    }

    public void update(TextBlock t) {
        t.touch();
        txManager.runInTransaction(conn -> {
            blockDAO.updateBlockRow(conn, t);
            updateTextRow(conn, t);
        });
    }

    public boolean deleteById(String blockId) {
        return txManager.executeInTransaction(conn -> blockDAO.deleteBlockRow(conn, blockId));
    }

    @Override
    public Optional<TextBlock> findById(String id) {
        return querySingle(SELECT_JOIN + " WHERE b.id = ?", id);
    }

    public List<TextBlock> findBySubject(String subjectId) {
        return queryList(SELECT_JOIN + " WHERE t.subject_id = ? ORDER BY b.created_at", subjectId);
    }

    private void insertTextRow(Connection conn, TextBlock t) throws SQLException {
        String sql = "INSERT INTO text_block (block_id, subject_id, title, content, background_color) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getId());
            ps.setString(2, t.getSubjectId());
            ps.setString(3, t.getTitle());
            ps.setString(4, t.getContent());
            ps.setString(5, t.getBackgroundColor());
            ps.executeUpdate();
        }
    }

    private void updateTextRow(Connection conn, TextBlock t) throws SQLException {
        String sql = "UPDATE text_block SET subject_id=?, title=?, content=?, background_color=? " +
                "WHERE block_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getSubjectId());
            ps.setString(2, t.getTitle());
            ps.setString(3, t.getContent());
            ps.setString(4, t.getBackgroundColor());
            ps.setString(5, t.getId());
            ps.executeUpdate();
        }
    }

    @Override
    protected List<TextBlock> queryList(String sql, Object... params) { return super.queryList(sql, params); }
    @Override
    protected Optional<TextBlock> querySingle(String sql, Object... params) { return super.querySingle(sql, params); }
}
