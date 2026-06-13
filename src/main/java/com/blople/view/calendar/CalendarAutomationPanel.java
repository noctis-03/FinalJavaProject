package com.blople.view.calendar;

import com.blople.dnd.BlockTransferable;
import com.blople.model.automation.CalendarAutomationRule;
import com.blople.model.block.PresetBlock;
import com.blople.model.enums.BlockType;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 캘린더 페이지 하단 자동화 창 (FR-12).
 *
 * <p>문장형 카드: "[이번 주/다음 주]의 [요일▼]요일 [시▼]시에 [미리블록 드롭 슬롯] 일정을 추가한다 [▶실행] [✕]".
 * 미리블록은 좌측 미리블록 패널에서 드래그하여 슬롯에 넣는다.
 */
public class CalendarAutomationPanel extends JPanel {

    private static final String[] DAYS  = {"일", "월", "화", "수", "목", "금", "토"};
    private static final String[] WEEKS = {"이번 주", "다음 주"};
    private static final int HOUR_START = 8;
    private static final int HOUR_END   = 22;

    private static final Color HEADER_BG = new Color(0xF7F7F9);
    private static final Color CARD_BG   = new Color(0xFFFFFF);
    private static final Color BORDER    = new Color(0xE0E0E0);
    private static final Color SLOT_EMPTY_BG = new Color(0xF0ECF8);
    private static final Color SLOT_FILL_BG  = new Color(0xE7DEFb);

    private final JButton addButton = new JButton("＋ 자동화 추가");
    private final JPanel listContainer = new JPanel();

    private List<PresetBlock> presets = new ArrayList<>();

    private Runnable onAdd;
    private Consumer<CalendarAutomationRule> onRuleChange;
    private Consumer<CalendarAutomationRule> onRun;
    private Consumer<CalendarAutomationRule> onDelete;

