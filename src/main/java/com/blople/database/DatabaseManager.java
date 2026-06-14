package com.blople.database;

import com.blople.common.Constants;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite 연결 관리 싱글톤.
 *
 * <p>v0.8 추가: {@link #wipeAllData()} — 모든 사용자 데이터를 비우고
 * 스키마를 재생성한다 (DB 전체 초기화, 요구 #6).
 */
public final class DatabaseManager {

    private static DatabaseManager instance;
    private boolean initialized = false;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "SQLite JDBC 드라이버를 찾을 수 없습니다. lib/sqlite-jdbc-*.jar 를 추가했는지 확인하세요.", e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public synchronized void initialize() {
        if (initialized) return;

        File dir = new File(Constants.DB_DIRECTORY);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("DB 디렉터리 생성 실패: " + dir.getAbsolutePath());
        }

        SchemaInitializer initializer = new SchemaInitializer(this);
        initializer.initializeAllTables();

        initialized = true;
        System.out.println("[DatabaseManager] 초기화 완료 → "
                + new File(Constants.DB_DIRECTORY, Constants.DB_FILE_NAME).getAbsolutePath());
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(Constants.DB_URL);
        enableForeignKeys(conn);
        return conn;
    }

    private void enableForeignKeys(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
    }

    public void backup(String destPath) {
        File src  = new File(Constants.DB_DIRECTORY, Constants.DB_FILE_NAME);
        File dest = new File(destPath);
        try (var in  = new java.io.FileInputStream(src);
             var out = new java.io.FileOutputStream(dest)) {
            in.transferTo(out);
        } catch (Exception e) {
            System.err.println("[DatabaseManager] 백업 실패: " + e.getMessage());
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 모든 사용자 테이블을 DROP 한 후 스키마를 다시 생성한다.
     * (학기/과목/시간표/과제/캔버스/자동화/외부 링크 모두 사라짐 — 요구 #6)
     */
    public synchronized void wipeAllData() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1) sqlite_master 에서 사용자 테이블 목록 조회 (sqlite_* 제외)
            List<String> tables = new ArrayList<>();
            try (var rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master " +
                            "WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
                while (rs.next()) tables.add(rs.getString(1));
            }

            // 2) 외래키 잠깐 끄고 모두 DROP
            stmt.execute("PRAGMA foreign_keys = OFF");
            conn.setAutoCommit(false);
            for (String t : tables) {
                stmt.execute("DROP TABLE IF EXISTS \"" + t + "\"");
            }
            conn.commit();
            stmt.execute("PRAGMA foreign_keys = ON");

        } catch (SQLException e) {
            throw new RuntimeException("DB 초기화(드롭) 실패: " + e.getMessage(), e);
        }

        // 3) 스키마 재생성
        SchemaInitializer initializer = new SchemaInitializer(this);
        initializer.initializeAllTables();

        System.out.println("[DatabaseManager] 전체 초기화 완료");
    }
}
