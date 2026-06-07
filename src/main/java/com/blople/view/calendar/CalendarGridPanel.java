package com.blople.view.calendar;

import com.blople.model.block.ScheduleBlock;
import com.blople.model.enums.ViewMode;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * 주간/월간 뷰를 전환하는 컨테이너 (FR-01).
 *
 * <p>주간 뷰는 세로로 길어 스크롤이 필요하므로 JScrollPane 으로 감싼다.
 * 월간 뷰는 화면에 맞춰 그리므로 스크롤 없이 그대로 둔다.
 */
public class CalendarGridPanel extends JPanel {

    private static final String CARD_WEEK  = "WEEK";
    private static final String CARD_MONTH = "MONTH";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private final WeekViewPanel  weekViewPanel  = new WeekViewPanel();
    private final MonthViewPanel monthViewPanel = new MonthViewPanel();

    private ViewMode currentMode = ViewMode.WEEK;

    public CalendarGridPanel() {
        setLayout(new BorderLayout());

        JScrollPane weekScroll = new JScrollPane(weekViewPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        weekScroll.getVerticalScrollBar().setUnitIncrement(16);

        cards.add(weekScroll, CARD_WEEK);
        cards.add(monthViewPanel, CARD_MONTH);
        add(cards, BorderLayout.CENTER);

        setViewMode(ViewMode.WEEK);
    }

    public ViewMode getCurrentMode() { return currentMode; }

    public void setViewMode(ViewMode mode) {
        this.currentMode = mode;
        cardLayout.show(cards, mode == ViewMode.WEEK ? CARD_WEEK : CARD_MONTH);
    }

    /** 주간 뷰 기준 날짜 설정. */
    public void setWeekBaseDate(LocalDate date) {
        weekViewPanel.setBaseDate(date);
    }

    /** 월간 뷰 연/월 설정. */
    public void setMonthYearMonth(int year, int month) {
        monthViewPanel.setYearMonth(year, month);
    }

    /** 현재 활성 뷰에 블록 데이터를 반영. */
    public void refresh(List<ScheduleBlock> blocks) {
        if (currentMode == ViewMode.WEEK) {
            weekViewPanel.setBlocks(blocks);
        } else {
            monthViewPanel.setBlocks(blocks);
        }
    }

    // ===== 콜백 위임 =====
    public void setOnBlockClick(Consumer<ScheduleBlock> cb) {
        weekViewPanel.setOnBlockClick(cb);
    }

    public void setOnDateClick(Consumer<LocalDate> cb) {
        monthViewPanel.setOnDateClick(cb);
    }

    public WeekViewPanel  getWeekViewPanel()  { return weekViewPanel; }
    public MonthViewPanel getMonthViewPanel() { return monthViewPanel; }
}
