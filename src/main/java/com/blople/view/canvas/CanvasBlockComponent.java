package com.blople.view.canvas;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * 무한 캔버스 위에 놓이는 블록 한 개의 렌더링·히트 단위 (FR-08).
 *
 * <p>위치/크기는 <b>world 좌표</b>로 가지며, 그릴 때 {@link CanvasViewport} 로
 * screen 좌표로 변환된다. 구체 블록은 본 클래스를 상속해
 * {@link #paintBody}/{@link #titleText} 만 다르게 구현한다.
 *
 * <p>노드 그래프 연결 대상이 되는 블록(파일/링크)은 {@link #hasInputPort()} 를
 * true 로 오버라이드한다. 입력 포트(좌측 ●)는 실행 블록의 포트를 드래그하는
 * 동안에만 패널이 표시한다.
 */
public abstract class CanvasBlockComponent {

    /** 리사이즈 핸들 크기(screen px). */
    public static final int HANDLE = 8;

    /** 입력 포트 반지름(screen px) — 작게. */
    public static final int IN_PORT_R = 5;

    /** 블록 최소 world 크기. */
    public static final double MIN_W = 60;
    public static final double MIN_H = 44;

    protected final String blockId;
    protected double worldX, worldY, worldW, worldH;
    protected boolean selected;

    protected CanvasBlockComponent(String blockId,
                                   double worldX, double worldY,
                                   double worldW, double worldH) {
        this.blockId = blockId;
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldW = Math.max(MIN_W, worldW);
        this.worldH = Math.max(MIN_H, worldH);
    }

    public String getBlockId() { return blockId; }

    public double getWorldX() { return worldX; }
    public double getWorldY() { return worldY; }
    public double getWorldW() { return worldW; }
    public double getWorldH() { return worldH; }

    public void setWorldPos(double x, double y) { this.worldX = x; this.worldY = y; }
    public void setWorldSize(double w, double h) {
        this.worldW = Math.max(MIN_W, w);
        this.worldH = Math.max(MIN_H, h);
    }

    public boolean isSelected()              { return selected; }
    public void setSelected(boolean sel)     { this.selected = sel; }

    /** 블록의 배경색(구체 클래스가 제공). */
    protected abstract Color bodyColor();
    /** 제목/요약 텍스트(구체 클래스가 제공). */
    protected abstract String titleText();
    /** 본문 추가 렌더링(선택적). 기본은 없음. */
    protected void paintBody(Graphics2D g2, int sx, int sy, int sw, int sh) { }

    /** 노드 그래프 연결의 입력(수신) 포트를 가지는지. 기본 false. */
    public boolean hasInputPort() { return false; }

    /** 입력 포트 중심의 screen 좌표 (좌측 가운데). */
    public Point inputPortCenterScreen(CanvasViewport vp) {
        return vp.worldToScreen(worldX, worldY + worldH / 2);
    }

    // ===== 히트 판정 (screen 좌표 기준) =====
    public boolean contains(CanvasViewport vp, int px, int py) {
        Rectangle2D r = screenRect(vp);
        return r.contains(px, py);
    }

    public boolean inResizeHandle(CanvasViewport vp, int px, int py) {
        Point br = vp.worldToScreen(worldX + worldW, worldY + worldH);
        return px >= br.x - HANDLE && px <= br.x
                && py >= br.y - HANDLE && py <= br.y;
    }

    private Rectangle2D screenRect(CanvasViewport vp) {
        Point tl = vp.worldToScreen(worldX, worldY);
        Point br = vp.worldToScreen(worldX + worldW, worldY + worldH);
        return new Rectangle2D.Double(tl.x, tl.y, br.x - tl.x, br.y - tl.y);
    }

    // ===== 렌더링 =====
    public void paint(Graphics2D g, CanvasViewport vp) {
        Point tl = vp.worldToScreen(worldX, worldY);
        Point br = vp.worldToScreen(worldX + worldW, worldY + worldH);
        int sx = tl.x, sy = tl.y;
        int sw = br.x - tl.x, sh = br.y - tl.y;
        if (sw <= 0 || sh <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 몸체
        g2.setColor(bodyColor());
        g2.fillRoundRect(sx, sy, sw, sh, 10, 10);

        // 테두리
        g2.setColor(selected ? new Color(0x4D96FF) : new Color(0xCCCCCC));
        g2.setStroke(new java.awt.BasicStroke(selected ? 2f : 1f));
        g2.drawRoundRect(sx, sy, sw, sh, 10, 10);

        // 본체 영역 밖으로 글자가 나가지 않도록 클립을 건다 (작게 줄였을 때 보호)
        Shape oldClip = g2.getClip();
        g2.clip(new RoundRectangle2D.Float(sx, sy, sw, sh, 10, 10));

        // 제목 — 높이가 너무 낮으면 생략
        if (sh >= 18) {
            g2.setColor(new Color(0x333333));
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            String title = clip(g2, titleText(), sw - 12);
            g2.drawString(title, sx + 6, sy + 16);
        }

        // 본문(구체 클래스) — 클립 안에서만 그려짐
        paintBody(g2, sx, sy, sw, sh);

        g2.setClip(oldClip);

        // 선택 시 리사이즈 핸들 (클립 밖에서 그려 항상 보이게)
        if (selected) {
            g2.setColor(new Color(0x4D96FF));
            g2.fillRect(br.x - HANDLE, br.y - HANDLE, HANDLE, HANDLE);
        }

        g2.dispose();
    }

    /**
     * 입력 포트(좌측 가운데 동그라미)를 그린다.
     * @param visible 패널이 "지금 포트를 보여줄 때"만 true (포트 드래그 중)
     */
    public void paintInputPort(Graphics2D g2, CanvasViewport vp, boolean visible) {
        if (!hasInputPort() || !visible) return;
        Point c = inputPortCenterScreen(vp);
        g2.setColor(new Color(0x9C6BFF));
        g2.fillOval(c.x - IN_PORT_R, c.y - IN_PORT_R, IN_PORT_R * 2, IN_PORT_R * 2);
        g2.setColor(Color.WHITE);
        g2.setStroke(new java.awt.BasicStroke(1.2f));
        g2.drawOval(c.x - IN_PORT_R, c.y - IN_PORT_R, IN_PORT_R * 2, IN_PORT_R * 2);
    }

    protected static String clip(Graphics2D g, String text, int maxWidth) {
        if (text == null) return "";
        if (maxWidth <= 0) return "";
        if (g.getFontMetrics().stringWidth(text) <= maxWidth) return text;
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (g.getFontMetrics().stringWidth(sb + String.valueOf(c) + "…") > maxWidth) break;
            sb.append(c);
        }
        return sb + "…";
    }
}
