package com.blople.view.calendar;

import com.blople.model.block.ScheduleBlock;
import com.blople.model.enums.ViewMode;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * 캘린더 페이지 전체 레이아웃 (FR-01).
 *
 * <p>상단 툴바: [이전][오늘][다음]  날짜라벨  [주간/월간 토글]
 * 중앙: {@link CalendarGridPanel}
 *
 * <p>버튼 동작 자체는 Controller 가 콜백으로 주입한다.
 */
public class CalendarPagePanel extends JPanel {

    private static final Color TOOLBAR_BG = new Color(0xFFFFFF);

    private final JButton prevButton   = new JButton("◀");
    private final JButton todayButton  = new JButton("오늘");
    private final JButton nextButton   = new JButton("▶");
    private final JButton viewToggle   = new JButton("월간 보기");
    private final JLabel  dateLabel    = new JLabel("―");

    private final CalendarGridPanel gridPanel = new CalendarGridPanel();

    public CalendarPagePanel() {
        setLayout(new BorderLayout());
        add(buildToolbar(), BorderLayout.NORTH);
        add(gridPanel, BorderLayout.CENTER);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(TOOLBAR_BG);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xDDDDDD)));
        bar.setPreferredSize(new Dimension(0, 48));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 8));
        left.setOpaque(false);
        styleNavButton(prevButton);
        styleNavButton(nextButton);
        styleNavButton(todayButton);
        dateLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        dateLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        left.add(prevButton);
        left.add(todayButton);
        left.add(nextButton);
        left.add(dateLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
        right.setOpaque(false);
        styleNavButton(viewToggle);
        right.add(viewToggle);
        right.add(Box.createHorizontalStrut(8));

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void styleNavButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ===== Controller 가 사용할 표시 갱신 메서드 =====
    public void updateDateLabel(String text)        { dateLabel.setText(text); }
    public void updateViewToggleLabel(ViewMode mode) {
        viewToggle.setText(mode == ViewMode.WEEK ? "월간 보기" : "주간 보기");
    }

    public void setViewMode(ViewMode mode)              { gridPanel.setViewMode(mode); }
    public void setWeekBaseDate(LocalDate date)         { gridPanel.setWeekBaseDate(date); }
    public void setMonthYearMonth(int year, int month)  { gridPanel.setMonthYearMonth(year, month); }
    public void refresh(List<ScheduleBlock> blocks)     { gridPanel.refresh(blocks); }

    // ===== Controller 콜백 주입 =====
    public void setOnPrev(Runnable r)      { prevButton.addActionListener(e -> r.run()); }
    public void setOnNext(Runnable r)      { nextButton.addActionListener(e -> r.run()); }
    public void setOnToday(Runnable r)     { todayButton.addActionListener(e -> r.run()); }
    public void setOnViewToggle(Runnable r){ viewToggle.addActionListener(e -> r.run()); }
    public void setOnBlockClick(Consumer<ScheduleBlock> cb) { gridPanel.setOnBlockClick(cb); }
    public void setOnDateClick(Consumer<LocalDate> cb)      { gridPanel.setOnDateClick(cb); }

    public CalendarGridPanel getGridPanel() { return gridPanel; }
}
