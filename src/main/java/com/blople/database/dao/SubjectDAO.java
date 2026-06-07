package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.model.subject.Subject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class SubjectDAO extends BaseDAO<Subject, String> {

    public SubjectDAO(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override protected String getTableName()    { return "subject"; }
    @Override protected String getIdColumnName() { return "id"; }

    @Override
    protected Subject mapRow(ResultSet rs) throws SQLException {
        return Subject.reconstruct(
                rs.getString("id"),
                rs.getString("semester_id"),
                rs.getString("name"),
                rs.getString("professor"),
                rs.getString("classroom"),
                rs.getString("color"),
                rs.getInt("sort_order"),
                rs.getString("created_at"));
    }

    public void insert(Subject s) {
        String sql = """
            INSERT INTO subject (id, semester_id, name, professor, classroom, color, sort_order, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        executeUpdate(sql, s.getId(), s.getSemesterId(), s.getName(),
                s.getProfessor(), s.getClassroom(), s.getColor(),
                s.getSortOrder(), s.getCreatedAt());
    }

    public void update(Subject s) {
        String sql = """
            UPDATE subject
               SET name = ?, professor = ?, classroom = ?, color = ?, sort_order = ?
             WHERE id = ?
            """;
        executeUpdate(sql, s.getName(), s.getProfessor(), s.getClassroom(),
                s.getColor(), s.getSortOrder(), s.getId());
    }

    public List<Subject> findBySemester(String semesterId) {
        return queryList(
                "SELECT * FROM subject WHERE semester_id = ? ORDER BY sort_order ASC, name ASC",
                semesterId);
    }

    public Optional<Subject> findByNameInSemester(String semesterId, String name) {
        return querySingle(
                "SELECT * FROM subject WHERE semester_id = ? AND name = ?",
                semesterId, name);
    }
}
