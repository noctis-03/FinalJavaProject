package com.blople.view.calendar;

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
 * 외부 링크 추가/수정 다이얼로그.
 * 이름(필수) + URL(필수) 을 입력받아 콜백으로 전달한다.
 */
public class ExternalLinkEditDialog extends JDialog {

    private final JTextField nameField;
    private final JTextField urlField;
    private BiConsumer<String, String> onSave;

    public ExternalLinkEditDialog(Window owner, String title,
                                  String initialName, String initialUrl) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setSize(380, 200);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 8));

        nameField = new JTextField(initialName == null ? "" : initialName, 20);
        urlField  = new JTextField(initialUrl  == null ? "" : initialUrl, 20);

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
        JLabel nameLabel = new JLabel("이름");
        nameLabel.setPreferredSize(new Dimension(48, 24));
        p.add(nameLabel, c);
        c.gridx = 1; c.weightx = 1;
        p.add(nameField, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        JLabel urlLabel = new JLabel("URL");
        urlLabel.setPreferredSize(new Dimension(48, 24));
        p.add(urlLabel, c);
        c.gridx = 1; c.weightx = 1;
        p.add(urlField, c);

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
        String name = nameField.getText().trim();
        String url  = urlField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "이름을 입력해주세요.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "URL 을 입력해주세요.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (onSave != null) onSave.accept(name, url);
        dispose();
    }
}
