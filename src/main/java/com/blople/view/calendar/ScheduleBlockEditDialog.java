package com.blople.view.calendar;

import com.blople.common.Constants;
import com.blople.model.block.ScheduleBlock;
import com.blople.view.component.ColorPalettePicker;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Consumer;

public class ScheduleBlockEditDialog extends JDialog {

    public static final DateTimeFormatter TS_FMT = ScheduleBlock.TS_FORMATTER;

    public record Form(String name, String colorHex, String startDatetime, String endDatetime) {}

    private final JTextField nameField = new JTextField(14);
    private final ColorPalettePicker colorPicker;
    private final JSpinner startSpinner;
    private final JSpinner endSpinner;

    private Consumer<Form> onSave;
    private Runnable onDelete;

    public ScheduleBlockEditDialog(Window owner, ScheduleBlock block) {
        super(owner, "일정 편집", ModalityType.APPLICATION_MODAL);
        setSize(420, 340);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 8));

        nameField.setText(block.getName() == null ? "" : block.getName());
        colorPicker = new ColorPalettePicker(
                block.getColor() == null ? Constants.DEFAULT_BLOCK_COLOR_HEX : block.getColor());

        LocalDateTime s = LocalDateTime.parse(block.getStartDatetime(), TS_FMT);
        LocalDateTime e = LocalDateTime.parse(block.getEndDatetime(), TS_FMT);
        startSpinner = makeDateSpinner(s);
        endSpinner   = makeDateSpinner(e);

        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
    }

    public void setOnSave(Consumer<Form> cb) { this.onSave = cb; }
    public void setOnDelete(Runnable cb)     { this.onDelete = cb; }

    private JSpinner makeDateSpinner(LocalDateTime dt) {
        Calendar cal = Calendar.getInstance();
        cal.set(dt.getYear(), dt.getMonthValue() - 1, dt.getDayOfMonth(),
                dt.getHour(), dt.getMinute(), 0);
        JSpinner sp = new JSpinner(new SpinnerDateModel(cal.getTime(), null, null, Calendar.MINUTE));
        sp.setEditor(new JSpinner.DateEditor(sp, "yyyy-MM-dd HH:mm"));
        return sp;
    }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(p, c, row++, "일정명", nameField);
        addRow(p, c, row++, "색상", colorPicker);
        addRow(p, c, row++, "시작", startSpinner);
        addRow(p, c, row++, "종료", endSpinner);
        return p;
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, java.awt.Component field) {
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(60, 24));
        p.add(l, c);
        c.gridx = 1; c.weightx = 1;
        p.add(field, c);
    }

    private JPanel buildButtons() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        JButton save = new JButton("저장");
        JButton del  = new JButton("삭제");
        JButton cancel = new JButton("취소");

        save.addActionListener(e -> handleSave());
        del.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    "이 일정을 삭제할까요?", "일정 삭제",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) {
                if (onDelete != null) onDelete.run();
                dispose();
            }
        });
        cancel.addActionListener(e -> dispose());

        p.add(save);
        p.add(del);
        p.add(cancel);
        return p;
    }

    private void handleSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "일정명을 입력해주세요.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Date sd = (Date) startSpinner.getValue();
        Date ed = (Date) endSpinner.getValue();
        LocalDateTime s = sd.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().withSecond(0);
        LocalDateTime e = ed.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().withSecond(0);
        if (!s.isBefore(e)) {
            JOptionPane.showMessageDialog(this, "시작이 종료보다 늦거나 같습니다.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Form form = new Form(name, colorPicker.getSelectedHex(),
                s.format(TS_FMT), e.format(TS_FMT));
        if (onSave != null) onSave.accept(form);
        dispose();
    }
}
