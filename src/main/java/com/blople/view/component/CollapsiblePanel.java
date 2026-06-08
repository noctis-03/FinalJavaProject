package com.blople.view.component;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 펼침/접힘 가능한 재사용 패널 (좌측 2단 아코디언용).
 *
 * <p>BoxLayout 안에서 세로로 늘어나지 않도록 getMaximumSize 를 내용 높이로 제한한다.
 * 들여쓰기 레벨(level)로 1단(과목)/2단(과제·파일)의 시각적 위계를 준다.
 */
public class CollapsiblePanel extends JPanel {

    private static final Color HEADER_BG_L0 = new Color(0xEDF1F7); // 과목
    private static final Color HEADER_BG_L1 = new Color(0xF6F7F9); // 과제/파일
    private static final Color BORDER_COLOR = new Color(0xE0E0E6);

    private final JButton toggleBtn = new JButton();
    private final JLabel titleLabel = new JLabel();
    private final JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
    private final JPanel content = new JPanel();
    private final int level;

    private boolean expanded;
    private Runnable onHeaderClick;

    public CollapsiblePanel(String title, boolean expanded) {
        this(title, expanded, 0);
    }

    /** @param level 0 = 1단(과목), 1 = 2단(과제/파일). 들여쓰기·배경에 반영. */
    public CollapsiblePanel(String title, boolean expanded, int level) {
        this.expanded = expanded;
        this.level = level;

        setLayout(new BorderLayout());
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setOpaque(false);

        add(buildHeader(title), BorderLayout.NORTH);

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setVisible(expanded);
        add(content, BorderLayout.CENTER);

        updateToggleIcon();
    }

    private JPanel buildHeader(String title) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(level == 0 ? HEADER_BG_L0 : HEADER_BG_L1);
        // 아래쪽 구분선 + 좌측 들여쓰기
        int indent = (level == 0) ? 6 : 18;
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(0, indent, 0, 4)));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        toggleBtn.setBorderPainted(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        toggleBtn.setForeground(new Color(0x888888));
        toggleBtn.setPreferredSize(new Dimension(16, 22));
        toggleBtn.addActionListener(e -> toggle());

        titleLabel.setText(title);
        titleLabel.setFont(new Font("SansSerif",
                level == 0 ? Font.BOLD : Font.PLAIN,
                level == 0 ? 13 : 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, level == 0 ? 6 : 3));
        left.setOpaque(false);
        left.add(toggleBtn);
        left.add(titleLabel);

        headerRight.setOpaque(false);

        header.add(left, BorderLayout.CENTER);
        header.add(headerRight, BorderLayout.EAST);

        MouseAdapter click = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (onHeaderClick != null) onHeaderClick.run();
            }
        };
        header.addMouseListener(click);
        titleLabel.addMouseListener(click);
        return header;
    }

    private void toggle() { setExpanded(!expanded); }

    public void setExpanded(boolean exp) {
        this.expanded = exp;
        content.setVisible(exp);
        updateToggleIcon();
        revalidate();
        repaint();
    }

    public boolean isExpanded() { return expanded; }

    private void updateToggleIcon() {
        toggleBtn.setText(expanded ? "▼" : "▶");
    }

    public void setTitle(String t)          { titleLabel.setText(t); }
    public void setTitleColor(Color c)      { titleLabel.setForeground(c); }
    public void addHeaderAction(Component c){ headerRight.add(c); }
    public void addContent(Component c)     { content.add(c); }

    public void clearContent() {
        content.removeAll();
        content.revalidate();
        content.repaint();
    }

    public JPanel getContentPanel() { return content; }
    public void setOnHeaderClick(Runnable cb) { this.onHeaderClick = cb; }

    /** ★ 핵심: BoxLayout 안에서 세로로 늘어나지 않도록 내용 높이로 제한. */
    @Override
    public Dimension getMaximumSize() {
        Dimension pref = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, pref.height);
    }
}
