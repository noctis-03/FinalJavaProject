package com.blople.database;

import com.blople.common.Constants;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite 연결을 관리하는 싱글톤.
 *
 * <p>모든 DB 접근의 진입점이며, 아래 책임을 가진다.
 * <ul>
 *   <li>JDBC 드라이버 로딩</li>
 *   <li>DB 파일이 존재하지 않으면 디렉터리·파일 자동 생성</li>
 *   <li>외래키(FOREIGN KEY) 제약 활성화</li>
 *   <li>{@link Connection} 발급 (호출자가 직접 close 책임)</li>
 *   <li>최초 부팅 시 {@link SchemaInitializer} 호출로 스키마 초기화</li>
 * </ul>
 *
 * <p>주의: SQLite 는 동시 쓰기에 약하므로 본 프로젝트에서는
 * 매번 짧은 Connection 을 열고 닫는 패턴(connection-per-call)을 사용한다.
 */
public final class DatabaseManager {

    private static DatabaseManager instance;
    private boolean initialized = false;

    private DatabaseManager() {
        try {
            // SQLite JDBC 드라이버는 SPI 로 자동 로딩되지만 안전을 위해 명시적 로딩
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

    /**
     * 최초 1회 호출 — 데이터 디렉터리 확인 및 스키마 생성.
     * App.java 의 main 에서 호출하는 것을 권장한다.
     */
    public synchronized void initialize() {
        if (initialized) return;

        // data 디렉터리 보장
        File dir = new File(Constants.DB_DIRECTORY);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("DB 디렉터리 생성 실패: " + dir.getAbsolutePath());
        }

        // 스키마 초기화
        SchemaInitializer initializer = new SchemaInitializer(this);
        initializer.initializeAllTables();

        initialized = true;
        System.out.println("[DatabaseManager] 초기화 완료 → " + new File(Constants.DB_DIRECTORY, Constants.DB_FILE_NAME).getAbsolutePath());
    }

    /**
     * 새 Connection 발급. 외래키 제약을 활성화한 상태로 반환한다.
     * 호출자는 try-with-resources 등으로 반드시 닫아야 한다.
     */
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

    /** 단순 백업: 현재 DB 파일을 지정된 경로로 복사. */
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
}
