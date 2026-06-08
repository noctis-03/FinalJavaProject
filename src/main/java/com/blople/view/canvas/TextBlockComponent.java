package com.blople.view.canvas;

import com.blople.model.block.TextBlock;
import com.blople.view.calendar.BlockRenderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * 캔버스 위 텍스트 블록 (FR-06) — 포스트잇 형태.
 */
public class TextBlockComponent extends CanvasBlockComponent {

    private final TextBlock model;

    public TextBlockComponent(TextBlock model, double x, double y, double w, double h) {
        super(model.getId(), x, y, w, h);
        this.model = model;
    }

    public TextBlock getModel() { return model; }

    @Override
    protected Color bodyColor() {
        return BlockRenderer.parseColor(model.getBackgroundColor(), new Color(0xFFFF99));
    }

    @Override
    protected String titleText() {
        String t = model.getTitle();
        return (t == null || t.isBlank()) ? "메모" : t;
    }

    @Override
    protected void paintBody(Graphics2D g2, int sx, int sy, int sw, int sh) {
        String content = model.getContent();
        if (content == null || content.isBlank()) return;

        g2.setColor(new Color(0x555555));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));

        // 내용을 줄 단위로 그리기 (영역 안에서만)
        int lineH = 14;
        int y = sy + 32;
        for (String line : content.split("\n")) {
            if (y > sy + sh - 6) break;
            g2.drawString(clip(g2, line, sw - 12), sx + 6, y);
            y += lineH;
        }
    }
}
