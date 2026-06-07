package com.blople.view.calendar;

import com.blople.model.block.ScheduleBlock;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 월간 뷰 렌더링 패널 (FR-01).
 *
 * <p>6주 × 7일 격자를 그리고, 각 날짜 셀 안에 해당 날짜에 걸치는 일정 블록을
 * 최대 N개까지 작은 막대로 표시한다. 초과분은 "+k" 로 요약한다.
 *
 * <p>요일 규약: 0=일 … 6=토.
 */
public class MonthViewPanel extends JPanel {

    private static final DateTimeFormatter TS_FMT = ScheduleBlock.TS_FORMATTER;
    private static final String[] DAY_HEADERS = {"일", "월", "화", "수", "목", "금", "토"};

    private static final int HEADER_HEIGHT = 28;
    private static final int MAX_BARS_PER_CELL = 3;

    private static final Color GRID_LINE  = new Color(0xE3E3E8);
    private static final Color HEADER_BG  = new Color(0xF2F4F8);
    private static final Color TODAY_BG   = new Color(0xFFF6E0);
    private static final Color OUT_MONTH  = new Color(0xFAFAFB);
    private static final Color OUT_TEXT   = new Color(0xBBBBBB);

    private int year;
    private int month;
    private List<ScheduleBlock> blocks = new ArrayList<>();

    private Consumer<LocalDate> onDateClick;

    public MonthViewPanel() {
        LocalDate now = LocalDate.now();
        this.year = now.getYear();
        this.month = now.getMonthValue();
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(840, 600));

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                LocalDate d = dateAt(e.getX(), e.getY());
                if (d != null && onDateClick != null) onDateClick.accept(d);
            }
        });
    }

    public void setOnDateClick(Consumer<LocalDate> cb) { this.onDateClick = cb; }

    public void setYearMonth(int year, int month) {
        this.year = year;
        this.month = month;
        repaint();
    }

    public void setBlocks(List<ScheduleBlock> blocks) {
        this.blocks = (blocks != null) ? blocks : new ArrayList<>();
        repaint();
    }

    /** 그리드 첫 칸에 해당하는 날짜 (이전 달 일부 포함). */
    private LocalDate gridStartDate() {
        LocalDate first = LocalDate.of(year, month, 1);
        int offset = first.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : first.getDayOfWeek().getValue();
        return first.minusDays(offset);
    }

    private int cellWidth()  { return getWidth() / 7; }
    private int cellHeight() { return (getHeight() - HEADER_HEIGHT) / 6; }

    private LocalDate dateAt(int mx, int my) {
        if (my < HEADER_HEIGHT) return null;
        int col = mx / cellWidth();
        int row = (my - HEADER_HEIGHT) / cellHeight();
        if (col < 0 || col > 6 || row < 0 || row > 5) return null;
        return gridStartDate().plusDays((long) row * 7 + col);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cw = cellWidth();
        int ch = cellHeight();

        drawHeader(g2, cw);
        drawCells(g2, cw, ch);
    }

    private void drawHeader(Graphics2D g2, int cw) {
        g2.setColor(HEADER_BG);
        g2.fillRect(0, 0, getWidth(), HEADER_HEIGHT);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        for (int d = 0; d < 7; d++) {
            Color fg = (d == 0) ? new Color(0xE5484D)
                    : (d == 6) ? new Color(0x3A6FF0)
                      : new Color(0x333333);
            g2.setColor(fg);
            g2.drawString(DAY_HEADERS[d], d * cw + 8, 19);
        }
    }

    private void drawCells(Graphics2D g2, int cw, int ch) {
        LocalDate start = gridStartDate();
        LocalDate today = LocalDate.now();

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                LocalDate date = start.plusDays((long) row * 7 + col);
                int x = col * cw;
                int y = HEADER_HEIGHT + row * ch;

                boolean inMonth = date.getMonthValue() == month && date.getYear() == year;

                if (!inMonth) {
                    g2.setColor(OUT_MONTH);
                    g2.fillRect(x, y, cw, ch);
                }
                if (date.equals(today)) {
                    g2.setColor(TODAY_BG);
                    g2.fillRect(x, y, cw, ch);
                }

                // 날짜 숫자
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.setColor(inMonth ? dayColor(col) : OUT_TEXT);
                g2.drawString(String.valueOf(date.getDayOfMonth()), x + 6, y + 16);

                // 블록 막대
                drawDayBlocks(g2, date, x, y, cw, ch);

                // 셀 테두리
                g2.setColor(GRID_LINE);
                g2.drawRect(x, y, cw, ch);
            }
        }
    }

    private Color dayColor(int col) {
        if (col == 0) return new Color(0xE5484D);
        if (col == 6) return new Color(0x3A6FF0);
        return new Color(0x333333);
    }

    private void drawDayBlocks(Graphics2D g2, LocalDate date, int x, int y, int cw, int ch) {
        List<ScheduleBlock> dayBlocks = blocksOn(date);
        if (dayBlocks.isEmpty()) return;

        int barH = 15;
        int gap = 2;
        int startY = y + 22;

        int shown = Math.min(dayBlocks.size(), MAX_BARS_PER_CELL);
        for (int i = 0; i < shown; i++) {
            ScheduleBlock b = dayBlocks.get(i);
            int by = startY + i * (barH + gap);
            if (by + barH > y + ch - 2) break;

            Color fill = BlockRenderer.parseColor(b.getColor(), new Color(0x4D96FF));
            g2.setColor(fill);
            g2.fillRoundRect(x + 4, by, cw - 8, barH, 6, 6);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            String name = clip(g2, b.getName(), cw - 14);
            g2.drawString(name, x + 8, by + 11);
        }

        int extra = dayBlocks.size() - shown;
        if (extra > 0) {
            int by = startY + shown * (barH + gap);
            if (by + 10 <= y + ch) {
                g2.setColor(new Color(0x888888));
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g2.drawString("+" + extra, x + 8, by + 9);
            }
        }
    }

    /** 해당 날짜에 걸치는 블록만 추림 (시작~종료 사이 포함). */
    private List<ScheduleBlock> blocksOn(LocalDate date) {
        List<ScheduleBlock> result = new ArrayList<>();
        for (ScheduleBlock b : blocks) {
            LocalDate s = LocalDateTime.parse(b.getStartDatetime(), TS_FMT).toLocalDate();
            LocalDate e = LocalDateTime.parse(b.getEndDatetime(), TS_FMT).toLocalDate();
            if (!date.isBefore(s) && !date.isAfter(e)) {
                result.add(b);
            }
        }
        return result;
    }

    private static String clip(Graphics2D g, String text, int maxWidth) {
        if (text == null) return "";
        if (g.getFontMetrics().stringWidth(text) <= maxWidth) return text;
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (g.getFontMetrics().stringWidth(sb.toString() + c + "…") > maxWidth) break;
            sb.append(c);
        }
        return sb + "…";
    }
}
