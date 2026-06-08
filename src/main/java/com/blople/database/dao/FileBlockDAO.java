package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.database.TransactionManager;
import com.blople.database.mapper.FileBlockMapper;
import com.blople.model.block.FileBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 파일 블록 DAO (FR-05).
 *
 * <p>block + file_block 두 테이블에 걸쳐 저장/수정/삭제하므로
 * {@link TransactionManager} 로 원자성을 보장한다. 조회는 JOIN.
 */
public class FileBlockDAO extends BaseDAO<FileBlock, String> {

    private final BlockDAO blockDAO;
    private final TransactionManager txManager;

    public FileBlockDAO(DatabaseManager dbManager,
                        BlockDAO blockDAO,
                        TransactionManager txManager) {
        super(dbManager);
        this.blockDAO  = blockDAO;
        this.txManager = txManager;
    }

    @Override protected String getTableName()    { return "file_block"; }
    @Override protected String getIdColumnName() { return "block_id"; }

    @Override
    protected FileBlock mapRow(ResultSet rs) throws SQLException {
        return FileBlockMapper.map(rs);
    }

    private static final String SELECT_JOIN =
            "SELECT b.id, b.name, b.color, b.memo, b.created_at, b.updated_at, " +
                    "       f.subject_id, f.file_path, f.file_name, f.file_type, f.file_size " +
                    "  FROM block b " +
                    "  JOIN file_block f ON b.id = f.block_id ";

    // ===== CRUD =====
    public void insert(FileBlock f) {
        txManager.runInTransaction(conn -> {
            blockDAO.insertBlockRow(conn, f);
            insertFileRow(conn, f);
        });
    }

    public void update(FileBlock f) {
        f.touch();
        txManager.runInTransaction(conn -> {
            blockDAO.updateBlockRow(conn, f);
            updateFileRow(conn, f);
        });
    }

    public boolean deleteById(String blockId) {
        return txManager.executeInTransaction(conn ->
                blockDAO.deleteBlockRow(conn, blockId));
    }

    // ===== Read =====
    @Override
    public Optional<FileBlock> findById(String id) {
        return querySingle(SELECT_JOIN + " WHERE b.id = ?", id);
    }

    @Override
    public List<FileBlock> findAll() {
        return queryList(SELECT_JOIN + " ORDER BY b.created_at ASC");
    }

    public List<FileBlock> findBySubject(String subjectId) {
        return queryList(SELECT_JOIN + " WHERE f.subject_id = ? ORDER BY b.created_at ASC", subjectId);
    }

    // ===== 내부 — file_block 테이블 전용 =====
    private void insertFileRow(Connection conn, FileBlock f) throws SQLException {
        String sql = """
            INSERT INTO file_block (block_id, subject_id, file_path, file_name, file_type, file_size)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, f.getId());
            ps.setString(2, f.getSubjectId());
            ps.setString(3, f.getFilePath());
            ps.setString(4, f.getFileName());
            ps.setString(5, f.getFileType());
            ps.setLong  (6, f.getFileSize());
            ps.executeUpdate();
        }
    }

    private void updateFileRow(Connection conn, FileBlock f) throws SQLException {
        String sql = """
            UPDATE file_block
               SET subject_id = ?, file_path = ?, file_name = ?, file_type = ?, file_size = ?
             WHERE block_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, f.getSubjectId());
            ps.setString(2, f.getFilePath());
            ps.setString(3, f.getFileName());
            ps.setString(4, f.getFileType());
            ps.setLong  (5, f.getFileSize());
            ps.setString(6, f.getId());
            ps.executeUpdate();
        }
    }

    @Override
    protected List<FileBlock> queryList(String sql, Object... params) {
        return super.queryList(sql, params);
    }

    @Override
    protected Optional<FileBlock> querySingle(String sql, Object... params) {
        return super.querySingle(sql, params);
    }
}
