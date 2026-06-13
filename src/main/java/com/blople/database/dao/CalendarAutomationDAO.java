package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.database.TransactionManager;
import com.blople.model.automation.CalendarAutomationRule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 캘린더 자동화 규칙 DAO (FR-12).
 *
 * <p>automation_rule(start_hour 포함) + command(1) + slot(3) 구조를 사용한다.
 * 요일·미리블록·주 오프셋은 슬롯에, 시작 시각은 automation_rule.start_hour 에 저장한다.
 */
public class CalendarAutomationDAO extends BaseDAO<CalendarAutomationRule, String> {

    private final TransactionManager txManager;

    public CalendarAutomationDAO(DatabaseManager dbManager, TransactionManager txManager) {
        super(dbManager);
        this.txManager = txManager;
    }

    @Override protected String getTableName()    { return "automation_rule"; }
    @Override protected String getIdColumnName() { return "id"; }

    @Override
    protected CalendarAutomationRule mapRow(ResultSet rs) throws SQLException {
        int startHour = rs.getInt("start_hour");
        if (rs.wasNull()) startHour = 9;
        return CalendarAutomationRule.reconstruct(
                rs.getString("id"),
                rs.getString("name"),
                rs.getInt("is_active") == 1,
                rs.getString("created_at"),
                startHour);
    }

    // ===== 저장 =====
    public void insert(CalendarAutomationRule rule) {
        txManager.runInTransaction(conn -> {
            insertRuleRow(conn, rule);
            String commandId = insertCommandRow(conn, rule.getId());

            String nwSlot = insertSlotRow(conn, commandId, "NEXT_WEEK", 1);
            insertNextWeek(conn, nwSlot, rule.getWeekOffset());

            String dowSlot = insertSlotRow(conn, commandId, "DAY_OF_WEEK", 2);
            if (rule.getDayOfWeek() >= 0) insertDayOfWeek(conn, dowSlot, rule.getDayOfWeek());

            String presetSlot = insertSlotRow(conn, commandId, "PRESET_BLOCK", 3);
            if (rule.getPresetBlockId() != null) insertPreset(conn, presetSlot, rule.getPresetBlockId());
        });
    }

