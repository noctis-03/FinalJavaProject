package com.blople.view.canvas;

import com.blople.dnd.BlockDragSource;
import com.blople.model.enums.BlockType;
import com.blople.model.subject.Subject;
import com.blople.view.calendar.BlockRenderer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * 무한 캔버스 상단 헤더 (FR-07, FR-08, FR-11).
 *
 * <p>좌측: 현재 과목 표시(색상 점 + 이름) — 탭 형태.
 * <p>우측: 블록 팔레트(메모/링크) — 캔버스로 드래그하여 생성.
 */
public class SubjectCanvasHeaderPanel extends JPanel {

    private static final Color BG          = new Color(0xF7F7F9);
    private static final Color TAB_BG      = new Color(0xFFFFFF);
    private static final Color BORDER      = new Color(0xDDDDDD);
    private static final Color TEXT_PALETTE = new Color(0xFFF3B0);
    private static final Color LINK_PALETTE = new Color(0xEAF2FF);

    private final JLabel dot       = new JLabel("●");
    private final JLabel subjectName = new JLabel("과목을 선택하세요");

    public SubjectCanvasHeaderPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        setPreferredSize(new Dimension(0, 48));

        add(buildSubjectTab(), BorderLayout.WEST);
        add(buildPalette(), BorderLayout.EAST);
    }

    private JPanel buildSubjectTab() {
        JPanel tab = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        tab.setBackground(TAB_BG);
        tab.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER),
                BorderFactory.createEmptyBorder(2, 12, 2, 16)));

        dot.setForeground(new Color(0xBBBBBB));
        subjectName.setFont(new Font("SansSerif", Font.BOLD, 14));
        subjectName.setForeground(new Color(0x999999));

        tab.add(dot);
        tab.add(subjectName);
        return tab;
    }

    private JPanel buildPalette() {
        JPanel palette = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        palette.setOpaque(false);

        JPanel memoCard = buildPaletteCard("📝 메모", TEXT_PALETTE, BlockType.TEXT);
        JPanel linkCard = buildPaletteCard("🔗 링크", LINK_PALETTE, BlockType.LINK);

        palette.add(new JLabel("끌어서 추가:"));
        palette.add(memoCard);
        palette.add(linkCard);
        return palette;
    }

    private JPanel buildPaletteCard(String text, Color bg, BlockType type) {
        JPanel card = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createLineBorder(new Color(0xCCCCCC)));
        card.setPreferredSize(new Dimension(80, 32));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        card.add(label);

        // 팔레트 드래그 — 종류만 운반(id 는 더미). 드롭 측은 BlockType 만 사용.
        new BlockDragSource(card, () -> "palette", type);

        return card;
    }

    /** 과목 선택 시 호출 — 헤더의 과목 표시 갱신. */
    public void showSubject(Subject subject) {
        if (subject == null) {
            dot.setForeground(new Color(0xBBBBBB));
            subjectName.setText("과목을 선택하세요");
            subjectName.setForeground(new Color(0x999999));
            return;
        }
        dot.setForeground(BlockRenderer.parseColor(subject.getColor(), new Color(0x4D96FF)));
        subjectName.setText(subject.getName());
        subjectName.setForeground(new Color(0x333333));
    }
}
