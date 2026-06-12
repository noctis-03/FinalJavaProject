package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.database.TransactionManager;
import com.blople.model.block.AutomationBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 자동화(실행) 블록 DAO (FR-13).
 *
 * <p>block + automation_block 두 테이블에 걸쳐 저장/삭제하므로
 * {@link TransactionManager} 로 원자성을 보장한다. 조회는 JOIN.
 *
 * <p>automation_block 전용 필드: subject_id, rule_id.
 */
public class AutomationBlockDAO extends BaseDAO<AutomationBlock, String> {

    private final BlockDAO blockDAO;
    private final TransactionManager txManager;

    public AutomationBlockDAO(DatabaseManager dbManager,
                              BlockDAO blockDAO,
                              TransactionManager txManager) {
        super(dbManager);
        this.blockDAO  = blockDAO;
        this.txManager = txManager;
    }

    @Override protected String getTableName()    { return "automation_block"; }
    @Override protected String getIdColumnName() { return "block_id"; }

    @Override
    protected AutomationBlock mapRow(ResultSet rs) throws SQLException {
        String id        = rs.getString("id");
        String name      = rs.getString("name");
        String color     = rs.getString("color");
        String memo      = rs.getString("memo");
        String createdAt = rs.getString("created_at");
        String updatedAt = rs.getString("updated_at");

        String ruleId    = rs.getString("rule_id");

        AutomationBlock a = new AutomationBlock(name, color, ruleId);
        a.restoreId(id);
        a.restoreCreatedAt(createdAt);
        a.restoreUpdatedAt(updatedAt);
        a.setMemo(memo);
        a.restoreUpdatedAt(updatedAt);
        return a;
    }

    private static final String SELECT_JOIN =
            "SELECT b.id, b.name, b.color, b.memo, b.created_at, b.updated_at, " +
                    "       ab.subject_id, ab.rule_id " +
                    "  FROM block b " +
                    "  JOIN automation_block ab ON b.id = ab.block_id ";

    public void insert(AutomationBlock a, String subjectId) {
        txManager.runInTransaction(conn -> {
            blockDAO.insertBlockRow(conn, a);
            insertAutomationRow(conn, a, subjectId);
        });
    }

    public boolean deleteById(String blockId) {
        return txManager.executeInTransaction(conn ->
                blockDAO.deleteBlockRow(conn, blockId));
    }

    @Override
    public Optional<AutomationBlock> findById(String id) {
        return querySingle(SELECT_JOIN + " WHERE b.id = ?", id);
    }

    public List<AutomationBlock> findBySubject(String subjectId) {
        return queryList(SELECT_JOIN + " WHERE ab.subject_id = ? ORDER BY b.created_at ASC", subjectId);
    }

    /** 이 자동화 블록이 가리키는 rule_id 조회 (편의). */
    public Optional<String> findRuleId(String blockId) {
        return findById(blockId).map(AutomationBlock::getRuleId);
    }

    private void insertAutomationRow(Connection conn, AutomationBlock a, String subjectId) throws SQLException {
        String sql = "INSERT INTO automation_block (block_id, subject_id, rule_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getId());
            ps.setString(2, subjectId);
            ps.setString(3, a.getRuleId());
            ps.executeUpdate();
        }
    }

    @Override
    protected List<AutomationBlock> queryList(String sql, Object... params) {
        return super.queryList(sql, params);
    }

    @Override
    protected Optional<AutomationBlock> querySingle(String sql, Object... params) {
        return super.querySingle(sql, params);
    }
}
