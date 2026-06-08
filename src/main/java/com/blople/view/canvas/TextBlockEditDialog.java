package com.blople.view.canvas;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Window;
import java.util.function.BiConsumer;

/**
 * 텍스트 블록 편집 다이얼로그 (FR-06).
 */
public class TextBlockEditDialog extends JDialog {

    private final JTextField titleField = new JTextField();
    private final JTextArea contentArea = new JTextArea(8, 24);

    private BiConsumer<String, String> onSave; // (title, content)

    public TextBlockEditDialog(Window owner, String title, String content) {
        super(owner, "메모 편집", ModalityType.APPLICATION_MODAL);
        setSize(360, 320);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 8));

        titleField.setText(title == null ? "" : title);
        contentArea.setText(content == null ? "" : content);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);

        JPanel top = new JPanel(new BorderLayout(4, 0));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        top.add(new JLabel("제목"), BorderLayout.WEST);
        top.add(titleField, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(contentArea);
        sp.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
        add(sp, BorderLayout.CENTER);

        JPanel btns = new JPanel();
        btns.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        JButton save = new JButton("저장");
        JButton cancel = new JButton("취소");
        save.addActionListener(e -> {
            if (onSave != null) onSave.accept(titleField.getText().trim(), contentArea.getText());
            dispose();
        });
        cancel.addActionListener(e -> dispose());
        btns.add(save);
        btns.add(cancel);
        add(btns, BorderLayout.SOUTH);
    }

    public void setOnSave(BiConsumer<String, String> cb) { this.onSave = cb; }
}
