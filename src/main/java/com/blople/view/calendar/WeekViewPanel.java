package com.blople.view.calendar;

import com.blople.common.Constants;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 주간 뷰 렌더링 패널 (FR-01).
 *
 * <p>좌측 시간축(8:00~23:00) + 상단 요일 헤더(일~토) 그리드를 그리고,
 * 그 위에 {@link ScheduleBlock} 들을 시간/요일 좌표로 변환해 배치한다.
 *
 * <p>좌표 규약: 요일 0=일 … 6=토 (프로젝트 공통 규약).
 */
public class WeekViewPanel extends JPanel {

    private static final DateTimeFormatter TS_FMT = ScheduleBlock.TS_FORMATTER;

    private static final String[] DAY_HEADERS = {"일", "월", "화", "수", "목", "금", "토"};

    private static final int TIME_AXIS_WIDTH = 56;   // 좌측 시간축 폭
    private static final int HEADER_HEIGHT   = 32;   // 상단 요일 헤더 높이
    private static final int SLOT_HEIGHT     = 28;   // 30분 슬롯 1칸 높이

    private static final Color GRID_LINE   = new Color(0xE3E3E8);
    private static final Color HEADER_BG   = new Color(0xF2F4F8);
    private static final Color TODAY_BG    = new Color(0xFFF6E0);
    private static final Color AXIS_TEXT   = new Color(0x666666);

    private final int startHour = Constants.CALENDAR_START_HOUR; // 8
    private final int endHour   = Constants.CALENDAR_END_HOUR;   // 23
    private final int rowsPerDay; // 30분 슬롯 수

    private LocalDate weekStart; // 일요일
    private List<ScheduleBlock> blocks = new ArrayList<>();
    private final List<BlockRenderer> renderers = new ArrayList<>();

    private String selectedBlockId;

    /** 블록 클릭 콜백 (Controller 가 주입). */
    private Consumer<ScheduleBlock> onBlockClick;

