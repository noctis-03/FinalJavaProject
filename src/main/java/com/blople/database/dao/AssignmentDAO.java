package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.database.TransactionManager;
import com.blople.database.mapper.AssignmentMapper;
import com.blople.model.block.AssignmentBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 과제 블록 DAO (FR-04).
 *
 * <p>block + assignment 두 테이블에 걸쳐 저장/수정/삭제하므로
 * {@link TransactionManager} 로 원자성을 보장한다. 조회는 JOIN.
 */
public class AssignmentDAO extends BaseDAO<AssignmentBlock, String> {

    private final BlockDAO blockDAO;
    private final TransactionManager txManager;

    public AssignmentDAO(DatabaseManager dbManager,
                         BlockDAO blockDAO,
                         TransactionManager txManager) {
        super(dbManager);
        this.blockDAO  = blockDAO;
        this.txManager = txManager;
    }

    @Override protected String getTableName()    { return "assignment"; }
    @Override protected String getIdColumnName() { return "block_id"; }

    @Override
    protected AssignmentBlock mapRow(ResultSet rs) throws SQLException {
        return AssignmentMapper.map(rs);
    }

    private static final String SELECT_JOIN =
            "SELECT b.id, b.name, b.color, b.memo, b.created_at, b.updated_at, " +
                    "       a.subject_id, a.deadline, a.description, a.score_weight, " +
                    "       a.submit_method, a.status, a.work_date, a.work_start_time, a.work_end_time " +
                    "  FROM block b " +
                    "  JOIN assignment a ON b.id = a.block_id ";

    // ===== CRUD =====
    public void insert(AssignmentBlock a) {
        txManager.runInTransaction(conn -> {
            blockDAO.insertBlockRow(conn, a);
            insertAssignmentRow(conn, a);
        });
    }

    public void update(AssignmentBlock a) {
        a.touch();
        txManager.runInTransaction(conn -> {
            blockDAO.updateBlockRow(conn, a);
            updateAssignmentRow(conn, a);
        });
    }

    public boolean deleteById(String blockId) {
        return txManager.executeInTransaction(conn ->
                blockDAO.deleteBlockRow(conn, blockId));
    }

    // ===== Read =====
    @Override
    public Optional<AssignmentBlock> findById(String id) {
        return querySingle(SELECT_JOIN + " WHERE b.id = ?", id);
    }

    @Override
    public List<AssignmentBlock> findAll() {
        return queryList(SELECT_JOIN + " ORDER BY a.deadline ASC");
    }

    public List<AssignmentBlock> findBySubject(String subjectId) {
        return queryList(SELECT_JOIN + " WHERE a.subject_id = ? ORDER BY a.deadline ASC", subjectId);
    }

    /** 마감일이 [from, to) 사이인 과제 (캘린더 마감 표시용). */
    public List<AssignmentBlock> findByDeadlineRange(String from, String to) {
        return queryList(SELECT_JOIN +
                " WHERE a.deadline >= ? AND a.deadline < ? ORDER BY a.deadline ASC", from, to);
    }

    // ===== 내부 — assignment 테이블 전용 =====
    private void insertAssignmentRow(Connection conn, AssignmentBlock a) throws SQLException {
        String sql = """
            INSERT INTO assignment
                (block_id, subject_id, deadline, description, score_weight, submit_method,
                 status, work_date, work_start_time, work_end_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,  a.getId());
            ps.setString(2,  a.getSubjectId());
            ps.setString(3,  a.getDeadline());
            ps.setString(4,  a.getDescription());
            ps.setString(5,  a.getScoreWeight());
            ps.setString(6,  a.getSubmitMethod());
            ps.setString(7,  a.getStatus().name());
            ps.setString(8,  a.getWorkDate());
            ps.setString(9,  a.getWorkStartTime());
            ps.setString(10, a.getWorkEndTime());
            ps.executeUpdate();
        }
    }

    private void updateAssignmentRow(Connection conn, AssignmentBlock a) throws SQLException {
        String sql = """
            UPDATE assignment
               SET subject_id = ?, deadline = ?, description = ?, score_weight = ?,
                   submit_method = ?, status = ?, work_date = ?, work_start_time = ?, work_end_time = ?
             WHERE block_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getSubjectId());
            ps.setString(2, a.getDeadline());
            ps.setString(3, a.getDescription());
            ps.setString(4, a.getScoreWeight());
            ps.setString(5, a.getSubmitMethod());
            ps.setString(6, a.getStatus().name());
            ps.setString(7, a.getWorkDate());
            ps.setString(8, a.getWorkStartTime());
            ps.setString(9, a.getWorkEndTime());
            ps.setString(10, a.getId());
            ps.executeUpdate();
        }
    }

    @Override
    protected List<AssignmentBlock> queryList(String sql, Object... params) {
        return super.queryList(sql, params);
    }

    @Override
    protected Optional<AssignmentBlock> querySingle(String sql, Object... params) {
        return super.querySingle(sql, params);
    }
}
