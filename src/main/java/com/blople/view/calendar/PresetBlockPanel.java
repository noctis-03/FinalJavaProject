package com.blople.view.calendar;

import com.blople.dnd.BlockDragSource;
import com.blople.model.block.PresetBlock;
import com.blople.model.enums.BlockType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.function.Consumer;

/**
 * 캘린더 페이지 좌측 미리블록 패널 (FR-02).
 *
 * <p>미리블록 템플릿 목록을 카드로 표시하고, "＋ 미리블록" 으로 생성한다.
 * 각 카드는 캘린더 주간 뷰로 <b>드래그</b>하여 일정으로 배치할 수 있고(FR-11),
 * "배치" 버튼으로도 (현재 날짜 09:00) 배치할 수 있다.
 */
public class PresetBlockPanel extends JPanel {

    private final JButton createBtn = new JButton("＋ 미리블록");
    private final JPanel listContainer = new JPanel();

    private Runnable onCreate;
    private Consumer<PresetBlock> onPlace;
    private Consumer<PresetBlock> onDelete;

    public PresetBlockPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xDDDDDD)));

        add(buildHeader(), BorderLayout.NORTH);

        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(Color.WHITE);
        JScrollPane sp = new JScrollPane(listContainer);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        add(sp, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        p.setBackground(new Color(0xF7F7F9));
        JLabel title = new JLabel("미리블록");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        createBtn.setFocusPainted(false);
        createBtn.addActionListener(e -> {
            if (onCreate != null) onCreate.run();
        });
        p.add(title);
        p.add(createBtn);
        return p;
    }

    public void setPresets(List<PresetBlock> presets) {
        listContainer.removeAll();
        if (presets.isEmpty()) {
            JLabel empty = new JLabel("<html><div style='color:#aaa;'>미리블록을<br>추가해보세요</div></html>");
            empty.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
            listContainer.add(empty);
        } else {
            for (PresetBlock p : presets) {
                listContainer.add(buildCard(p));
                listContainer.add(Box.createVerticalStrut(6));
            }
        }
        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel buildCard(PresetBlock p) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        Color color = BlockRenderer.parseColor(p.getColor(), new Color(0x4D96FF));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, color),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xE0E0E0)),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8))));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ★ 드래그 소스 등록 — 카드를 끌면 이 미리블록 id 를 운반한다 (FR-11)
        new BlockDragSource(card, p::getId, BlockType.PRESET);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        JLabel name = new JLabel(p.getName());
        name.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel meta = new JLabel(p.getDurationMinutes() + "분"
                + (p.getCategory() != null ? "  ·  " + p.getCategory().getDisplayName() : ""));
        meta.setFont(new Font("SansSerif", Font.PLAIN, 11));
        meta.setForeground(new Color(0x888888));
        info.add(name);
        info.add(meta);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btns.setOpaque(false);
        JButton place = new JButton("배치");
        place.setFocusPainted(false);
        place.setFont(new Font("SansSerif", Font.PLAIN, 11));
        place.addActionListener(e -> {
            if (onPlace != null) onPlace.accept(p);
        });
        JButton del = new JButton("✕");
        del.setFocusPainted(false);
        del.setFont(new Font("SansSerif", Font.PLAIN, 11));
        del.addActionListener(e -> {
            if (onDelete != null) onDelete.accept(p);
        });
        btns.add(place);
        btns.add(del);

        card.add(info, BorderLayout.CENTER);
        card.add(btns, BorderLayout.SOUTH);
        return card;
    }

    // 콜백 주입
    public void setOnCreate(Runnable cb)              { this.onCreate = cb; }
    public void setOnPlace(Consumer<PresetBlock> cb)  { this.onPlace = cb; }
    public void setOnDelete(Consumer<PresetBlock> cb) { this.onDelete = cb; }
}
