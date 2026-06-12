package com.blople.service;

import com.blople.common.EventBus;
import com.blople.database.dao.AutomationBlockDAO;
import com.blople.database.dao.AutomationRuleDAO;
import com.blople.database.dao.CanvasStateDAO;
import com.blople.database.dao.ExecutionLogDAO;
import com.blople.database.dao.FileBlockDAO;
import com.blople.database.dao.LinkBlockDAO;
import com.blople.model.automation.AutomationRule;
import com.blople.model.automation.ExecutionLog;
import com.blople.model.automation.OpenBlockRef;
import com.blople.model.block.AutomationBlock;
import com.blople.model.block.FileBlock;
import com.blople.model.block.LinkBlock;
import com.blople.model.canvas.CanvasState;
import com.blople.model.enums.BlockType;

import java.util.List;
import java.util.Optional;

/**
 * 과목 페이지 자동화 서비스 (FR-12, FR-13).
 *
 * <p>캔버스의 "실행 블록" 하나 = 자동화 블록(캔버스 표현) + 자동화 규칙(열 블록 묶음).
 * 책임:
 * <ul>
 *   <li>실행 블록 생성/삭제 (블록 + 규칙 + 캔버스 배치를 함께 관리)</li>
 *   <li>실행 블록에 파일/링크 블록 연결·해제 (드롭으로 담기)</li>
 *   <li>실행 — 연결된 블록들을 순서대로 열고 결과를 execution_log 에 기록</li>
 * </ul>
 */
public class AutomationService {

    public static final double AUTO_DEFAULT_W = 200;
    public static final double AUTO_DEFAULT_H = 120;

    private final AutomationBlockDAO automationBlockDAO;
    private final AutomationRuleDAO  automationRuleDAO;
    private final ExecutionLogDAO    executionLogDAO;
    private final CanvasStateDAO     canvasStateDAO;
    private final FileBlockDAO       fileBlockDAO;
    private final LinkBlockDAO       linkBlockDAO;
    private final EventBus eventBus = EventBus.getInstance();

    public AutomationService(AutomationBlockDAO automationBlockDAO,
                             AutomationRuleDAO automationRuleDAO,
                             ExecutionLogDAO executionLogDAO,
                             CanvasStateDAO canvasStateDAO,
                             FileBlockDAO fileBlockDAO,
                             LinkBlockDAO linkBlockDAO) {
        this.automationBlockDAO = automationBlockDAO;
        this.automationRuleDAO  = automationRuleDAO;
        this.executionLogDAO    = executionLogDAO;
        this.canvasStateDAO     = canvasStateDAO;
        this.fileBlockDAO       = fileBlockDAO;
        this.linkBlockDAO       = linkBlockDAO;
    }

    // ===== 로드 =====

    /** 한 과목 캔버스의 자동화 블록 + 배치 + 규칙을 함께 조회. */
    public List<Placed> loadAutomationBlocks(String subjectId) {
        List<AutomationBlock> blocks = automationBlockDAO.findBySubject(subjectId);
        List<Placed> result = new java.util.ArrayList<>();
        for (AutomationBlock b : blocks) {
            CanvasState st = canvasStateDAO.findByBlock(b.getId())
                    .orElseGet(() -> new CanvasState(subjectId, b.getId(),
                            40, 40, AUTO_DEFAULT_W, AUTO_DEFAULT_H));
            AutomationRule rule = automationRuleDAO.findById(b.getRuleId()).orElse(null);
            result.add(new Placed(b, rule, st));
        }
        return result;
    }

    public Optional<AutomationRule> getRule(String ruleId) {
        return automationRuleDAO.findById(ruleId);
    }

    // ===== 생성 =====

    /** 새 실행 블록 생성 — 빈 규칙 + 자동화 블록 + 캔버스 배치. */
    public Placed createAutomationBlock(String subjectId, double worldX, double worldY) {
        AutomationRule rule = new AutomationRule("실행", subjectId);
        automationRuleDAO.insert(rule); // open_blocks 0개 (command/slot 도 아직 없음)

        AutomationBlock block = new AutomationBlock("⚡ 실행", "#9C6BFF", rule.getId());
        automationBlockDAO.insert(block, subjectId);

        CanvasState st = new CanvasState(subjectId, block.getId(),
                worldX, worldY, AUTO_DEFAULT_W, AUTO_DEFAULT_H);
        canvasStateDAO.insert(st);

        eventBus.publish(EventBus.AUTOMATION_EXECUTED, "automation-created");
        return new Placed(block, rule, st);
    }

    // ===== 블록 연결/해제 =====

