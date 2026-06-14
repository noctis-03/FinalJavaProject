package com.blople.view.canvas;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public abstract class CanvasBlockComponent {

    public static final int HANDLE = 8;
    public static final int IN_PORT_R = 5;
    public static final double MIN_W = 60;
    public static final double MIN_H = 44;

    /** 우선순위 1(가장 위), 2, 3(가장 아래). 기본 2. */
    public static final int PRIORITY_DEFAULT = 2;
    public static final int PRIORITY_MIN = 1;
    public static final int PRIORITY_MAX = 3;

    protected final String blockId;
    protected double worldX, worldY, worldW, worldH;
    protected boolean selected;

    /** 1=가장 위, 3=가장 아래. */
    protected int priority = PRIORITY_DEFAULT;

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

    public int getPriority() { return priority; }
    public void setPriority(int p) {
        this.priority = Math.max(PRIORITY_MIN, Math.min(PRIORITY_MAX, p));
    }

    protected abstract Color bodyColor();
    protected abstract String titleText();
    protected void paintBody(Graphics2D g2, int sx, int sy, int sw, int sh) { }

    public boolean hasInputPort() { return false; }

    public Point inputPortCenterScreen(CanvasViewport vp) {
        return vp.worldToScreen(worldX, worldY + worldH / 2);
    }

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

    public void paint(Graphics2D g, CanvasViewport vp) {
        Point tl = vp.worldToScreen(worldX, worldY);
        Point br = vp.worldToScreen(worldX + worldW, worldY + worldH);
        int sx = tl.x, sy = tl.y;
        int sw = br.x - tl.x, sh = br.y - tl.y;
        if (sw <= 0 || sh <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(bodyColor());
        g2.fillRoundRect(sx, sy, sw, sh, 10, 10);

        g2.setColor(selected ? new Color(0x4D96FF) : new Color(0xCCCCCC));
        g2.setStroke(new java.awt.BasicStroke(selected ? 2f : 1f));
        g2.drawRoundRect(sx, sy, sw, sh, 10, 10);

        Shape oldClip = g2.getClip();
        g2.clip(new RoundRectangle2D.Float(sx, sy, sw, sh, 10, 10));

        if (sh >= 18) {
            g2.setColor(new Color(0x333333));
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            String title = clip(g2, titleText(), sw - 12);
            g2.drawString(title, sx + 6, sy + 16);
        }

        paintBody(g2, sx, sy, sw, sh);

        g2.setClip(oldClip);

        if (selected) {
            g2.setColor(new Color(0x4D96FF));
            g2.fillRect(br.x - HANDLE, br.y - HANDLE, HANDLE, HANDLE);
        }

        g2.dispose();
    }

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
