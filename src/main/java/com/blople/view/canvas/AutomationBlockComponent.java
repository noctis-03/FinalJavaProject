package com.blople.view.canvas;

import com.blople.model.automation.AutomationRule;
import com.blople.model.automation.OpenBlockRef;
import com.blople.model.block.AutomationBlock;
import com.blople.view.calendar.BlockRenderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

/**
 * 캔버스 위 자동화(실행) 블록 (FR-12, FR-13).
 *
 * <p>제목 아래에 "연결된 블록 목록" 을 표시하고, 하단에 실행 안내를 둔다.
 * 파일/링크 블록을 이 블록 위로 드롭하면 목록에 담기고(컨트롤러 처리),
 * 더블클릭하면 담긴 블록들을 한 번에 연다.
 *
 * <p>표시할 연결 목록은 {@link AutomationRule#getOpenBlocks()} 에서 읽으므로,
 * 연결이 바뀌면 컨트롤러가 rule 을 갱신하고 캔버스를 repaint 한다.
 */
public class AutomationBlockComponent extends CanvasBlockComponent {

    private static final Color AUTO_BG = new Color(0xF1ECFB);

    private final AutomationBlock block;
    private AutomationRule rule;     // 연결 목록 표시용 (갱신 가능)

    public AutomationBlockComponent(AutomationBlock block, AutomationRule rule,
                                    double x, double y, double w, double h) {
        super(block.getId(), x, y, w, h);
        this.block = block;
        this.rule = rule;
    }

    public AutomationBlock getBlock()      { return block; }
    public AutomationRule  getRule()       { return rule; }
    public void setRule(AutomationRule r)  { this.rule = r; }

    /** 이 블록이 가리키는 규칙 id. */
    public String getRuleId()              { return block.getRuleId(); }

    @Override
    protected Color bodyColor() {
        return AUTO_BG;
    }

    @Override
    protected String titleText() {
        int n = (rule == null) ? 0 : rule.getOpenBlocks().size();
        return "⚡ 실행 (" + n + ")";
    }

    @Override
    protected void paintBody(Graphics2D g2, int sx, int sy, int sw, int sh) {
        // 좌측 보라색 띠
        g2.setColor(BlockRenderer.parseColor(block.getColor(), new Color(0x9C6BFF)));
        g2.fillRect(sx + 1, sy + 1, 4, sh - 2);

        List<OpenBlockRef> refs = (rule == null) ? List.of() : rule.getOpenBlocks();

        if (refs.isEmpty()) {
            g2.setColor(new Color(0x999999));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.drawString(clip(g2, "파일/링크를 끌어다 넣으세요", sw - 14), sx + 10, sy + 34);
            return;
        }

        // 연결된 블록 목록 (영역 안에서만)
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        int lineH = 15;
        int y = sy + 34;
        int shown = 0;
        for (OpenBlockRef ref : refs) {
            if (y > sy + sh - 16) break; // 하단 안내문 자리 남김
            g2.setColor(new Color(0x555555));
            String icon = (ref.getBlockType() != null && ref.getBlockType().name().equals("LINK"))
                    ? "🔗 " : "📄 ";
            g2.drawString(clip(g2, icon + safe(ref.getDisplayName()), sw - 14), sx + 10, y);
            y += lineH;
            shown++;
        }
        int extra = refs.size() - shown;
        if (extra > 0) {
            g2.setColor(new Color(0x999999));
            g2.drawString("+" + extra + "개 더", sx + 10, y);
        }

        // 하단 실행 안내
        g2.setColor(new Color(0x9C6BFF));
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("더블클릭 = 한 번에 열기", sx + 10, sy + sh - 6);
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "(이름 없음)" : s;
    }
}
