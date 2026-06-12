package com.blople.view.canvas;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.function.BiConsumer;

/**
 * 링크 블록 생성·편집 다이얼로그 (FR-07).
 *
 * <p>URL(필수) + 표시 이름(선택)을 입력받아 (url, displayName) 콜백으로 넘긴다.
 * URL 에 스킴이 없으면 https:// 를 자동으로 보정한다.
 */
public class LinkBlockEditDialog extends JDialog {

    private final JTextField urlField  = new JTextField(20);
    private final JTextField nameField = new JTextField(20);

    private BiConsumer<String, String> onSave; // (url, displayName)

    /** 생성용 — 빈 입력. */
    public LinkBlockEditDialog(Window owner) {
        this(owner, "", "");
    }

    /** 편집용 — 기존 값 채움. */
    public LinkBlockEditDialog(Window owner, String url, String displayName) {
        super(owner, "링크 블록", ModalityType.APPLICATION_MODAL);
        setSize(380, 200);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 8));

        urlField.setText(url == null ? "" : url);
        nameField.setText(displayName == null ? "" : displayName);

        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
    }

    public void setOnSave(BiConsumer<String, String> cb) { this.onSave = cb; }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 4, 14));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        JLabel urlLabel = new JLabel("URL");
        urlLabel.setPreferredSize(new Dimension(64, 24));
        p.add(urlLabel, c);
        c.gridx = 1; c.weightx = 1;
        p.add(urlField, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        JLabel nameLabel = new JLabel("표시 이름");
        nameLabel.setPreferredSize(new Dimension(64, 24));
        p.add(nameLabel, c);
        c.gridx = 1; c.weightx = 1;
        p.add(nameField, c);

        return p;
    }

    private JPanel buildButtons() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createEmptyBorder(0, 14, 12, 14));
        JButton save = new JButton("저장");
        JButton cancel = new JButton("취소");
        save.addActionListener(e -> handleSave());
        cancel.addActionListener(e -> dispose());
        p.add(save);
        p.add(cancel);
        return p;
    }

    private void handleSave() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "URL을 입력해주세요.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // 스킴이 없으면 https:// 보정
        if (!url.matches("(?i)^https?://.*")) {
            url = "https://" + url;
        }
        String name = nameField.getText().trim();
        if (onSave != null) onSave.accept(url, name);
        dispose();
    }
}
