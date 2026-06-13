package com.blople.view.calendar;

import com.blople.model.block.ScheduleBlock;
import com.blople.model.enums.ViewMode;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

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

    public void setWeekBaseDate(LocalDate date) {
        weekViewPanel.setBaseDate(date);
    }

    public void setMonthYearMonth(int year, int month) {
        monthViewPanel.setYearMonth(year, month);
    }

    public void refresh(List<ScheduleBlock> blocks) {
        if (currentMode == ViewMode.WEEK) {
            weekViewPanel.setBlocks(blocks);
        } else {
            monthViewPanel.setBlocks(blocks);
        }
    }

    public void setOnBlockClick(Consumer<ScheduleBlock> cb) {
        weekViewPanel.setOnBlockClick(cb);
    }

    public void setOnBlockDoubleClick(Consumer<ScheduleBlock> cb) {
        weekViewPanel.setOnBlockDoubleClick(cb);
    }

    public void setOnBlockDelete(Consumer<ScheduleBlock> cb) {
        weekViewPanel.setOnBlockDelete(cb);
    }

    public void setOnBlockMove(WeekViewPanel.BlockMoveListener cb) {
        weekViewPanel.setOnBlockMove(cb);
    }

    public void setOnEmptySlotDoubleClick(Consumer<LocalDateTime> cb) {
        weekViewPanel.setOnEmptySlotDoubleClick(cb);
    }

    public void setOnDateClick(Consumer<LocalDate> cb) {
        monthViewPanel.setOnDateClick(cb);
    }

    public WeekViewPanel  getWeekViewPanel()  { return weekViewPanel; }
    public MonthViewPanel getMonthViewPanel() { return monthViewPanel; }
}
