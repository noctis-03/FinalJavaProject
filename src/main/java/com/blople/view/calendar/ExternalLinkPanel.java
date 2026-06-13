package com.blople.view.calendar;

import com.blople.model.ExternalLink;
import com.blople.service.ExternalLinkService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * 캘린더 페이지 우측 "외부 링크" 패널.
 *
 * <p>대학 홈페이지, e-Class 등 자주 쓰는 외부 URL 을 카드로 표시한다.
 * 카드 클릭 → 시스템 기본 브라우저로 열기.
 * "＋ 링크 추가" 버튼 → 사용자가 원하는 URL 을 등록.
 * 각 카드의 ✎(수정)/✕(삭제) 버튼으로 편집 가능.
 */
public class ExternalLinkPanel extends JPanel {

    private static final Color BG_HEADER = new Color(0xF7F7F9);
    private static final Color CARD_BG   = Color.WHITE;
    private static final Color CARD_BORDER = new Color(0xE0E0E6);
    private static final Color CARD_HOVER  = new Color(0xF4F8FF);
    private static final Color ACCENT      = new Color(0x4D96FF);

    private final JButton addBtn = new JButton("＋ 링크 추가");
    private final JPanel listContainer = new JPanel();

    private final ExternalLinkService service;

    public ExternalLinkPanel(ExternalLinkService service) {
        this.service = service;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(220, 0));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(0xDDDDDD)));

        add(buildHeader(), BorderLayout.NORTH);

        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(Color.WHITE);
        JScrollPane sp = new JScrollPane(listContainer);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        add(sp, BorderLayout.CENTER);

        reload();
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        p.setBackground(BG_HEADER);

        JLabel title = new JLabel("🔗 외부 링크");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));

        addBtn.setFocusPainted(false);
        addBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        addBtn.addActionListener(e -> onAdd());

        p.add(title);
        p.add(addBtn);
        return p;
    }

    public void reload() {
        listContainer.removeAll();
        List<ExternalLink> all = service.getAll();
        if (all.isEmpty()) {
            JLabel empty = new JLabel(
                    "<html><div style='color:#aaa; padding:10px;'>"
                            + "자주 쓰는 사이트를<br>+ 버튼으로 추가해보세요.</div></html>");
            listContainer.add(empty);
        } else {
            for (ExternalLink link : all) {
                listContainer.add(buildCard(link));
                listContainer.add(Box.createVerticalStrut(6));
            }
        }
        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel buildCard(ExternalLink link) {
        JPanel card = new JPanel(new BorderLayout(4, 0));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, ACCENT),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(CARD_BORDER),
                        BorderFactory.createEmptyBorder(6, 8, 6, 6))));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel name = new JLabel("🔗 " + link.getName());
        name.setFont(new Font("SansSerif", Font.BOLD, 12));
        name.setForeground(new Color(0x222831));

        JLabel url = new JLabel(shortenUrl(link.getUrl()));
        url.setFont(new Font("SansSerif", Font.PLAIN, 10));
        url.setForeground(new Color(0x7A7F8A));

        text.add(name);
        text.add(url);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        btns.setOpaque(false);
        JButton edit = miniButton("✎");
        edit.setToolTipText("수정");
        edit.addActionListener(e -> onEdit(link));
        JButton del = miniButton("✕");
        del.setToolTipText("삭제");
        del.addActionListener(e -> onDelete(link));
        btns.add(edit);
        btns.add(del);

        card.add(text, BorderLayout.CENTER);
        card.add(btns, BorderLayout.EAST);

        // 카드 본체 클릭 → URL 열기 (버튼 클릭은 이벤트 소비됨)
        MouseAdapter clickToOpen = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openLink(link); }
            @Override public void mouseEntered(MouseEvent e) { card.setBackground(CARD_HOVER); }
            @Override public void mouseExited(MouseEvent e)  { card.setBackground(CARD_BG); }
        };
        card.addMouseListener(clickToOpen);
        text.addMouseListener(clickToOpen);
        name.addMouseListener(clickToOpen);
        url.addMouseListener(clickToOpen);

        return card;
    }

    private JButton miniButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setMargin(new java.awt.Insets(1, 4, 1, 4));
        b.setFont(new Font("SansSerif", Font.PLAIN, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentY(Component.CENTER_ALIGNMENT);
        return b;
    }

    private void openLink(ExternalLink link) {
        try {
            service.open(link);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                    "링크를 열 수 없습니다: " + ex.getMessage(),
                    "오류", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onAdd() {
        ExternalLinkEditDialog d = new ExternalLinkEditDialog(
                SwingUtilities.getWindowAncestor(this), "외부 링크 추가", "", "");
        d.setOnSave((name, url) -> {
            try {
                service.create(name, url);
                reload();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "추가 실패", JOptionPane.WARNING_MESSAGE);
            }
        });
        d.setVisible(true);
    }

    private void onEdit(ExternalLink link) {
        ExternalLinkEditDialog d = new ExternalLinkEditDialog(
                SwingUtilities.getWindowAncestor(this),
                "외부 링크 수정", link.getName(), link.getUrl());
        d.setOnSave((name, url) -> {
            try {
                service.update(link, name, url);
                reload();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "수정 실패", JOptionPane.WARNING_MESSAGE);
            }
        });
        d.setVisible(true);
    }

    private void onDelete(ExternalLink link) {
        int ok = JOptionPane.showConfirmDialog(this,
                "'" + link.getName() + "' 링크를 삭제할까요?",
                "링크 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        service.delete(link.getId());
        reload();
    }

    private static String shortenUrl(String url) {
        if (url == null) return "";
        String s = url.replaceFirst("(?i)^https?://", "");
        if (s.length() > 28) s = s.substring(0, 27) + "…";
        return s;
    }
}