    public WeekViewPanel() {
        this.rowsPerDay = (endHour - startHour) * 2;
        setBackground(Color.WHITE);
        setBaseDate(LocalDate.now());

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    public void setOnBlockClick(Consumer<ScheduleBlock> cb) { this.onBlockClick = cb; }

    /** 기준 날짜가 포함된 주(일~토)로 설정. */
    public void setBaseDate(LocalDate base) {
        this.weekStart = base.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        recomputePreferredSize();
        rebuildRenderers();
        revalidate();
        repaint();
    }

    public void setBlocks(List<ScheduleBlock> blocks) {
        this.blocks = (blocks != null) ? blocks : new ArrayList<>();
        rebuildRenderers();
        repaint();
    }

    public void setSelectedBlockId(String id) {
        this.selectedBlockId = id;
        for (BlockRenderer r : renderers) {
            r.setSelected(r.getBlock().getId().equals(id));
        }
        repaint();
    }

    private void recomputePreferredSize() {
        int w = TIME_AXIS_WIDTH + 7 * 120;
        int h = HEADER_HEIGHT + rowsPerDay * SLOT_HEIGHT;
        setPreferredSize(new Dimension(w, h));
    }

    private int dayColumnWidth() {
        int avail = getWidth() - TIME_AXIS_WIDTH;
        return Math.max(60, avail / 7);
    }

    /** 블록 목록을 좌표로 변환해 BlockRenderer 리스트를 다시 만든다. */
    private void rebuildRenderers() {
        renderers.clear();
        if (getWidth() == 0) return; // 아직 레이아웃 전이면 paint 시점에 다시 계산

        int colW = dayColumnWidth();
        for (ScheduleBlock b : blocks) {
            LocalDateTime s = LocalDateTime.parse(b.getStartDatetime(), TS_FMT);
            LocalDateTime e = LocalDateTime.parse(b.getEndDatetime(), TS_FMT);

            LocalDate sDate = s.toLocalDate();
            int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(weekStart, sDate);
            if (dayIndex < 0 || dayIndex > 6) continue; // 이번 주 밖

            int x = TIME_AXIS_WIDTH + dayIndex * colW + 2;
            int y = HEADER_HEIGHT + minutesFromStart(s.toLocalTime()) * SLOT_HEIGHT / 30;
            int endY = HEADER_HEIGHT + minutesFromStart(e.toLocalTime()) * SLOT_HEIGHT / 30;
            int h = Math.max(SLOT_HEIGHT / 2, endY - y);
            int w = colW - 4;

            BlockRenderer r = new BlockRenderer(b);
            r.setBounds(x, y, w, h);
            r.setSelected(b.getId().equals(selectedBlockId));
            renderers.add(r);
        }
    }

    /** startHour 기준으로 몇 분 지났는지 (음수/초과는 클램프). */
    private int minutesFromStart(LocalTime t) {
        int minutes = (t.getHour() - startHour) * 60 + t.getMinute();
        int max = (endHour - startHour) * 60;
        return Math.max(0, Math.min(minutes, max));
    }

    private void handleClick(int mx, int my) {
        // 위에 그려진 블록(나중에 추가된 것)을 우선 판정
        for (int i = renderers.size() - 1; i >= 0; i--) {
            BlockRenderer r = renderers.get(i);
            if (r.contains(mx, my)) {
                setSelectedBlockId(r.getBlock().getId());
                if (onBlockClick != null) onBlockClick.accept(r.getBlock());
                return;
            }
        }
        setSelectedBlockId(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        rebuildRenderers(); // 현재 폭 기준으로 좌표 재계산

        int colW = dayColumnWidth();

        drawHeader(g2, colW);
        drawTimeAxisAndGrid(g2, colW);

        for (BlockRenderer r : renderers) {
            r.paint(g2);
        }
    }

    private void drawHeader(Graphics2D g2, int colW) {
        g2.setColor(HEADER_BG);
        g2.fillRect(0, 0, getWidth(), HEADER_HEIGHT);

        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        LocalDate today = LocalDate.now();
        for (int d = 0; d < 7; d++) {
            LocalDate date = weekStart.plusDays(d);
            int x = TIME_AXIS_WIDTH + d * colW;

            if (date.equals(today)) {
                g2.setColor(TODAY_BG);
                g2.fillRect(x, 0, colW, HEADER_HEIGHT);
            }

            Color fg = (d == 0) ? new Color(0xE5484D)        // 일요일 빨강
                    : (d == 6) ? new Color(0x3A6FF0)         // 토요일 파랑
                      : new Color(0x333333);
            g2.setColor(fg);
            String label = DAY_HEADERS[d] + " " + date.getDayOfMonth();
            g2.drawString(label, x + 8, 21);
        }
    }

    private void drawTimeAxisAndGrid(Graphics2D g2, int colW) {
        // 오늘 열 배경
        LocalDate today = LocalDate.now();
        for (int d = 0; d < 7; d++) {
            if (weekStart.plusDays(d).equals(today)) {
                int x = TIME_AXIS_WIDTH + d * colW;
                g2.setColor(TODAY_BG);
                g2.fillRect(x, HEADER_HEIGHT, colW, rowsPerDay * SLOT_HEIGHT);
            }
        }

        // 가로선 + 시간 라벨
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (int row = 0; row <= rowsPerDay; row++) {
            int y = HEADER_HEIGHT + row * SLOT_HEIGHT;
            boolean isHour = (row % 2 == 0);

            g2.setColor(isHour ? GRID_LINE : new Color(0xF0F0F3));
            g2.drawLine(TIME_AXIS_WIDTH, y, getWidth(), y);

            if (isHour) {
                int hour = startHour + row / 2;
                g2.setColor(AXIS_TEXT);
                g2.drawString(String.format("%02d:00", hour), 8, y + 14);
            }
        }

        // 세로선
        g2.setColor(GRID_LINE);
        for (int d = 0; d <= 7; d++) {
            int x = TIME_AXIS_WIDTH + d * colW;
            g2.drawLine(x, HEADER_HEIGHT, x, HEADER_HEIGHT + rowsPerDay * SLOT_HEIGHT);
        }
    }
}