    /**
     * 실행 블록(rule)에 파일/링크 블록을 연결.
     * @param targetBlockId 연결할 파일/링크 블록 id
     * @param type          FILE / LINK
     * @return 추가된 참조 (이미 있거나 미지원 종류면 빈 Optional)
     */
    public Optional<OpenBlockRef> connectBlock(String ruleId, String targetBlockId, BlockType type) {
        String displayName = resolveDisplayName(targetBlockId, type);
        if (displayName == null) return Optional.empty(); // 대상 블록 없음/미지원

        OpenBlockRef ref = new OpenBlockRef(targetBlockId, type, displayName);
        automationRuleDAO.addOpenBlock(ruleId, ref);
        eventBus.publish(EventBus.AUTOMATION_EXECUTED, "automation-connected");
        return Optional.of(ref);
    }

    public void disconnectBlock(String ruleId, String targetBlockId) {
        automationRuleDAO.removeOpenBlock(ruleId, targetBlockId);
        eventBus.publish(EventBus.AUTOMATION_EXECUTED, "automation-disconnected");
    }

    private String resolveDisplayName(String blockId, BlockType type) {
        return switch (type) {
            case FILE -> fileBlockDAO.findById(blockId).map(FileBlock::getFileName).orElse(null);
            case LINK -> linkBlockDAO.findById(blockId).map(LinkBlock::getDisplayName).orElse(null);
            default   -> null;
        };
    }

    // ===== 삭제 =====

    /** 실행 블록 제거 — 자동화 블록 + 규칙 + 캔버스 배치를 모두 정리. */
    public void deleteAutomationBlock(String automationBlockId) {
        Optional<AutomationBlock> opt = automationBlockDAO.findById(automationBlockId);
        if (opt.isEmpty()) return;
        String ruleId = opt.get().getRuleId();

        canvasStateDAO.deleteByBlock(automationBlockId);
        automationBlockDAO.deleteById(automationBlockId); // block 행 삭제(automation_block CASCADE)
        automationRuleDAO.deleteById(ruleId);             // command/slot/open_blocks CASCADE

        eventBus.publish(EventBus.AUTOMATION_EXECUTED, "automation-deleted");
    }

    // ===== 실행 =====

    /**
     * 규칙에 연결된 블록들을 순서대로 연다.
     * 일부 실패해도 끝까지 시도하며, 결과를 execution_log 에 기록한다.
     *
     * @return 실행 이력 (성공/부분실패 메시지 포함)
     */
    public ExecutionLog execute(String ruleId) {
        Optional<AutomationRule> opt = automationRuleDAO.findById(ruleId);
        if (opt.isEmpty()) {
            ExecutionLog log = new ExecutionLog(ruleId, ExecutionLog.STATUS_FAILED, "규칙을 찾을 수 없음");
            executionLogDAO.insert(log);
            return log;
        }
        AutomationRule rule = opt.get();
        List<OpenBlockRef> refs = rule.getOpenBlocks();

        if (refs.isEmpty()) {
            ExecutionLog log = new ExecutionLog(ruleId, ExecutionLog.STATUS_FAILED, "연결된 블록이 없습니다.");
            executionLogDAO.insert(log);
            eventBus.publish(EventBus.AUTOMATION_EXECUTED, log);
            return log;
        }

        int ok = 0;
        int fail = 0;
        StringBuilder msg = new StringBuilder();

        for (OpenBlockRef ref : refs) {
            try {
                openOne(ref);
                ok++;
            } catch (Exception e) {
                fail++;
                msg.append("[실패] ").append(ref.getDisplayName())
                        .append(": ").append(e.getMessage()).append("\n");
            }
        }

        String status = (fail == 0) ? ExecutionLog.STATUS_SUCCESS : ExecutionLog.STATUS_FAILED;
        String summary = "%d개 열기 성공%s".formatted(ok, fail > 0 ? (", " + fail + "개 실패") : "");
        if (msg.length() > 0) summary += "\n" + msg;

        ExecutionLog log = new ExecutionLog(ruleId, status, summary);
        executionLogDAO.insert(log);
        eventBus.publish(EventBus.AUTOMATION_EXECUTED, log);
        return log;
    }

    /** 참조 한 건을 실제로 연다 (파일/링크 분기). */
    private void openOne(OpenBlockRef ref) throws Exception {
        switch (ref.getBlockType()) {
            case FILE -> {
                FileBlock f = fileBlockDAO.findById(ref.getBlockId())
                        .orElseThrow(() -> new IllegalStateException("파일 블록이 삭제됨"));
                f.openFile();
            }
            case LINK -> {
                LinkBlock l = linkBlockDAO.findById(ref.getBlockId())
                        .orElseThrow(() -> new IllegalStateException("링크 블록이 삭제됨"));
                l.openInBrowser();
            }
            default -> throw new IllegalStateException("지원하지 않는 블록 종류: " + ref.getBlockType());
        }
    }

    public List<ExecutionLog> getHistory(String ruleId) {
        return executionLogDAO.findByRule(ruleId);
    }

    /** 자동화 블록 + 규칙 + 배치 묶음. */
    public record Placed(AutomationBlock block, AutomationRule rule, CanvasState state) {}
}
