package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.model.subject.Semester;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class SemesterDAO extends BaseDAO<Semester, String> {

    public SemesterDAO(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override protected String getTableName()    { return "semester"; }
    @Override protected String getIdColumnName() { return "id"; }

    @Override
    protected Semester mapRow(ResultSet rs) throws SQLException {
        return Semester.reconstruct(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("start_date"),
                rs.getString("end_date"),
                rs.getInt("is_active") == 1,
                rs.getString("created_at"));
    }

    public void insert(Semester s) {
        String sql = """
            INSERT INTO semester (id, name, start_date, end_date, is_active, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        executeUpdate(sql, s.getId(), s.getName(), s.getStartDate(),
                s.getEndDate(), s.isActive() ? 1 : 0, s.getCreatedAt());
    }

    public void update(Semester s) {
        String sql = """
            UPDATE semester SET name = ?, start_date = ?, end_date = ?, is_active = ?
             WHERE id = ?
            """;
        executeUpdate(sql, s.getName(), s.getStartDate(), s.getEndDate(),
                s.isActive() ? 1 : 0, s.getId());
    }

    /** 단 하나의 학기만 active 상태가 되도록 토글한다. */
    public void setActive(String semesterId) {
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement("UPDATE semester SET is_active = 0");
                 PreparedStatement ps2 = conn.prepareStatement(
                         "UPDATE semester SET is_active = 1 WHERE id = ?")) {
                ps1.executeUpdate();
                ps2.setString(1, semesterId);
                ps2.executeUpdate();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("setActive 실패", e);
        }
    }

    public Optional<Semester> findActive() {
        return querySingle("SELECT * FROM semester WHERE is_active = 1 LIMIT 1");
    }
}
