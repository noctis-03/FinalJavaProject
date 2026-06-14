package com.blople.view.canvas;

import com.blople.common.Constants;
import com.blople.dnd.BlockDragSource;
import com.blople.model.enums.BlockType;
import com.blople.model.subject.Subject;
import com.blople.view.calendar.BlockRenderer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * 무한 캔버스 상단 헤더.
 *
 * <p>좌측: 현재 선택된 과목 표시(컬러 닷 + 이름).
 * <p>우측: 메모/링크 추가용 드래그 카드 + 캔버스 액션 버튼(중심으로 / 비우기).
 */
public class SubjectCanvasHeaderPanel extends JPanel {

    private static final Color BG          = new Color(0xFAFAFC);
    private static final Color BORDER      = new Color(0xE0E0E6);
    private static final Color TEXT_PALETTE = new Color(0xFFF6C2); // 메모 카드 배경
    private static final Color LINK_PALETTE = new Color(0xCFE3FF); // 링크 카드 배경
    private static final Color DOT_DEFAULT  = new Color(0x4D96FF);
    private static final Color TITLE_COLOR  = new Color(0x1F2937);
    private static final Color MUTED        = new Color(0x6B7280);

    private final JLabel dotLabel     = new JLabel("●");
    private final JLabel subjectLabel = new JLabel("과목을 선택하세요");

    private final JButton recenterBtn = new JButton("⌂ 중심으로");
    private final JButton clearBtn    = new JButton("🗑 캔버스 비우기");

    private Runnable onRecenterClick;
    private Runnable onClearCanvasClick;

    public SubjectCanvasHeaderPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        setPreferredSize(new Dimension(0, 56));

        add(buildLeft(),  BorderLayout.WEST);
        add(buildRight(), BorderLayout.EAST);
    }

    private JPanel buildLeft() {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        left.setOpaque(false);

        dotLabel.setForeground(DOT_DEFAULT);
        dotLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        subjectLabel.setForeground(TITLE_COLOR);
        subjectLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        left.add(dotLabel);
        left.add(subjectLabel);
        return left;
    }

    private JPanel buildRight() {
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        right.setOpaque(false);

        right.add(buildPaletteCard("📝 메모", TEXT_PALETTE, BlockType.TEXT));
        right.add(buildPaletteCard("🔗 링크", LINK_PALETTE, BlockType.LINK));

        right.add(buildDivider());

        styleActionButton(recenterBtn);
        recenterBtn.setToolTipText("캔버스 화면을 (0,0) 중심으로 되돌립니다");
        recenterBtn.addActionListener(e -> {
            if (onRecenterClick != null) onRecenterClick.run();
        });

        styleActionButton(clearBtn);
        clearBtn.setToolTipText("이 과목의 캔버스 배치를 모두 비웁니다");
        clearBtn.addActionListener(e -> {
            if (onClearCanvasClick != null) onClearCanvasClick.run();
        });

        right.add(recenterBtn);
        right.add(clearBtn);
        return right;
    }

    private JPanel buildDivider() {
        JPanel d = new JPanel();
        d.setPreferredSize(new Dimension(1, 24));
        d.setBackground(new Color(0xE0E0E6));
        return d;
    }

    private JPanel buildPaletteCard(String label, Color bg, BlockType type) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(0, 0, 0, 18));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 4));
        card.setPreferredSize(new Dimension(78, 32));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        card.setToolTipText("캔버스로 드래그하여 " + (type == BlockType.TEXT ? "메모" : "링크") + " 블록을 만듭니다");

        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(new Color(0x374151));
        card.add(l);

        // 헤더 카드도 드래그 소스로 — block_id 는 빈 문자열(=새 블록 생성)로 보낸다.
        new BlockDragSource(card, () -> "", type);
        new BlockDragSource(l, () -> "", type);
        return card;
    }

    private void styleActionButton(JButton b) {
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setOpaque(true);
        b.setBackground(Color.WHITE);
        b.setForeground(new Color(0x334155));
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD6E0EE), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** 헤더에 현재 선택된 과목을 표시 (없으면 null). */
    public void showSubject(Subject subject) {
        if (subject == null) {
            dotLabel.setForeground(DOT_DEFAULT);
            subjectLabel.setText("과목을 선택하세요");
            subjectLabel.setForeground(MUTED);
            return;
        }
        Color c = BlockRenderer.parseColor(subject.getColor(), DOT_DEFAULT);
        dotLabel.setForeground(c);
        subjectLabel.setText(subject.getName());
        subjectLabel.setForeground(TITLE_COLOR);
    }

    /** 캔버스 "중심으로" 버튼 콜백. */
    public void setOnRecenterClick(Runnable cb) {
        this.onRecenterClick = cb;
    }

    /** 캔버스 "비우기" 버튼 콜백. */
    public void setOnClearCanvasClick(Runnable cb) {
        this.onClearCanvasClick = cb;
    }

    /** Constants 사용 보장용 (팔레트 변경 시 자동 import 유지). */
    @SuppressWarnings("unused")
    private static int paletteSize() {
        return Constants.BLOCK_COLOR_PALETTE.length;
    }
}
