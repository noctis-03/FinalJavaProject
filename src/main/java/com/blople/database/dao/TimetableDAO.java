package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.model.subject.Timetable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TimetableDAO extends BaseDAO<Timetable, String> {

    public TimetableDAO(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override protected String getTableName()    { return "timetable_entry"; }
    @Override protected String getIdColumnName() { return "id"; }

    @Override
    protected Timetable mapRow(ResultSet rs) throws SQLException {
        return Timetable.reconstruct(
                rs.getString("id"),
                rs.getString("subject_id"),
                rs.getInt("day_of_week"),
                rs.getString("start_time"),
                rs.getString("end_time"));
    }

    public void insert(Timetable t) {
        String sql = """
            INSERT INTO timetable_entry (id, subject_id, day_of_week, start_time, end_time)
            VALUES (?, ?, ?, ?, ?)
            """;
        executeUpdate(sql, t.getId(), t.getSubjectId(), t.getDayOfWeek(),
                t.getStartTime(), t.getEndTime());
    }

    public void update(Timetable t) {
        String sql = """
            UPDATE timetable_entry
               SET subject_id = ?, day_of_week = ?, start_time = ?, end_time = ?
             WHERE id = ?
            """;
        executeUpdate(sql, t.getSubjectId(), t.getDayOfWeek(),
                t.getStartTime(), t.getEndTime(), t.getId());
    }

    public List<Timetable> findBySubject(String subjectId) {
        return queryList(
                "SELECT * FROM timetable_entry WHERE subject_id = ? ORDER BY day_of_week, start_time",
                subjectId);
    }

    public List<Timetable> findBySemester(String semesterId) {
        return queryList("""
                SELECT t.*
                  FROM timetable_entry t
                  JOIN subject s ON t.subject_id = s.id
                 WHERE s.semester_id = ?
                 ORDER BY t.day_of_week, t.start_time
                """, semesterId);
    }
}
