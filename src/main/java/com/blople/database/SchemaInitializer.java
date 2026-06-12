package com.blople.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 전체 테이블 DDL 실행기.
 *
 * <p>설계 문서 3.3 "데이터베이스 이용 시 필요한 필드 이름 정리" 의 스키마를 그대로 옮겼다.
 * 테이블이 이미 존재하면 건너뛴다 (CREATE TABLE IF NOT EXISTS).
 *
 * <p>참고:
 * <ul>
 *   <li>SQLite 는 TIMESTAMP/DATETIME 을 TEXT 로 저장 (ISO-8601 권장)</li>
 *   <li>BOOLEAN 은 INTEGER 0/1 로 저장</li>
 *   <li>모든 PK 는 TEXT(UUID) 사용</li>
 * </ul>
 */
public class SchemaInitializer {

    private final DatabaseManager dbManager;

    public SchemaInitializer(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void initializeAllTables() {
        List<String> ddls = buildDdlList();
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);
            for (String ddl : ddls) {
                stmt.execute(ddl);
            }
            conn.commit();
            System.out.println("[SchemaInitializer] " + ddls.size() + "개 테이블 DDL 실행 완료");
        } catch (SQLException e) {
            throw new RuntimeException("스키마 초기화 실패", e);
        }
    }

    public boolean isSchemaInitialized() {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='block'";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    // ============================================================
    //  DDL 정의 — 설계 문서 3.3 절 기준
    // ============================================================
    private List<String> buildDdlList() {
        List<String> ddls = new ArrayList<>();

        // ---------- 1. 기본 구조 ----------
        ddls.add("""
            CREATE TABLE IF NOT EXISTS semester (
                id          TEXT PRIMARY KEY,
                name        TEXT NOT NULL,
                start_date  TEXT,
                end_date    TEXT,
                is_active   INTEGER DEFAULT 0,
                created_at  TEXT DEFAULT (datetime('now'))
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS subject (
                id           TEXT PRIMARY KEY,
                semester_id  TEXT NOT NULL,
                name         TEXT NOT NULL,
                professor    TEXT,
                classroom    TEXT,
                color        TEXT NOT NULL,
                sort_order   INTEGER DEFAULT 0,
                created_at   TEXT DEFAULT (datetime('now')),
                FOREIGN KEY (semester_id) REFERENCES semester(id) ON DELETE CASCADE
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS timetable_entry (
                id           TEXT PRIMARY KEY,
                subject_id   TEXT NOT NULL,
                day_of_week  INTEGER NOT NULL,
                start_time   TEXT NOT NULL,
                end_time     TEXT NOT NULL,
                FOREIGN KEY (subject_id) REFERENCES subject(id) ON DELETE CASCADE
            )
            """);

        // ---------- 2. 블록 시스템 ----------
        ddls.add("""
            CREATE TABLE IF NOT EXISTS block (
                id          TEXT PRIMARY KEY,
                block_type  TEXT NOT NULL,
                name        TEXT NOT NULL,
                color       TEXT,
                memo        TEXT,
                created_at  TEXT DEFAULT (datetime('now')),
                updated_at  TEXT DEFAULT (datetime('now'))
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS schedule_block (
                block_id         TEXT PRIMARY KEY,
                start_datetime   TEXT NOT NULL,
                end_datetime     TEXT NOT NULL,
                subject_id       TEXT,
                is_recurring     INTEGER DEFAULT 0,
                recurrence_rule  TEXT,
                FOREIGN KEY (block_id)   REFERENCES block(id)   ON DELETE CASCADE,
                FOREIGN KEY (subject_id) REFERENCES subject(id) ON DELETE SET NULL
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS preset_block (
                block_id          TEXT PRIMARY KEY,
                duration_minutes  INTEGER NOT NULL,
                category          TEXT,
                FOREIGN KEY (block_id) REFERENCES block(id) ON DELETE CASCADE
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS preset_block_instance (
                id               TEXT PRIMARY KEY,
                preset_block_id  TEXT NOT NULL,
                start_datetime   TEXT NOT NULL,
                end_datetime     TEXT NOT NULL,
                FOREIGN KEY (preset_block_id) REFERENCES preset_block(block_id) ON DELETE CASCADE
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS assignment (
                block_id         TEXT PRIMARY KEY,
                subject_id       TEXT NOT NULL,
                deadline         TEXT NOT NULL,
                description      TEXT,
                score_weight     TEXT,
                submit_method    TEXT,
                status           TEXT DEFAULT 'NOT_STARTED',
                work_date        TEXT,
                work_start_time  TEXT,
                work_end_time    TEXT,
                FOREIGN KEY (block_id)   REFERENCES block(id)   ON DELETE CASCADE,
                FOREIGN KEY (subject_id) REFERENCES subject(id) ON DELETE CASCADE
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS assignment_attachment (
                id             TEXT PRIMARY KEY,
                assignment_id  TEXT NOT NULL,
                file_path      TEXT NOT NULL,
                file_name      TEXT NOT NULL,
                FOREIGN KEY (assignment_id) REFERENCES assignment(block_id) ON DELETE CASCADE
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS file_block (
                block_id    TEXT PRIMARY KEY,
                subject_id  TEXT NOT NULL,
                file_path   TEXT NOT NULL,
                file_name   TEXT NOT NULL,
                file_type   TEXT,
                file_size   INTEGER,
                FOREIGN KEY (block_id)   REFERENCES block(id)   ON DELETE CASCADE,
                FOREIGN KEY (subject_id) REFERENCES subject(id) ON DELETE CASCADE
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS text_block (
                block_id          TEXT PRIMARY KEY,
                subject_id        TEXT,
                title             TEXT,
                content           TEXT,
                background_color  TEXT DEFAULT '#FFFF00',
                FOREIGN KEY (block_id)   REFERENCES block(id)   ON DELETE CASCADE,
                FOREIGN KEY (subject_id) REFERENCES subject(id) ON DELETE SET NULL
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS link_block (
                block_id      TEXT PRIMARY KEY,
                subject_id    TEXT,
                url           TEXT NOT NULL,
                display_name  TEXT,
                link_type     TEXT,
                FOREIGN KEY (block_id)   REFERENCES block(id)   ON DELETE CASCADE,
                FOREIGN KEY (subject_id) REFERENCES subject(id) ON DELETE SET NULL
            )
            """);

        // ---------- 3. 태그 시스템 ----------
        ddls.add("""
            CREATE TABLE IF NOT EXISTS tag (
                id    TEXT PRIMARY KEY,
                name  TEXT NOT NULL UNIQUE
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS block_tag (
                block_id  TEXT NOT NULL,
                tag_id    TEXT NOT NULL,
                PRIMARY KEY (block_id, tag_id),
                FOREIGN KEY (block_id) REFERENCES block(id) ON DELETE CASCADE,
                FOREIGN KEY (tag_id)   REFERENCES tag(id)   ON DELETE CASCADE
            )
            """);

        // ---------- 4. 캔버스 상태 ----------
        ddls.add("""
            CREATE TABLE IF NOT EXISTS canvas_state (
                id          TEXT PRIMARY KEY,
                subject_id  TEXT NOT NULL,
                block_id    TEXT NOT NULL,
                pos_x       REAL NOT NULL,
                pos_y       REAL NOT NULL,
                width       REAL NOT NULL,
                height      REAL NOT NULL,
                z_order     INTEGER DEFAULT 0,
                FOREIGN KEY (subject_id) REFERENCES subject(id) ON DELETE CASCADE,
                FOREIGN KEY (block_id)   REFERENCES block(id)   ON DELETE CASCADE
            )
            """);

        // ---------- 5. 자동화 시스템 ----------
        ddls.add("""
            CREATE TABLE IF NOT EXISTS automation_rule (
                id          TEXT PRIMARY KEY,
                name        TEXT,
                context     TEXT NOT NULL,
                subject_id  TEXT,
                is_active   INTEGER DEFAULT 1,
                created_at  TEXT DEFAULT (datetime('now')),
                FOREIGN KEY (subject_id) REFERENCES subject(id) ON DELETE CASCADE
            )
            """);

        // 캔버스 위 자동화(실행) 블록 (FR-12, FR-13)
        ddls.add("""
            CREATE TABLE IF NOT EXISTS automation_block (
                block_id    TEXT PRIMARY KEY,
                subject_id  TEXT,
                rule_id     TEXT NOT NULL,
                FOREIGN KEY (block_id)   REFERENCES block(id)            ON DELETE CASCADE,
                FOREIGN KEY (subject_id) REFERENCES subject(id)          ON DELETE CASCADE,
                FOREIGN KEY (rule_id)    REFERENCES automation_rule(id)  ON DELETE CASCADE
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS automation_command (
                id            TEXT PRIMARY KEY,
                rule_id       TEXT NOT NULL,
                command_type  TEXT NOT NULL,
                sort_order    INTEGER DEFAULT 0,
                FOREIGN KEY (rule_id) REFERENCES automation_rule(id) ON DELETE CASCADE
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS command_slot (
                id          TEXT PRIMARY KEY,
                command_id  TEXT NOT NULL,
                slot_type   TEXT NOT NULL,
                slot_order  INTEGER NOT NULL,
                FOREIGN KEY (command_id) REFERENCES automation_command(id) ON DELETE CASCADE
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS slot_next_week (
                slot_id      TEXT PRIMARY KEY,
                week_offset  INTEGER DEFAULT 1,
                FOREIGN KEY (slot_id) REFERENCES command_slot(id) ON DELETE CASCADE
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS slot_day_of_week (
                id           TEXT PRIMARY KEY,
                slot_id      TEXT NOT NULL,
                day_of_week  INTEGER NOT NULL,
                FOREIGN KEY (slot_id) REFERENCES command_slot(id) ON DELETE CASCADE
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS slot_preset_block (
                slot_id          TEXT PRIMARY KEY,
                preset_block_id  TEXT NOT NULL,
                FOREIGN KEY (slot_id)         REFERENCES command_slot(id)        ON DELETE CASCADE,
                FOREIGN KEY (preset_block_id) REFERENCES preset_block(block_id)  ON DELETE CASCADE
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS slot_open_blocks (
                id        TEXT PRIMARY KEY,
                slot_id   TEXT NOT NULL,
                block_id  TEXT NOT NULL,
                FOREIGN KEY (slot_id)  REFERENCES command_slot(id) ON DELETE CASCADE,
                FOREIGN KEY (block_id) REFERENCES block(id)        ON DELETE CASCADE
            )
            """);

        ddls.add("""
            CREATE TABLE IF NOT EXISTS execution_log (
                id              TEXT PRIMARY KEY,
                rule_id         TEXT NOT NULL,
                executed_at     TEXT DEFAULT (datetime('now')),
                status          TEXT NOT NULL,
                result_message  TEXT,
                FOREIGN KEY (rule_id) REFERENCES automation_rule(id) ON DELETE CASCADE
            )
            """);

        // ---------- 6. 정렬 상태 ----------
        ddls.add("""
            CREATE TABLE IF NOT EXISTS sort_preference (
                id              TEXT PRIMARY KEY,
                subject_id      TEXT NOT NULL,
                sort_field      TEXT NOT NULL,
                sort_direction  TEXT DEFAULT 'ASC',
                FOREIGN KEY (subject_id) REFERENCES subject(id) ON DELETE CASCADE
            )
            """);

        // ---------- 인덱스 (성능) ----------
        ddls.add("CREATE INDEX IF NOT EXISTS idx_subject_semester    ON subject(semester_id)");
        ddls.add("CREATE INDEX IF NOT EXISTS idx_schedule_subject    ON schedule_block(subject_id)");
        ddls.add("CREATE INDEX IF NOT EXISTS idx_schedule_start      ON schedule_block(start_datetime)");
        ddls.add("CREATE INDEX IF NOT EXISTS idx_assignment_subject  ON assignment(subject_id)");
        ddls.add("CREATE INDEX IF NOT EXISTS idx_assignment_deadline ON assignment(deadline)");
        ddls.add("CREATE INDEX IF NOT EXISTS idx_file_subject        ON file_block(subject_id)");
        ddls.add("CREATE INDEX IF NOT EXISTS idx_canvas_subject      ON canvas_state(subject_id)");
        ddls.add("CREATE INDEX IF NOT EXISTS idx_block_type          ON block(block_type)");
        ddls.add("CREATE INDEX IF NOT EXISTS idx_automation_block_subject ON automation_block(subject_id)");
        ddls.add("CREATE INDEX IF NOT EXISTS idx_slot_open_blocks_slot    ON slot_open_blocks(slot_id)");

        return ddls;
    }
}
