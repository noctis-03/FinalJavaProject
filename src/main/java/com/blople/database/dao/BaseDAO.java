package com.blople.database.dao;

import com.blople.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 모든 DAO 의 추상 기반.
 *
 * <p>공통 CRUD 골격(findById, findAll, delete, count, exists)을 제공하고,
 * 구체 DAO 가 다음 4개 메서드만 구현하면 된다.
 *
 * <ul>
 *   <li>{@link #getTableName()}</li>
 *   <li>{@link #getIdColumnName()}</li>
 *   <li>{@link #mapRow(ResultSet)}</li>
 *   <li>insert / update 는 각 DAO 가 직접 구현 (컬럼 구성이 모두 다르므로)</li>
 * </ul>
 *
 * <p>또한 외부에서 전달된 Connection(트랜잭션 컨텍스트)을 사용할 수 있는
 * 오버로드를 모두 제공하여 {@link com.blople.database.TransactionManager} 와 함께 쓸 수 있다.
 *
 * @param <T>  도메인 모델 타입
 * @param <ID> 식별자 타입 (대부분 String UUID)
 */
public abstract class BaseDAO<T, ID> {

    protected final DatabaseManager dbManager;

    protected BaseDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // ===== 구체 DAO 가 구현해야 할 추상 메서드 =====
    protected abstract String getTableName();
    protected abstract String getIdColumnName();
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    // ============================================================
    //  공통 CRUD — 자체 Connection 사용
    // ============================================================
    public Optional<T> findById(ID id) {
        try (Connection conn = dbManager.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new RuntimeException("findById 실패: " + e.getMessage(), e);
        }
    }

    public List<T> findAll() {
        try (Connection conn = dbManager.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RuntimeException("findAll 실패: " + e.getMessage(), e);
        }
    }

    public boolean delete(ID id) {
        try (Connection conn = dbManager.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new RuntimeException("delete 실패: " + e.getMessage(), e);
        }
    }

    public int count() {
        try (Connection conn = dbManager.getConnection()) {
            return count(conn);
        } catch (SQLException e) {
            throw new RuntimeException("count 실패: " + e.getMessage(), e);
        }
    }

    public boolean exists(ID id) {
        return findById(id).isPresent();
    }

    // ============================================================
    //  공통 CRUD — 외부 Connection(트랜잭션) 사용
    // ============================================================
    public Optional<T> findById(Connection conn, ID id) throws SQLException {
        String sql = "SELECT * FROM " + getTableName() + " WHERE " + getIdColumnName() + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    public List<T> findAll(Connection conn) throws SQLException {
        String sql = "SELECT * FROM " + getTableName();
        List<T> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public boolean delete(Connection conn, ID id) throws SQLException {
        String sql = "DELETE FROM " + getTableName() + " WHERE " + getIdColumnName() + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int count(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + getTableName();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        }
    }

    // ============================================================
    //  헬퍼 — 동적 WHERE 절을 가진 임의 조회
    // ============================================================
    protected List<T> queryList(String sql, Object... params) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("queryList 실패: " + e.getMessage(), e);
        }
    }

    protected Optional<T> querySingle(String sql, Object... params) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("querySingle 실패: " + e.getMessage(), e);
        }
    }

    protected int executeUpdate(String sql, Object... params) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("executeUpdate 실패: " + e.getMessage(), e);
        }
    }

    protected int executeUpdate(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            return ps.executeUpdate();
        }
    }

    private void bindParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
