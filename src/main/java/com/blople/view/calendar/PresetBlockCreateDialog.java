package com.blople.view.calendar;

import com.blople.common.Constants;
import com.blople.model.enums.Category;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.function.Consumer;

/**
 * 미리블록 생성 다이얼로그 (FR-02).
 * 이름 / 소요시간(분) / 색상 / 카테고리를 입력받는다.
 */
public class PresetBlockCreateDialog extends JDialog {

    private final JTextField nameField = new JTextField(14);
    private final JSpinner durationSpinner =
            new JSpinner(new SpinnerNumberModel(60, 10, 600, 10));
    private final JComboBox<String> colorCombo = new JComboBox<>();
    private final JComboBox<Category> categoryCombo = new JComboBox<>(Category.values());

    public record PresetForm(String name, int durationMinutes, String colorHex, Category category) {}

    private Consumer<PresetForm> onCreate;

    public PresetBlockCreateDialog(Window owner) {
        super(owner, "미리블록 생성", ModalityType.APPLICATION_MODAL);
        setSize(360, 280);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 8));

        for (var color : Constants.BLOCK_COLOR_PALETTE) {
            colorCombo.addItem(String.format("#%06X", color.getRGB() & 0xFFFFFF));
        }
        categoryCombo.setRenderer((list, value, idx, sel, foc) -> {
            JLabel l = new JLabel(value == null ? "" : value.getDisplayName());
            l.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            return l;
        });

        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
    }

    public void setOnCreate(Consumer<PresetForm> cb) { this.onCreate = cb; }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(p, c, row++, "이름",      nameField);
        addRow(p, c, row++, "소요(분)",  durationSpinner);
        addRow(p, c, row++, "색상",      colorCombo);
        addRow(p, c, row++, "카테고리",  categoryCombo);
        return p;
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, java.awt.Component field) {
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(64, 24));
        p.add(l, c);
        c.gridx = 1; c.weightx = 1;
        p.add(field, c);
    }

    private JPanel buildButtons() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        JButton create = new JButton("생성");
        JButton cancel = new JButton("취소");
        create.addActionListener(e -> handleCreate());
        cancel.addActionListener(e -> dispose());
        p.add(create);
        p.add(cancel);
        return p;
    }

    private void handleCreate() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "이름을 입력해주세요.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }
        PresetForm form = new PresetForm(
                name,
                (Integer) durationSpinner.getValue(),
                (String) colorCombo.getSelectedItem(),
                (Category) categoryCombo.getSelectedItem());
        if (onCreate != null) onCreate.accept(form);
        dispose();
    }
}
