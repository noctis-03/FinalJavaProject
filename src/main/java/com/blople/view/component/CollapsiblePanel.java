package com.blople.view.component;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 좌측 트리에서 사용하는 재사용형 접기/펼치기 패널.
 *
 * level 0 : 과목 카드
 * level 1 : 과제 / 파일 섹션
 */
public class CollapsiblePanel extends JPanel {

    private static final Color SUBJECT_CARD_BORDER = new Color(0xDDE5F0);
    private static final Color SUBJECT_HEADER_BG = new Color(0xF5F9FF);
    private static final Color SECTION_HEADER_BG = new Color(0xF8FAFD);
    private static final Color SECTION_BORDER = new Color(0xE7ECF3);
    private static final Color TITLE_COLOR = new Color(0x233142);
    private static final Color SUBTITLE_COLOR = new Color(0x5B6574);

    private final JButton toggleBtn = new JButton();
    private final JLabel titleLabel = new JLabel();
    private final JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    private final JPanel content = new JPanel();
    private final JPanel header = new JPanel(new BorderLayout());
    private final int level;

    private boolean expanded;
    private Runnable onHeaderClick;

    public CollapsiblePanel(String title, boolean expanded) {
        this(title, expanded, 0);
    }

    public CollapsiblePanel(String title, boolean expanded, int level) {
        this.expanded = expanded;
        this.level = level;

        setLayout(new BorderLayout());
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setOpaque(false);

        buildHeader(title);

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(true);
        content.setBackground(Color.WHITE);
        content.setVisible(expanded);
        add(content, BorderLayout.CENTER);

        applyLevelStyle();
        updateToggleIcon();
    }

    private void buildHeader(String title) {
        header.setOpaque(true);
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        toggleBtn.setBorderPainted(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setOpaque(false);
        toggleBtn.setHorizontalAlignment(SwingConstants.CENTER);
        toggleBtn.setMargin(new Insets(0, 0, 0, 0));
        toggleBtn.setForeground(new Color(0x697586));
        toggleBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
        toggleBtn.addActionListener(e -> {
            toggle();
            if (onHeaderClick != null) onHeaderClick.run();
        });

        titleLabel.setText(title);
        titleLabel.setForeground(TITLE_COLOR);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(toggleBtn);
        left.add(titleLabel);

        headerRight.setOpaque(false);

        header.add(left, BorderLayout.CENTER);
        header.add(headerRight, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        MouseAdapter click = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggle();
                if (onHeaderClick != null) onHeaderClick.run();
            }
        };
        header.addMouseListener(click);
        titleLabel.addMouseListener(click);
    }

    private void applyLevelStyle() {
        if (level == 0) {
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SUBJECT_CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(0, 0, 0, 0)
            ));
            header.setBackground(SUBJECT_HEADER_BG);
            header.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, SUBJECT_CARD_BORDER),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            titleLabel.setForeground(TITLE_COLOR);
            toggleBtn.setPreferredSize(new Dimension(18, 18));
            content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        } else {
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SECTION_BORDER, 1),
                    BorderFactory.createEmptyBorder(0, 0, 0, 0)
            ));
            header.setBackground(SECTION_HEADER_BG);
            header.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, SECTION_BORDER),
                    BorderFactory.createEmptyBorder(8, 10, 8, 8)
            ));
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            titleLabel.setForeground(SUBTITLE_COLOR);
            toggleBtn.setPreferredSize(new Dimension(16, 16));
            content.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));
        }
    }

    private void toggle() {
        setExpanded(!expanded);
    }

    public void setExpanded(boolean exp) {
        this.expanded = exp;
        content.setVisible(exp);
        updateToggleIcon();
        revalidate();
        repaint();
    }

    public boolean isExpanded() {
        return expanded;
    }

    private void updateToggleIcon() {
        toggleBtn.setText(expanded ? "⌄" : "›");
    }

    public void setTitle(String t) {
        titleLabel.setText(t);
    }

    public void setTitleColor(Color c) {
        titleLabel.setForeground(c);
    }

    public void addHeaderAction(Component c) {
        headerRight.add(c);
    }

    public void addContent(Component c) {
        content.add(c);
    }

    public void clearContent() {
        content.removeAll();
        content.revalidate();
        content.repaint();
    }

    public JPanel getContentPanel() {
        return content;
    }

    public void setOnHeaderClick(Runnable cb) {
        this.onHeaderClick = cb;
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension pref = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, pref.height);
    }
}
