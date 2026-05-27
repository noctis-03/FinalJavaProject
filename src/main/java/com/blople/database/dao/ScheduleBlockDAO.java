package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.database.TransactionManager;
import com.blople.database.mapper.BlockMapper;
import com.blople.model.block.ScheduleBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 일정 블록 DAO (FR-01).
 *
 * <p>저장/수정/삭제는 block + schedule_block 두 테이블에 걸쳐 일어나므로
 * 내부적으로 {@link TransactionManager} 를 사용해 원자성을 보장한다.
 *
 * <p>조회는 두 테이블을 JOIN 해 한 번에 가져온다.
 */
public class ScheduleBlockDAO extends BaseDAO<ScheduleBlock, String> {

    private final BlockDAO blockDAO;
    private final TransactionManager txManager;

    public ScheduleBlockDAO(DatabaseManager dbManager,
                            BlockDAO blockDAO,
                            TransactionManager txManager) {
        super(dbManager);
        this.blockDAO  = blockDAO;
        this.txManager = txManager;
    }

    @Override protected String getTableName()    { return "schedule_block"; }
    @Override protected String getIdColumnName() { return "block_id"; }

    /** JOIN 결과 한 행을 ScheduleBlock 으로 매핑. */
    @Override
    protected ScheduleBlock mapRow(ResultSet rs) throws SQLException {
        return BlockMapper.mapScheduleBlock(rs);
    }

    // ============================================================
    //  Create / Update / Delete (트랜잭션으로 묶음)
    // ============================================================

    /** 일정 블록 신규 저장. block 공통 + schedule_block 전용 행을 함께 INSERT. */
    public void insert(ScheduleBlock block) {
        txManager.runInTransaction(conn -> {
            blockDAO.insertBlockRow(conn, block);
            insertScheduleRow(conn, block);
        });
    }

    /** 일정 블록 수정. 공통 + 전용 필드를 함께 UPDATE. */
    public void update(ScheduleBlock block) {
        block.touch(); // updated_at 갱신
        txManager.runInTransaction(conn -> {
            blockDAO.updateBlockRow(conn, block);
            updateScheduleRow(conn, block);
        });
    }

    /** ID 로 삭제. block 행만 지우면 CASCADE 로 schedule_block 도 함께 삭제된다. */
    public boolean deleteById(String blockId) {
        return txManager.executeInTransaction(conn ->
                blockDAO.deleteBlockRow(conn, blockId));
    }

    // ============================================================
    //  Read — JOIN 조회
    // ============================================================

    private static final String SELECT_JOIN =
            "SELECT b.id, b.name, b.color, b.memo, b.created_at, b.updated_at, " +
                    "       sb.start_datetime, sb.end_datetime, sb.subject_id, " +
                    "       sb.is_recurring, sb.recurrence_rule " +
                    "  FROM block b " +
                    "  JOIN schedule_block sb ON b.id = sb.block_id ";

    @Override
    public Optional<ScheduleBlock> findById(String id) {
        return querySingle(SELECT_JOIN + " WHERE b.id = ?", id);
    }

    @Override
    public List<ScheduleBlock> findAll() {
        return queryList(SELECT_JOIN + " ORDER BY sb.start_datetime ASC");
    }

    /** 지정한 [from, to) 범위와 겹치는 일정을 모두 조회. */
    public List<ScheduleBlock> findInRange(String fromDatetime, String toDatetime) {
        String sql = SELECT_JOIN +
                " WHERE sb.start_datetime < ? AND sb.end_datetime > ? " +
                " ORDER BY sb.start_datetime ASC";
        return queryList(sql, toDatetime, fromDatetime);
    }

    /** 특정 과목의 일정 블록만 조회. */
    public List<ScheduleBlock> findBySubject(String subjectId) {
        String sql = SELECT_JOIN +
                " WHERE sb.subject_id = ? " +
                " ORDER BY sb.start_datetime ASC";
        return queryList(sql, subjectId);
    }

    // ============================================================
    //  내부 — schedule_block 테이블만 다루는 메서드
    // ============================================================

    private void insertScheduleRow(Connection conn, ScheduleBlock b) throws SQLException {
        String sql = """
            INSERT INTO schedule_block
                (block_id, start_datetime, end_datetime, subject_id, is_recurring, recurrence_rule)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getId());
            ps.setString(2, b.getStartDatetime());
            ps.setString(3, b.getEndDatetime());
            ps.setString(4, b.getSubjectId());
            ps.setInt   (5, b.isRecurring() ? 1 : 0);
            ps.setString(6, b.getRecurrenceRule());
            ps.executeUpdate();
        }
    }

    private void updateScheduleRow(Connection conn, ScheduleBlock b) throws SQLException {
        String sql = """
            UPDATE schedule_block
               SET start_datetime = ?, end_datetime = ?, subject_id = ?,
                   is_recurring = ?, recurrence_rule = ?
             WHERE block_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getStartDatetime());
            ps.setString(2, b.getEndDatetime());
            ps.setString(3, b.getSubjectId());
            ps.setInt   (4, b.isRecurring() ? 1 : 0);
            ps.setString(5, b.getRecurrenceRule());
            ps.setString(6, b.getId());
            ps.executeUpdate();
        }
    }

    // ============================================================
    //  queryList/querySingle 오버라이드 — JOIN 컬럼 처리
    // ============================================================
    // (BaseDAO 의 헬퍼를 그대로 활용. mapRow 가 JOIN 결과를 처리한다.)
    @Override
    protected List<ScheduleBlock> queryList(String sql, Object... params) {
        return super.queryList(sql, params);
    }

    @Override
    protected Optional<ScheduleBlock> querySingle(String sql, Object... params) {
        return super.querySingle(sql, params);
    }
}
