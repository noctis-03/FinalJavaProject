package com.blople.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * 트랜잭션 관리 유틸.
 *
 * <p>여러 DAO 작업을 원자적으로 묶고 싶을 때 사용한다. 예시:
 * <pre>
 * txManager.executeInTransaction(conn -&gt; {
 *     blockDAO.insert(conn, block);
 *     scheduleBlockDAO.insert(conn, scheduleBlock);
 *     return null;
 * });
 * </pre>
 *
 * <p>단일 INSERT/UPDATE 처럼 한 번의 SQL 만 실행하는 경우에는
 * 굳이 본 클래스를 거치지 않고 DAO 가 자체적으로 처리해도 된다.
 */
public class TransactionManager {

    private final DatabaseManager dbManager;

    public TransactionManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * 콜백 안의 작업들을 하나의 트랜잭션으로 실행한다.
     * 예외 발생 시 자동 롤백, 정상 종료 시 커밋.
     *
     * @param action Connection 을 받아 작업을 수행하고 결과를 반환하는 함수
     * @return action 의 반환값
     */
    public <T> T executeInTransaction(TransactionalAction<T> action) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            T result = action.apply(conn);

            conn.commit();
            return result;
        } catch (Exception e) {
            rollbackQuietly(conn);
            throw new RuntimeException("트랜잭션 실패: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    /** 반환값이 필요 없는 경우의 편의 메서드. */
    public void runInTransaction(VoidTransactionalAction action) {
        executeInTransaction(conn -> {
            action.apply(conn);
            return null;
        });
    }

    private void rollbackQuietly(Connection conn) {
        if (conn == null) return;
        try {
            conn.rollback();
        } catch (SQLException ex) {
            System.err.println("[TransactionManager] 롤백 실패: " + ex.getMessage());
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn == null) return;
        try {
            conn.setAutoCommit(true);
            conn.close();
        } catch (SQLException ex) {
            System.err.println("[TransactionManager] 연결 종료 실패: " + ex.getMessage());
        }
    }

    // ===== Functional Interfaces =====
    @FunctionalInterface
    public interface TransactionalAction<T> {
        T apply(Connection conn) throws Exception;
    }

    @FunctionalInterface
    public interface VoidTransactionalAction {
        void apply(Connection conn) throws Exception;
    }
}
