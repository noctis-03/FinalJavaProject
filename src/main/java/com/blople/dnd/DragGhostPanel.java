package com.blople.dnd;

import com.blople.view.calendar.BlockRenderer;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

/**
 * 드래그 중 마우스를 따라다니는 반투명 미리보기 (FR-11).
 *
 * <p>JFrame 의 글래스페인으로 설정되어 모든 컴포넌트 위에 그려진다.
 * DnD 메커니즘과는 독립적인 "시각 피드백 전용" 컴포넌트.
 */
public class DragGhostPanel extends JComponent {

    private boolean active = false;
    private String label = "";
    private Color color = new Color(0x4D96FF);
    private final Point pos = new Point();

    private static final int W = 130;
    private static final int H = 36;

    public void start(String label, String colorHex) {
        this.active = true;
        this.label = label;
        this.color = BlockRenderer.parseColor(colorHex, new Color(0x4D96FF));
        setVisible(true);
        repaint();
    }

    /** 좌표는 글래스페인(=프레임 콘텐츠) 기준. */
    public void moveTo(Point glassPoint) {
        this.pos.setLocation(glassPoint);
        repaint();
    }

    public void stop() {
        this.active = false;
        setVisible(false);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!active) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = pos.x + 10;
        int y = pos.y - H / 2;

        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
        g2.fillRoundRect(x, y, W, H, 10, 10);
        g2.setColor(new Color(255, 255, 255, 230));
        g2.drawString(clip(label), x + 8, y + H / 2 + 4);

        g2.dispose();
    }

    private String clip(String s) {
        if (s == null) return "";
        return s.length() > 12 ? s.substring(0, 12) + "…" : s;
    }
}
