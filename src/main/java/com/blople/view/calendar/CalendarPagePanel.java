package com.blople.view.calendar;

import com.blople.model.automation.CalendarAutomationRule;
import com.blople.model.block.PresetBlock;
import com.blople.model.block.ScheduleBlock;
import com.blople.model.enums.ViewMode;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
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
 * 캘린더 페이지 전체 레이아웃 (FR-01, FR-02, FR-12).
 *
 * <p>좌측: {@link PresetBlockPanel} (미리블록)
 * <p>우측: 상단 툴바 + 중앙 {@link CalendarGridPanel}(위) / {@link CalendarAutomationPanel}(아래, 자동화 창)
 */
public class CalendarPagePanel extends JPanel {

    private static final Color TOOLBAR_BG = new Color(0xFFFFFF);

    private final JButton prevButton   = new JButton("◀");
    private final JButton todayButton  = new JButton("오늘");
    private final JButton nextButton   = new JButton("▶");
    private final JButton viewToggle   = new JButton("월간 보기");
    private final JLabel  dateLabel    = new JLabel("―");

    private final PresetBlockPanel       presetPanel = new PresetBlockPanel();
    private final CalendarGridPanel      gridPanel   = new CalendarGridPanel();
    private final CalendarAutomationPanel automationPanel = new CalendarAutomationPanel();

    public CalendarPagePanel() {
        setLayout(new BorderLayout());

        // 우측: 그리드(위) / 자동화 창(아래) 세로 분할
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, gridPanel, automationPanel);
        rightSplit.setResizeWeight(0.72);     // 그리드가 더 크게
        rightSplit.setDividerSize(6);
        rightSplit.setBorder(null);

        JPanel rightArea = new JPanel(new BorderLayout());
        rightArea.add(buildToolbar(), BorderLayout.NORTH);
        rightArea.add(rightSplit, BorderLayout.CENTER);

        add(presetPanel, BorderLayout.WEST);
        add(rightArea, BorderLayout.CENTER);
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

    // ===== 표시 갱신 =====
    public void updateDateLabel(String text)         { dateLabel.setText(text); }
    public void updateViewToggleLabel(ViewMode mode) {
        viewToggle.setText(mode == ViewMode.WEEK ? "월간 보기" : "주간 보기");
    }

    public void setViewMode(ViewMode mode)              { gridPanel.setViewMode(mode); }
    public void setWeekBaseDate(LocalDate date)         { gridPanel.setWeekBaseDate(date); }
    public void setMonthYearMonth(int year, int month)  { gridPanel.setMonthYearMonth(year, month); }
    public void refresh(List<ScheduleBlock> blocks)     { gridPanel.refresh(blocks); }
    public void setPresets(List<PresetBlock> presets)   {
        presetPanel.setPresets(presets);
        automationPanel.setPresets(presets); // 자동화 콤보도 같은 목록 사용
    }

    // ===== 캘린더 콜백 =====
    public void setOnPrev(Runnable r)      { prevButton.addActionListener(e -> r.run()); }
    public void setOnNext(Runnable r)      { nextButton.addActionListener(e -> r.run()); }
    public void setOnToday(Runnable r)     { todayButton.addActionListener(e -> r.run()); }
    public void setOnViewToggle(Runnable r){ viewToggle.addActionListener(e -> r.run()); }
    public void setOnBlockClick(Consumer<ScheduleBlock> cb) { gridPanel.setOnBlockClick(cb); }
    public void setOnDateClick(Consumer<LocalDate> cb)      { gridPanel.setOnDateClick(cb); }

    // ===== 미리블록 콜백 =====
    public void setOnPresetCreate(Runnable r)               { presetPanel.setOnCreate(r); }
    public void setOnPresetPlace(Consumer<PresetBlock> cb)  { presetPanel.setOnPlace(cb); }
    public void setOnPresetDelete(Consumer<PresetBlock> cb) { presetPanel.setOnDelete(cb); }

    // ===== 자동화 위임 =====
    public void setAutomationRules(List<CalendarAutomationRule> rules) {
        automationPanel.setRules(rules);
    }
    public void setOnAutomationAdd(Runnable r)                          { automationPanel.setOnAdd(r); }
    public void setOnAutomationRuleChange(Consumer<CalendarAutomationRule> cb) {
        automationPanel.setOnRuleChange(cb);
    }
    public void setOnAutomationRun(Consumer<CalendarAutomationRule> cb) { automationPanel.setOnRun(cb); }
    public void setOnAutomationDelete(Consumer<CalendarAutomationRule> cb) {
        automationPanel.setOnDelete(cb);
    }

    public WeekViewPanel getWeekViewPanel() { return gridPanel.getWeekViewPanel(); }
    public CalendarGridPanel getGridPanel() { return gridPanel; }
    public PresetBlockPanel  getPresetPanel() { return presetPanel; }
    public CalendarAutomationPanel getAutomationPanel() { return automationPanel; }
}
