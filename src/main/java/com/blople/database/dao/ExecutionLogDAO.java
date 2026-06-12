package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.model.automation.ExecutionLog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 자동화 실행 이력 DAO (FR-12). execution_log 단일 테이블.
 */
public class ExecutionLogDAO extends BaseDAO<ExecutionLog, String> {

    public ExecutionLogDAO(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override protected String getTableName()    { return "execution_log"; }
    @Override protected String getIdColumnName() { return "id"; }

    @Override
    protected ExecutionLog mapRow(ResultSet rs) throws SQLException {
        return ExecutionLog.reconstruct(
                rs.getString("id"),
                rs.getString("rule_id"),
                rs.getString("executed_at"),
                rs.getString("status"),
                rs.getString("result_message"));
    }

    public void insert(ExecutionLog log) {
        executeUpdate("INSERT INTO execution_log (id, rule_id, executed_at, status, result_message) " +
                        "VALUES (?, ?, ?, ?, ?)",
                log.getId(), log.getRuleId(), log.getExecutedAt(),
                log.getStatus(), log.getResultMessage());
    }

    public List<ExecutionLog> findByRule(String ruleId) {
        return queryList("SELECT * FROM execution_log WHERE rule_id = ? ORDER BY executed_at DESC", ruleId);
    }
}