    public CalendarAutomationPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));

        add(buildHeader(), BorderLayout.NORTH);

        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(new Color(0xFBF3EE));
        JScrollPane sp = new JScrollPane(listContainer);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        add(sp, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        p.setBackground(HEADER_BG);
        JLabel title = new JLabel("⚙ 자동화");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        addButton.setFocusPainted(false);
        addButton.addActionListener(e -> { if (onAdd != null) onAdd.run(); });
        p.add(title);
        p.add(addButton);
        return p;
    }

    public void setPresets(List<PresetBlock> presets) {
        this.presets = (presets != null) ? presets : new ArrayList<>();
    }

    public void setRules(List<CalendarAutomationRule> rules) {
        listContainer.removeAll();
        if (rules == null || rules.isEmpty()) {
            JLabel empty = new JLabel("자동화가 없습니다. ‘＋ 자동화 추가’로 만들어 보세요.");
            empty.setForeground(new Color(0x999999));
            empty.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            listContainer.add(empty);
        } else {
            for (CalendarAutomationRule r : rules) {
                listContainer.add(buildCard(r));
                listContainer.add(javax.swing.Box.createVerticalStrut(6));
            }
        }
        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel buildCard(CalendarAutomationRule rule) {
        JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        // 주 콤보
        JComboBox<String> weekCombo = new JComboBox<>(WEEKS);
        weekCombo.setSelectedIndex(rule.getWeekOffset() == 0 ? 0 : 1);
        weekCombo.addActionListener(e -> {
            rule.setWeekOffset(weekCombo.getSelectedIndex()); // 0=이번 주, 1=다음 주
            fireChange(rule);
        });
        card.add(weekCombo);
        card.add(plain("의"));

        // 요일 콤보
        JComboBox<String> dayCombo = new JComboBox<>(DAYS);
        dayCombo.setSelectedIndex((rule.getDayOfWeek() >= 0 && rule.getDayOfWeek() <= 6)
                ? rule.getDayOfWeek() : -1);
        dayCombo.addActionListener(e -> {
            rule.setDayOfWeek(dayCombo.getSelectedIndex());
            fireChange(rule);
        });
        card.add(dayCombo);
        card.add(plain("요일"));

        // 시각 콤보
        JComboBox<Integer> hourCombo = new JComboBox<>();
        for (int h = HOUR_START; h <= HOUR_END; h++) hourCombo.addItem(h);
        hourCombo.setSelectedItem(clampHour(rule.getStartHour()));
        hourCombo.addActionListener(e -> {
            Integer h = (Integer) hourCombo.getSelectedItem();
            if (h != null) { rule.setStartHour(h); fireChange(rule); }
        });
        card.add(hourCombo);
        card.add(plain("시에"));

        // 미리블록 드롭 슬롯
        card.add(buildPresetSlot(rule));
        card.add(plain("일정을 추가한다"));

        // 실행
        JButton run = new JButton("▶ 실행");
        run.setFocusPainted(false);
        run.addActionListener(e -> { if (onRun != null) onRun.accept(rule); });
        card.add(run);

        // 삭제
        JButton del = new JButton("✕");
        del.setFocusPainted(false);
        del.setMargin(new java.awt.Insets(1, 6, 1, 6));
        del.addActionListener(e -> { if (onDelete != null) onDelete.accept(rule); });
        card.add(del);

        return card;
    }

    /** 미리블록을 끌어다 놓는 슬롯. 드롭되면 rule.presetBlockId 설정 후 저장·재렌더. */
    private JPanel buildPresetSlot(CalendarAutomationRule rule) {
        JPanel slot = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        boolean filled = rule.getPresetBlockId() != null && !rule.getPresetBlockId().isBlank();
        slot.setBackground(filled ? SLOT_FILL_BG : SLOT_EMPTY_BG);
        slot.setBorder(BorderFactory.createDashedBorder(new Color(0x9C6BFF), 1.5f, 4, 3, true));
        slot.setPreferredSize(new Dimension(150, 30));

        JLabel label = new JLabel(filled
                ? "📦 " + presetName(rule.getPresetBlockId())
                : "여기에 미리블록 드롭");
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(filled ? new Color(0x333333) : new Color(0x9C6BFF));
        slot.add(label);

        // 드롭 타겟: PRESET 블록만 수용
        new DropTarget(slot, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override public void dragOver(DropTargetDragEvent dtde) {
                if (dtde.isDataFlavorSupported(BlockTransferable.BLOCK_FLAVOR)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                } else {
                    dtde.rejectDrag();
                }
            }
            @Override public void drop(DropTargetDropEvent dtde) {
                try {
                    if (!dtde.isDataFlavorSupported(BlockTransferable.BLOCK_FLAVOR)) {
                        dtde.rejectDrop();
                        return;
                    }
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Object data = dtde.getTransferable()
                            .getTransferData(BlockTransferable.BLOCK_FLAVOR);
                    if (data instanceof BlockTransferable bt
                            && bt.getBlockType() == BlockType.PRESET) {
                        rule.setPresetBlockId(bt.getBlockId());
                        fireChange(rule);
                        // 슬롯 표시 갱신을 위해 카드 목록 재렌더 (단순)
                        java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(CalendarAutomationPanel.this);
                        if (w != null) w.repaint();
                        refreshSlotLabel(label, rule);
                        slot.setBackground(SLOT_FILL_BG);
                        slot.revalidate();
                        slot.repaint();
                        dtde.dropComplete(true);
                    } else {
                        dtde.dropComplete(false);
                    }
                } catch (Exception ex) {
                    dtde.dropComplete(false);
                }
            }
        }, true);

        // 슬롯 우클릭/클릭으로 비우기(선택적): 채워져 있으면 클릭 시 해제
        if (filled) {
            slot.setToolTipText("클릭하면 비웁니다");
            slot.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    rule.setPresetBlockId(null);
                    fireChange(rule);
                    refreshSlotLabel(label, rule);
                    slot.setBackground(SLOT_EMPTY_BG);
                    slot.revalidate();
                    slot.repaint();
                }
            });
        }

        return slot;
    }

    private void refreshSlotLabel(JLabel label, CalendarAutomationRule rule) {
        boolean filled = rule.getPresetBlockId() != null && !rule.getPresetBlockId().isBlank();
        label.setText(filled ? "📦 " + presetName(rule.getPresetBlockId()) : "여기에 미리블록 드롭");
        label.setForeground(filled ? new Color(0x333333) : new Color(0x9C6BFF));
    }

    private String presetName(String presetId) {
        for (PresetBlock p : presets) {
            if (p.getId().equals(presetId)) return p.getName();
        }
        return "(삭제된 미리블록)";
    }

    private int clampHour(int h) {
        if (h < HOUR_START) return HOUR_START;
        if (h > HOUR_END)   return HOUR_END;
        return h;
    }

    private void fireChange(CalendarAutomationRule rule) {
        if (onRuleChange != null) onRuleChange.accept(rule);
    }

    private JLabel plain(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return l;
    }

    public void setOnAdd(Runnable cb)                              { this.onAdd = cb; }
    public void setOnRuleChange(Consumer<CalendarAutomationRule> cb){ this.onRuleChange = cb; }
    public void setOnRun(Consumer<CalendarAutomationRule> cb)      { this.onRun = cb; }
    public void setOnDelete(Consumer<CalendarAutomationRule> cb)   { this.onDelete = cb; }
}
