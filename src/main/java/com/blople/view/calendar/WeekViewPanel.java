package com.blople.view.calendar;

import com.blople.common.Constants;
import com.blople.model.block.ScheduleBlock;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WeekViewPanel extends JPanel {

    private static final DateTimeFormatter TS_FMT = ScheduleBlock.TS_FORMATTER;
    private static final String[] DAY_HEADERS = {"일", "월", "화", "수", "목", "금", "토"};

    private static final int TIME_AXIS_WIDTH = 56;
    private static final int HEADER_HEIGHT   = 32;
    private static final int SLOT_HEIGHT     = 28;

    private static final Color GRID_LINE   = new Color(0xE3E3E8);
    private static final Color HEADER_BG   = new Color(0xF2F4F8);
    private static final Color TODAY_BG    = new Color(0xFFF6E0);
    private static final Color AXIS_TEXT   = new Color(0x666666);

    private final int startHour = Constants.CALENDAR_START_HOUR;
    private final int endHour   = Constants.CALENDAR_END_HOUR;
    private final int rowsPerDay;

    private LocalDate weekStart;
    private List<ScheduleBlock> blocks = new ArrayList<>();
    private final List<BlockRenderer> renderers = new ArrayList<>();

    private String selectedBlockId;

    private Consumer<ScheduleBlock> onBlockClick;
    private Consumer<ScheduleBlock> onBlockDoubleClick;
    private Consumer<ScheduleBlock> onBlockDelete;

    /** (blockId, newStart, newEnd) — 드래그로 시간 이동했을 때 호출. */
    public interface BlockMoveListener {
        void onMove(String blockId, LocalDateTime newStart, LocalDateTime newEnd);
    }
    private BlockMoveListener onBlockMove;

    /** 빈 셀 클릭으로 새 일정 생성을 요청. */
    private Consumer<LocalDateTime> onEmptySlotDoubleClick;

    // ===== 드래그 상태 =====
    private BlockRenderer dragging;
    private int dragOffsetY;     // 마우스가 블록 상단에서 얼마나 아래에 있는지
    private int dragStartCol;    // 시작 컬럼
    private int dragStartRow;    // 시작 30분 인덱스 (요일 무관, 절대 행)

    public WeekViewPanel() {
        this.rowsPerDay = (endHour - startHour) * 2;
        setBackground(Color.WHITE);
        setBaseDate(LocalDate.now());

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                handlePress(e);
            }
            @Override public void mouseReleased(MouseEvent e) {
                handleRelease(e);
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) return;
                if (e.getClickCount() == 2) {
                    handleDoubleClick(e);
                } else {
                    handleClick(e.getX(), e.getY());
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                handleDrag(e);
            }
            @Override public void mouseMoved(MouseEvent e) {
                BlockRenderer over = hit(e.getX(), e.getY());
                setCursor(over != null
                        ? Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                        : Cursor.getDefaultCursor());
            }
        });
    }

    public void setOnBlockClick(Consumer<ScheduleBlock> cb)        { this.onBlockClick = cb; }
    public void setOnBlockDoubleClick(Consumer<ScheduleBlock> cb)  { this.onBlockDoubleClick = cb; }
    public void setOnBlockDelete(Consumer<ScheduleBlock> cb)       { this.onBlockDelete = cb; }
    public void setOnBlockMove(BlockMoveListener cb)               { this.onBlockMove = cb; }
    public void setOnEmptySlotDoubleClick(Consumer<LocalDateTime> cb) { this.onEmptySlotDoubleClick = cb; }

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

    private void rebuildRenderers() {
        renderers.clear();
        if (getWidth() == 0) return;

        int colW = dayColumnWidth();
        for (ScheduleBlock b : blocks) {
            LocalDateTime s = LocalDateTime.parse(b.getStartDatetime(), TS_FMT);
            LocalDateTime e = LocalDateTime.parse(b.getEndDatetime(), TS_FMT);

            LocalDate sDate = s.toLocalDate();
            int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(weekStart, sDate);
            if (dayIndex < 0 || dayIndex > 6) continue;

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

    private int minutesFromStart(LocalTime t) {
        int minutes = (t.getHour() - startHour) * 60 + t.getMinute();
        int max = (endHour - startHour) * 60;
        return Math.max(0, Math.min(minutes, max));
    }

    // ===== 마우스 처리 =====
    private BlockRenderer hit(int mx, int my) {
        for (int i = renderers.size() - 1; i >= 0; i--) {
            BlockRenderer r = renderers.get(i);
            if (r.contains(mx, my)) return r;
        }
        return null;
    }

    private void handleClick(int mx, int my) {
        BlockRenderer hit = hit(mx, my);
        if (hit != null) {
            setSelectedBlockId(hit.getBlock().getId());
            if (onBlockClick != null) onBlockClick.accept(hit.getBlock());
        } else {
            setSelectedBlockId(null);
        }
    }

    private void handleDoubleClick(MouseEvent e) {
        BlockRenderer hit = hit(e.getX(), e.getY());
        if (hit != null) {
            if (onBlockDoubleClick != null) onBlockDoubleClick.accept(hit.getBlock());
        } else {
            LocalDateTime when = datetimeAt(e.getX(), e.getY());
            if (when != null && onEmptySlotDoubleClick != null) {
                onEmptySlotDoubleClick.accept(when);
            }
        }
    }

    private void handlePress(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            BlockRenderer hit = hit(e.getX(), e.getY());
            if (hit != null) {
                showContextMenu(hit, e.getX(), e.getY());
            }
            return;
        }
        if (SwingUtilities.isLeftMouseButton(e)) {
            BlockRenderer hit = hit(e.getX(), e.getY());
            if (hit != null) {
                dragging = hit;
                dragOffsetY = e.getY() - hit.getBounds().y;
                setSelectedBlockId(hit.getBlock().getId());

                int colW = dayColumnWidth();
                dragStartCol = (hit.getBounds().x - TIME_AXIS_WIDTH) / colW;
                dragStartRow = (hit.getBounds().y - HEADER_HEIGHT) / SLOT_HEIGHT;
                hit.setDragging(true);
                repaint();
            }
        }
    }

    private void handleDrag(MouseEvent e) {
        if (dragging == null) return;

        int colW = dayColumnWidth();
        int col = (e.getX() - TIME_AXIS_WIDTH) / colW;
        col = Math.max(0, Math.min(6, col));

        int rawTop = e.getY() - dragOffsetY - HEADER_HEIGHT;
        int row = Math.round((float) rawTop / SLOT_HEIGHT);
        int durationRows = Math.max(1, dragging.getBounds().height / SLOT_HEIGHT);
        row = Math.max(0, Math.min(rowsPerDay - durationRows, row));

        int newX = TIME_AXIS_WIDTH + col * colW + 2;
        int newY = HEADER_HEIGHT + row * SLOT_HEIGHT;
        dragging.setBounds(newX, newY, dragging.getBounds().width, dragging.getBounds().height);
        repaint();
    }

    private void handleRelease(MouseEvent e) {
        if (dragging == null) return;
        BlockRenderer rel = dragging;
        rel.setDragging(false);
        dragging = null;

        int colW = dayColumnWidth();
        int col = (rel.getBounds().x - TIME_AXIS_WIDTH) / colW;
        int row = (rel.getBounds().y - HEADER_HEIGHT) / SLOT_HEIGHT;
        col = Math.max(0, Math.min(6, col));
        row = Math.max(0, Math.min(rowsPerDay - 1, row));

        if (col == dragStartCol && row == dragStartRow) {
            // 위치 변동 없음 — 그냥 재배치만
            rebuildRenderers();
            repaint();
            return;
        }

        // 원본 길이 보존
        ScheduleBlock b = rel.getBlock();
        LocalDateTime origStart = LocalDateTime.parse(b.getStartDatetime(), TS_FMT);
        LocalDateTime origEnd   = LocalDateTime.parse(b.getEndDatetime(),   TS_FMT);
        long durationMin = java.time.Duration.between(origStart, origEnd).toMinutes();

        LocalDate newDate = weekStart.plusDays(col);
        int totalMin = row * 30;
        LocalTime newStartTime = LocalTime.of(startHour + totalMin / 60, totalMin % 60);
        LocalDateTime newStart = LocalDateTime.of(newDate, newStartTime);
        LocalDateTime newEnd = newStart.plusMinutes(durationMin);

        if (onBlockMove != null) {
            onBlockMove.onMove(b.getId(), newStart, newEnd);
        } else {
            rebuildRenderers();
            repaint();
        }
    }

    private void showContextMenu(BlockRenderer hit, int mx, int my) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem edit = new JMenuItem("일정 편집");
        edit.addActionListener(ev -> {
            if (onBlockDoubleClick != null) onBlockDoubleClick.accept(hit.getBlock());
        });
        JMenuItem del = new JMenuItem("일정 삭제");
        del.addActionListener(ev -> {
            if (onBlockDelete != null) onBlockDelete.accept(hit.getBlock());
        });
        menu.add(edit);
        menu.addSeparator();
        menu.add(del);
        menu.show(this, mx, my);
    }

    public LocalDateTime datetimeAt(int mx, int my) {
        if (my < HEADER_HEIGHT) return null;
        if (mx < TIME_AXIS_WIDTH) return null;

        int colW = dayColumnWidth();
        int dayIndex = (mx - TIME_AXIS_WIDTH) / colW;
        if (dayIndex < 0 || dayIndex > 6) return null;

        int rowsFromTop = (my - HEADER_HEIGHT) / SLOT_HEIGHT;
        if (rowsFromTop < 0) rowsFromTop = 0;
        if (rowsFromTop > rowsPerDay - 1) rowsFromTop = rowsPerDay - 1;

        LocalDate date = weekStart.plusDays(dayIndex);
        int totalMinutes = rowsFromTop * 30;
        return date.atTime(startHour + totalMinutes / 60, totalMinutes % 60);
    }

    public LocalDate getWeekStart() { return weekStart; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (dragging == null) rebuildRenderers();

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

            Color fg = (d == 0) ? new Color(0xE5484D)
                    : (d == 6) ? new Color(0x3A6FF0)
                      : new Color(0x333333);
            g2.setColor(fg);
            String label = DAY_HEADERS[d] + " " + date.getDayOfMonth();
            g2.drawString(label, x + 8, 21);
        }
    }

    private void drawTimeAxisAndGrid(Graphics2D g2, int colW) {
        LocalDate today = LocalDate.now();
        for (int d = 0; d < 7; d++) {
            if (weekStart.plusDays(d).equals(today)) {
                int x = TIME_AXIS_WIDTH + d * colW;
                g2.setColor(TODAY_BG);
                g2.fillRect(x, HEADER_HEIGHT, colW, rowsPerDay * SLOT_HEIGHT);
            }
        }

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

        g2.setColor(GRID_LINE);
        for (int d = 0; d <= 7; d++) {
            int x = TIME_AXIS_WIDTH + d * colW;
            g2.drawLine(x, HEADER_HEIGHT, x, HEADER_HEIGHT + rowsPerDay * SLOT_HEIGHT);
        }
    }
}
