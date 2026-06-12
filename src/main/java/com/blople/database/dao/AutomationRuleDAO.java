package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.database.TransactionManager;
import com.blople.model.automation.AutomationRule;
import com.blople.model.automation.OpenBlockRef;
import com.blople.model.enums.BlockType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 자동화 규칙 DAO (FR-12) — 과목 페이지 자동화(OPEN_BLOCKS) 전용 처리.
 *
 * <p>설계 스키마는 rule → command → slot → slot_open_blocks 4단이지만,
 * 과목 자동화는 규칙당 command/slot 이 각각 1개로 고정이므로
 * 본 DAO 가 중간 단계(command/slot)를 자동으로 1개씩 만들어 끼우고,
 * 로드 시 다시 풀어 {@link AutomationRule#getOpenBlocks()} 로 합쳐준다.
 *
 * <p>여러 테이블에 걸치므로 {@link TransactionManager} 로 원자성을 보장한다.
 */
public class AutomationRuleDAO extends BaseDAO<AutomationRule, String> {

    private final TransactionManager txManager;

    public AutomationRuleDAO(DatabaseManager dbManager, TransactionManager txManager) {
        super(dbManager);
        this.txManager = txManager;
    }

    @Override protected String getTableName()    { return "automation_rule"; }
    @Override protected String getIdColumnName() { return "id"; }

    @Override
    protected AutomationRule mapRow(ResultSet rs) throws SQLException {
        return AutomationRule.reconstruct(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("context"),
                rs.getString("subject_id"),
                rs.getInt("is_active") == 1,
                rs.getString("created_at"));
    }

    // ============================================================
    //  저장 — rule + command(1) + slot(1) + open_blocks(N)
    // ============================================================
    public void insert(AutomationRule rule) {
        txManager.runInTransaction(conn -> {
            insertRuleRow(conn, rule);
            String commandId = insertCommandRow(conn, rule.getId());
            String slotId    = insertSlotRow(conn, commandId);
            for (OpenBlockRef ref : rule.getOpenBlocks()) {
                insertOpenBlockRow(conn, slotId, ref);
            }
        });
    }

    /** 규칙 이름/활성 상태만 갱신 (open_blocks 는 add/remove 메서드로 따로 처리). */
    public void updateMeta(AutomationRule rule) {
        executeUpdate("UPDATE automation_rule SET name = ?, is_active = ? WHERE id = ?",
                rule.getName(), rule.isActive() ? 1 : 0, rule.getId());
    }

    public boolean deleteById(String ruleId) {
        // automation_command/slot/open_blocks 는 CASCADE 로 함께 삭제됨
        return executeUpdate("DELETE FROM automation_rule WHERE id = ?", ruleId) > 0;
    }

    // ============================================================
    //  open_blocks 개별 추가/삭제 (드롭으로 담기 / 제거)
    // ============================================================

    /** 규칙에 열 블록 1건 추가. 규칙의 slot 을 찾아(없으면 생성) 거기에 매단다. */
    public void addOpenBlock(String ruleId, OpenBlockRef ref) {
        txManager.runInTransaction(conn -> {
            String slotId = ensureSlotId(conn, ruleId);
            // 중복 방지
            if (!openBlockExists(conn, slotId, ref.getBlockId())) {
                insertOpenBlockRow(conn, slotId, ref);
            }
        });
    }

    /** 규칙에서 특정 블록을 열 목록에서 제거. */
    public void removeOpenBlock(String ruleId, String blockId) {
        txManager.runInTransaction(conn -> {
            Optional<String> slotId = findSlotId(conn, ruleId);
            if (slotId.isEmpty()) return;
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM slot_open_blocks WHERE slot_id = ? AND block_id = ?")) {
                ps.setString(1, slotId.get());
                ps.setString(2, blockId);
                ps.executeUpdate();
            }
        });
    }

    // ============================================================
    //  로드 — open_blocks 까지 채워서 반환
    // ============================================================
    @Override
    public Optional<AutomationRule> findById(String ruleId) {
        Optional<AutomationRule> opt = querySingle(
                "SELECT * FROM automation_rule WHERE id = ?", ruleId);
        opt.ifPresent(this::fillOpenBlocks);
        return opt;
    }

    public List<AutomationRule> findBySubject(String subjectId) {
        List<AutomationRule> rules = queryList(
                "SELECT * FROM automation_rule WHERE subject_id = ? AND context = 'SUBJECT' " +
                        "ORDER BY created_at ASC", subjectId);
        rules.forEach(this::fillOpenBlocks);
        return rules;
    }

    /** 규칙의 slot → open_blocks 를 읽어(block JOIN 으로 종류·이름 포함) 모델에 채운다. */
    private void fillOpenBlocks(AutomationRule rule) {
        rule.getOpenBlocks().clear();
        String sql = """
            SELECT sob.id AS sob_id, sob.block_id, b.block_type, b.name
              FROM automation_command ac
              JOIN command_slot cs       ON cs.command_id = ac.id
              JOIN slot_open_blocks sob  ON sob.slot_id   = cs.id
              JOIN block b               ON b.id          = sob.block_id
             WHERE ac.rule_id = ?
             ORDER BY b.name ASC
            """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rule.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String blockId = rs.getString("block_id");
                    BlockType type = parseType(rs.getString("block_type"));
                    String name    = rs.getString("name");
                    rule.addOpenBlock(new OpenBlockRef(
                            rs.getString("sob_id"), blockId, type, name));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("open_blocks 로드 실패: " + e.getMessage(), e);
        }
    }

    // ============================================================
    //  내부 — 행 삽입/조회 헬퍼
    // ============================================================
    private void insertRuleRow(Connection conn, AutomationRule r) throws SQLException {
        String sql = """
            INSERT INTO automation_rule (id, name, context, subject_id, is_active, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.setString(2, r.getName());
            ps.setString(3, r.getContext());
            ps.setString(4, r.getSubjectId());
            ps.setInt   (5, r.isActive() ? 1 : 0);
            ps.setString(6, r.getCreatedAt());
            ps.executeUpdate();
        }
    }

    private String insertCommandRow(Connection conn, String ruleId) throws SQLException {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO automation_command (id, rule_id, command_type, sort_order) " +
                "VALUES (?, ?, 'OPEN_BLOCKS', 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, ruleId);
            ps.executeUpdate();
        }
        return id;
    }

    private String insertSlotRow(Connection conn, String commandId) throws SQLException {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO command_slot (id, command_id, slot_type, slot_order) " +
                "VALUES (?, ?, 'OPEN_BLOCKS', 1)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, commandId);
            ps.executeUpdate();
        }
        return id;
    }

    private void insertOpenBlockRow(Connection conn, String slotId, OpenBlockRef ref) throws SQLException {
        String sql = "INSERT INTO slot_open_blocks (id, slot_id, block_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, slotId);
            ps.setString(3, ref.getBlockId());
            ps.executeUpdate();
        }
    }

    /** 규칙의 slot id 조회 (command → slot). */
    private Optional<String> findSlotId(Connection conn, String ruleId) throws SQLException {
        String sql = """
            SELECT cs.id
              FROM automation_command ac
              JOIN command_slot cs ON cs.command_id = ac.id
             WHERE ac.rule_id = ?
             LIMIT 1
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ruleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getString(1));
                return Optional.empty();
            }
        }
    }

    /** slot 이 없으면 command/slot 을 새로 만들어 보장. */
    private String ensureSlotId(Connection conn, String ruleId) throws SQLException {
        Optional<String> existing = findSlotId(conn, ruleId);
        if (existing.isPresent()) return existing.get();
        String commandId = insertCommandRow(conn, ruleId);
        return insertSlotRow(conn, commandId);
    }

    private boolean openBlockExists(Connection conn, String slotId, String blockId) throws SQLException {
        String sql = "SELECT 1 FROM slot_open_blocks WHERE slot_id = ? AND block_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slotId);
            ps.setString(2, blockId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static BlockType parseType(String s) {
        if (s == null) return null;
        try { return BlockType.valueOf(s); }
        catch (IllegalArgumentException e) { return null; }
    }

    @Override
    protected List<AutomationRule> queryList(String sql, Object... params) {
        return super.queryList(sql, params);
    }

    @Override
    protected Optional<AutomationRule> querySingle(String sql, Object... params) {
        return super.querySingle(sql, params);
    }
}
