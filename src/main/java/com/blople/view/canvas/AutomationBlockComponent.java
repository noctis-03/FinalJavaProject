package com.blople.view.canvas;

import com.blople.model.automation.AutomationRule;
import com.blople.model.automation.OpenBlockRef;
import com.blople.model.block.AutomationBlock;
import com.blople.model.enums.BlockType;
import com.blople.view.calendar.BlockRenderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * 캔버스 위 자동화(실행) 블록 (FR-12, FR-13) — 노드 그래프 방식.
 *
 * <p>내부에 "▶ 실행" 버튼 영역을 가지며, 우측 가운데에 출력 포트(●)를 둔다.
 * 포트는 평소 숨기고, 사용자가 포트를 드래그하는 동안에만 패널이 표시한다.
 */
public class AutomationBlockComponent extends CanvasBlockComponent {

    private static final Color AUTO_BG    = new Color(0xF1ECFB);
    private static final Color ACCENT     = new Color(0x9C6BFF);
    private static final Color RUN_BG     = new Color(0x9C6BFF);
    private static final Color PORT_FILL  = new Color(0x9C6BFF);
    private static final Color PORT_RING  = Color.WHITE;

    /** 실행 버튼 높이(world 단위). */
    public static final double RUN_BTN_H = 28;
    /** 출력 포트 반지름(screen px) — 작게. */
    public static final int PORT_R = 5;

    private final AutomationBlock block;
    private AutomationRule rule;

    public AutomationBlockComponent(AutomationBlock block, AutomationRule rule,
                                    double x, double y, double w, double h) {
        super(block.getId(), x, y, w, h);
        this.block = block;
        this.rule = rule;
    }

    public AutomationBlock getBlock()      { return block; }
    public AutomationRule  getRule()       { return rule; }
    public void setRule(AutomationRule r)  { this.rule = r; }

    public String getRuleId()              { return block.getRuleId(); }

    public List<String> getConnectedBlockIds() {
        List<String> ids = new ArrayList<>();
        if (rule != null) {
            for (OpenBlockRef ref : rule.getOpenBlocks()) ids.add(ref.getBlockId());
        }
        return ids;
    }

    // ===== 실행 버튼 영역 (world 좌표) =====
    public double runBtnWorldX() { return worldX + 8; }
    public double runBtnWorldY() { return worldY + worldH - RUN_BTN_H - 6; }
    public double runBtnWorldW() { return worldW - 16; }
    public double runBtnWorldH() { return RUN_BTN_H; }

    public boolean inRunButton(CanvasViewport vp, int px, int py) {
        Point tl = vp.worldToScreen(runBtnWorldX(), runBtnWorldY());
        Point br = vp.worldToScreen(runBtnWorldX() + runBtnWorldW(),
                runBtnWorldY() + runBtnWorldH());
        return px >= tl.x && px <= br.x && py >= tl.y && py <= br.y;
    }

    // ===== 출력 포트 (우측 가운데) =====
    public Point portCenterScreen(CanvasViewport vp) {
        return vp.worldToScreen(worldX + worldW, worldY + worldH / 2);
    }

    public boolean inPort(CanvasViewport vp, int px, int py) {
        Point c = portCenterScreen(vp);
        int dx = px - c.x, dy = py - c.y;
        int r = PORT_R + 4; // 클릭 잡기 쉽게 약간 넉넉히
        return dx * dx + dy * dy <= r * r;
    }

    @Override
    protected Color bodyColor() { return AUTO_BG; }

    @Override
    protected String titleText() {
        int n = (rule == null) ? 0 : rule.getOpenBlocks().size();
        return "⚡ 실행 (" + n + ")";
    }

    @Override
    protected void paintBody(Graphics2D g2, int sx, int sy, int sw, int sh) {
        g2.setColor(BlockRenderer.parseColor(block.getColor(), ACCENT));
        g2.fillRect(sx + 1, sy + 1, 4, sh - 2);

        List<OpenBlockRef> refs = (rule == null) ? List.of() : rule.getOpenBlocks();

        int runTopScreenY = (int) Math.round(
                sy + (runBtnWorldY() - worldY) / worldH * sh);

        if (refs.isEmpty()) {
            if (sh >= 30) {
                g2.setColor(new Color(0x999999));
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2.drawString(clip(g2, "포트(●)를 끌어 연결", sw - 14), sx + 10, sy + 34);
            }
        } else {
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            int lineH = 15;
            int y = sy + 34;
            int shown = 0;
            for (OpenBlockRef ref : refs) {
                if (y > runTopScreenY - 6) break;
                g2.setColor(new Color(0x555555));
                String icon = (ref.getBlockType() == BlockType.LINK) ? "🔗 " : "📄 ";
                g2.drawString(clip(g2, icon + safe(ref.getDisplayName()), sw - 14), sx + 10, y);
                y += lineH;
                shown++;
            }
            int extra = refs.size() - shown;
            if (extra > 0 && y <= runTopScreenY - 6) {
                g2.setColor(new Color(0x999999));
                g2.drawString("+" + extra + "개 더", sx + 10, y);
            }
        }

        // ▶ 실행 버튼 (블록이 너무 낮으면 생략)
        if (sh >= 40) {
            Point rtl = vp_worldToLocal(sx, sy, sw, sh, runBtnWorldX(), runBtnWorldY());
            int rw = (int) Math.round(runBtnWorldW() / worldW * sw);
            int rh = (int) Math.round(runBtnWorldH() / worldH * sh);
            if (rw > 0 && rh > 0) {
                g2.setColor(RUN_BG);
                g2.fillRoundRect(rtl.x, rtl.y, rw, rh, 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                String label = "▶ 실행";
                int tw = g2.getFontMetrics().stringWidth(label);
                if (tw <= rw - 6) g2.drawString(label, rtl.x + (rw - tw) / 2, rtl.y + rh / 2 + 4);
            }
        }
    }

    /** world 좌표를 현재 그려지는 screen 사각형 기준 로컬 좌표로 환산. */
    private Point vp_worldToLocal(int sx, int sy, int sw, int sh, double wx, double wy) {
        int lx = (int) Math.round(sx + (wx - worldX) / worldW * sw);
        int ly = (int) Math.round(sy + (wy - worldY) / worldH * sh);
        return new Point(lx, ly);
    }

    /** 출력 포트(●)를 그린다. 포트 드래그 중일 때만 보이게 한다. */
    public void paintPort(Graphics2D g2, CanvasViewport vp, boolean visible) {
        if (!visible) return;
        Point c = portCenterScreen(vp);
        g2.setColor(PORT_FILL);
        g2.fillOval(c.x - PORT_R, c.y - PORT_R, PORT_R * 2, PORT_R * 2);
        g2.setColor(PORT_RING);
        g2.setStroke(new java.awt.BasicStroke(1.5f));
        g2.drawOval(c.x - PORT_R, c.y - PORT_R, PORT_R * 2, PORT_R * 2);
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "(이름 없음)" : s;
    }
}
