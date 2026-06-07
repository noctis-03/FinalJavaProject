package com.blople.view.calendar;

import com.blople.model.block.ScheduleBlock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * 캘린더 위에 일정 블록 하나를 그리는 렌더러 (FR-01).
 *
 * <p>좌표/크기는 외부(WeekViewPanel, MonthViewPanel)가 계산해 setBounds 로 주입한다.
 * 본 클래스는 "주어진 사각형 영역에 블록을 어떻게 칠할지"만 책임진다.
 *
 * <p>상태(selected, dragging, conflict)에 따라 테두리·투명도를 달리한다.
 */
public class BlockRenderer {

    private static final Color DEFAULT_FILL    = new Color(0x4D96FF);
    private static final Color TEXT_COLOR      = Color.WHITE;
    private static final Color SELECTED_BORDER = new Color(0x222222);
    private static final Color CONFLICT_BORDER = new Color(0xFF3B30);

    private final ScheduleBlock block;
    private final Rectangle bounds = new Rectangle();

    private boolean selected;
    private boolean dragging;
    private boolean conflict;

    public BlockRenderer(ScheduleBlock block) {
        this.block = block;
    }

    public ScheduleBlock getBlock()             { return block; }
    public Rectangle getBounds()                { return bounds; }
    public void setBounds(int x, int y, int w, int h) { bounds.setBounds(x, y, w, h); }

    public boolean isSelected()                 { return selected; }
    public void setSelected(boolean selected)   { this.selected = selected; }
    public boolean isDragging()                 { return dragging; }
    public void setDragging(boolean dragging)   { this.dragging = dragging; }
    public boolean isConflict()                 { return conflict; }
    public void setConflict(boolean conflict)   { this.conflict = conflict; }

    /** 포인트가 이 블록 영역 안에 있는지 (클릭 판정용). */
    public boolean contains(int px, int py) {
        return bounds.contains(px, py);
    }

    public void paint(Graphics2D g) {
        if (bounds.width <= 0 || bounds.height <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color fill = parseColor(block.getColor(), DEFAULT_FILL);
        if (dragging) {
            // 드래그 중에는 반투명
            fill = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 130);
        }

        int arc = 10;
        g2.setColor(fill);
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc);

        // 테두리 — 충돌 > 선택 순으로 강조
        if (conflict) {
            g2.setColor(CONFLICT_BORDER);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, arc, arc);
        } else if (selected) {
            g2.setColor(SELECTED_BORDER);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, arc, arc);
        }

        // 텍스트 (영역이 충분히 클 때만)
        if (bounds.height >= 18 && bounds.width >= 30) {
            g2.setColor(TEXT_COLOR);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            String label = clip(g2, block.getName(), bounds.width - 8);
            g2.drawString(label, bounds.x + 4, bounds.y + 14);

            if (bounds.height >= 32) {
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                String time = timeRange();
                String timeClip = clip(g2, time, bounds.width - 8);
                g2.drawString(timeClip, bounds.x + 4, bounds.y + 27);
            }
        }

        g2.dispose();
    }

    private String timeRange() {
        // "yyyy-MM-dd HH:mm:ss" 에서 HH:mm 만 추출
        String s = block.getStartDatetime();
        String e = block.getEndDatetime();
        return hhmm(s) + "~" + hhmm(e);
    }

    private static String hhmm(String datetime) {
        if (datetime == null || datetime.length() < 16) return "";
        return datetime.substring(11, 16);
    }

    /** 폭을 초과하면 말줄임. */
    private static String clip(Graphics2D g, String text, int maxWidth) {
        if (text == null) return "";
        if (g.getFontMetrics().stringWidth(text) <= maxWidth) return text;
        String ellipsis = "…";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (g.getFontMetrics().stringWidth(sb.toString() + c + ellipsis) > maxWidth) break;
            sb.append(c);
        }
        return sb + ellipsis;
    }

    /** "#RRGGBB" → Color, 실패 시 fallback. */
    public static Color parseColor(String hex, Color fallback) {
        if (hex == null || hex.isBlank()) return fallback;
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            return new Color(Integer.parseInt(h, 16));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