    public void update(CalendarAutomationRule rule) {
        txManager.runInTransaction(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE automation_rule SET name = ?, is_active = ?, start_hour = ? WHERE id = ?")) {
                ps.setString(1, rule.getName());
                ps.setInt   (2, rule.isActive() ? 1 : 0);
                ps.setInt   (3, rule.getStartHour());
                ps.setString(4, rule.getId());
                ps.executeUpdate();
            }
            deleteCommandsOf(conn, rule.getId());
            String commandId = insertCommandRow(conn, rule.getId());

            String nwSlot = insertSlotRow(conn, commandId, "NEXT_WEEK", 1);
            insertNextWeek(conn, nwSlot, rule.getWeekOffset());

            String dowSlot = insertSlotRow(conn, commandId, "DAY_OF_WEEK", 2);
            if (rule.getDayOfWeek() >= 0) insertDayOfWeek(conn, dowSlot, rule.getDayOfWeek());

            String presetSlot = insertSlotRow(conn, commandId, "PRESET_BLOCK", 3);
            if (rule.getPresetBlockId() != null) insertPreset(conn, presetSlot, rule.getPresetBlockId());
        });
    }

    public boolean deleteById(String ruleId) {
        return executeUpdate("DELETE FROM automation_rule WHERE id = ?", ruleId) > 0;
    }

    // ===== 로드 =====
    @Override
    public Optional<CalendarAutomationRule> findById(String ruleId) {
        Optional<CalendarAutomationRule> opt = querySingle(
                "SELECT * FROM automation_rule WHERE id = ?", ruleId);
        opt.ifPresent(this::fillSlots);
        return opt;
    }

    public List<CalendarAutomationRule> findAllCalendar() {
        List<CalendarAutomationRule> rules = queryList(
                "SELECT * FROM automation_rule WHERE context = 'CALENDAR' ORDER BY created_at ASC");
        rules.forEach(this::fillSlots);
        return rules;
    }

    private void fillSlots(CalendarAutomationRule rule) {
        String sql = """
            SELECT cs.slot_type,
                   nw.week_offset      AS week_offset,
                   dow.day_of_week     AS day_of_week,
                   sp.preset_block_id  AS preset_block_id
              FROM automation_command ac
              JOIN command_slot cs            ON cs.command_id = ac.id
              LEFT JOIN slot_next_week nw     ON nw.slot_id = cs.id
              LEFT JOIN slot_day_of_week dow  ON dow.slot_id = cs.id
              LEFT JOIN slot_preset_block sp  ON sp.slot_id = cs.id
             WHERE ac.rule_id = ?
            """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rule.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("slot_type");
                    switch (type) {
                        case "NEXT_WEEK" -> {
                            int wo = rs.getInt("week_offset");
                            if (!rs.wasNull()) rule.setWeekOffset(wo);
                        }
                        case "DAY_OF_WEEK" -> {
                            int dow = rs.getInt("day_of_week");
                            if (!rs.wasNull()) rule.setDayOfWeek(dow);
                        }
                        case "PRESET_BLOCK" -> {
                            String pid = rs.getString("preset_block_id");
                            if (pid != null) rule.setPresetBlockId(pid);
                        }
                        default -> { }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("캘린더 자동화 슬롯 로드 실패: " + e.getMessage(), e);
        }
    }

    // ===== 내부 헬퍼 =====
    private void insertRuleRow(Connection conn, CalendarAutomationRule r) throws SQLException {
        String sql = """
            INSERT INTO automation_rule (id, name, context, subject_id, start_hour, is_active, created_at)
            VALUES (?, ?, 'CALENDAR', NULL, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.setString(2, r.getName());
            ps.setInt   (3, r.getStartHour());
            ps.setInt   (4, r.isActive() ? 1 : 0);
            ps.setString(5, r.getCreatedAt());
            ps.executeUpdate();
        }
    }

    private String insertCommandRow(Connection conn, String ruleId) throws SQLException {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO automation_command (id, rule_id, command_type, sort_order) " +
                "VALUES (?, ?, 'REGISTER_SCHEDULE', 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, ruleId);
            ps.executeUpdate();
        }
        return id;
    }

    private String insertSlotRow(Connection conn, String commandId,
                                 String slotType, int slotOrder) throws SQLException {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO command_slot (id, command_id, slot_type, slot_order) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, commandId);
            ps.setString(3, slotType);
            ps.setInt   (4, slotOrder);
            ps.executeUpdate();
        }
        return id;
    }

    private void insertNextWeek(Connection conn, String slotId, int weekOffset) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO slot_next_week (slot_id, week_offset) VALUES (?, ?)")) {
            ps.setString(1, slotId);
            ps.setInt   (2, weekOffset);
            ps.executeUpdate();
        }
    }

    private void insertDayOfWeek(Connection conn, String slotId, int dayOfWeek) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO slot_day_of_week (id, slot_id, day_of_week) VALUES (?, ?, ?)")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, slotId);
            ps.setInt   (3, dayOfWeek);
            ps.executeUpdate();
        }
    }

    private void insertPreset(Connection conn, String slotId, String presetBlockId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO slot_preset_block (slot_id, preset_block_id) VALUES (?, ?)")) {
            ps.setString(1, slotId);
            ps.setString(2, presetBlockId);
            ps.executeUpdate();
        }
    }

    private void deleteCommandsOf(Connection conn, String ruleId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM automation_command WHERE rule_id = ?")) {
            ps.setString(1, ruleId);
            ps.executeUpdate();
        }
    }

    @Override
    protected List<CalendarAutomationRule> queryList(String sql, Object... params) {
        return super.queryList(sql, params);
    }

    @Override
    protected Optional<CalendarAutomationRule> querySingle(String sql, Object... params) {
        return super.querySingle(sql, params);
    }
}
