package com.blople.view.calendar;

import com.blople.common.Constants;
import com.blople.model.enums.Category;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 미리블록 생성 다이얼로그 (FR-02).
 *
 * 수정 사항:
 * - 색상 선택을 콤보박스 대신 클릭형 팔레트로 변경
 * - 색상 잘림 현상 해결
 * - 다이얼로그 폭/행 높이 조정
 */
public class PresetBlockCreateDialog extends JDialog {

    private final JTextField nameField = new JTextField(16);
    private final JSpinner durationSpinner =
            new JSpinner(new SpinnerNumberModel(60, 10, 600, 10));
    private final CategoryComboBox categoryCombo = new CategoryComboBox();
    private final ColorPalettePanel colorPalette = new ColorPalettePanel();

    public record PresetForm(String name, int durationMinutes, String colorHex, Category category) {}

    private Consumer<PresetForm> onCreate;

    public PresetBlockCreateDialog(Window owner) {
        super(owner, "미리블록 생성", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(0, 10));
        setSize(430, 300);
        setMinimumSize(new Dimension(430, 300));
        setResizable(false);
        setLocationRelativeTo(owner);

        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
    }

    public void setOnCreate(Consumer<PresetForm> cb) {
        this.onCreate = cb;
    }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 2, 14));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 6, 7, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(p, c, row++, "이름", nameField);
        addRow(p, c, row++, "소요(분)", durationSpinner);
        addRow(p, c, row++, "색상", colorPalette);
        addRow(p, c, row++, "카테고리", categoryCombo);

        return p;
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, Component field) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        c.gridwidth = 1;

        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(64, 28));
        p.add(l, c);

        c.gridx = 1;
        c.weightx = 1;
        p.add(field, c);
    }

    private JPanel buildButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        p.setBorder(BorderFactory.createEmptyBorder(4, 12, 14, 12));

        JButton create = new JButton("생성");
        JButton cancel = new JButton("취소");

        create.setPreferredSize(new Dimension(74, 30));
        cancel.setPreferredSize(new Dimension(74, 30));

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
                colorPalette.getSelectedHex(),
                (Category) categoryCombo.getSelectedItem()
        );

        if (onCreate != null) onCreate.accept(form);
        dispose();
    }

    /**
     * 카테고리 표시명 렌더링용 콤보박스.
     */
    private static class CategoryComboBox extends javax.swing.JComboBox<Category> {
        CategoryComboBox() {
            super(Category.values());
            setRenderer((list, value, idx, sel, foc) -> {
                JLabel l = new JLabel(value == null ? "" : value.getDisplayName());
                l.setOpaque(true);
                l.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                if (sel) {
                    l.setBackground(list.getSelectionBackground());
                    l.setForeground(list.getSelectionForeground());
                } else {
                    l.setBackground(Color.WHITE);
                    l.setForeground(new Color(0x222222));
                }
                return l;
            });
        }
    }

    /**
     * 색상 잘림 없이 항상 한 줄로 보이게 하는 클릭형 팔레트 패널.
     */
    private static class ColorPalettePanel extends JPanel {
        private final List<ColorSwatchButton> swatches = new ArrayList<>();
        private final ButtonGroup group = new ButtonGroup();

        ColorPalettePanel() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            setPreferredSize(new Dimension(300, 30));
            setMinimumSize(new Dimension(300, 30));

            for (int i = 0; i < Constants.BLOCK_COLOR_PALETTE.length; i++) {
                Color color = Constants.BLOCK_COLOR_PALETTE[i];
                ColorSwatchButton btn = new ColorSwatchButton(color);
                if (i == 0) btn.setSelected(true);

                swatches.add(btn);
                group.add(btn);
                add(btn);

                if (i < Constants.BLOCK_COLOR_PALETTE.length - 1) {
                    add(Box.createHorizontalStrut(8));
                }
            }
        }

        String getSelectedHex() {
            for (ColorSwatchButton b : swatches) {
                if (b.isSelected()) {
                    Color c = b.getSwatchColor();
                    return String.format("#%06X", c.getRGB() & 0xFFFFFF);
                }
            }
            Color fallback = Constants.BLOCK_COLOR_PALETTE[0];
            return String.format("#%06X", fallback.getRGB() & 0xFFFFFF);
        }
    }

    /**
     * 사각형 색상 버튼.
     */
    private static class ColorSwatchButton extends JRadioButton {
        private final Color swatchColor;

        ColorSwatchButton(Color swatchColor) {
            this.swatchColor = swatchColor;
            setOpaque(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(24, 24));
            setMinimumSize(new Dimension(24, 24));
            setMaximumSize(new Dimension(24, 24));
            setToolTipText(String.format("#%06X", swatchColor.getRGB() & 0xFFFFFF));
        }

        Color getSwatchColor() {
            return swatchColor;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(swatchColor);
            g2.fillRoundRect(4, 4, w - 8, h - 8, 6, 6);

            g2.setColor(new Color(0xB8C2D1));
            g2.drawRoundRect(4, 4, w - 8, h - 8, 6, 6);

            if (isSelected()) {
                g2.setStroke(new BasicStroke(2.2f));
                g2.setColor(new Color(0x1F3B64));
                g2.drawRoundRect(2, 2, w - 4, h - 4, 8, 8);
            }

            g2.dispose();
        }
    }
}
