package com.blople.view.subject;

import com.blople.model.block.ScheduleBlock;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
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
import java.util.Calendar;
import java.util.Date;
import java.util.function.Consumer;

/**
 * 과제 입력 다이얼로그 (FR-10).
 *
 * <p>과제명(필수) + 마감일시(필수) + 설명/배점/제출방식(선택)을 입력받아
 * {@link #onSave} 콜백으로 넘긴다.
 */
public class AssignmentInputDialog extends JDialog {

    public static final java.time.format.DateTimeFormatter TS_FMT = ScheduleBlock.TS_FORMATTER;

    private final JTextField nameField    = new JTextField(16);
    private final JSpinner deadlineSpinner;
    private final JTextArea descArea      = new JTextArea(4, 16);
    private final JTextField scoreField   = new JTextField(8);
    private final JComboBox<String> submitCombo =
            new JComboBox<>(new String[]{"온라인", "오프라인", "기타"});

    /** 입력값 묶음. */
    public record AssignmentForm(String name, String deadline, String description,
                                 String scoreWeight, String submitMethod) {}

    private Consumer<AssignmentForm> onSave;

    public AssignmentInputDialog(Window owner) {
        super(owner, "과제 추가", ModalityType.APPLICATION_MODAL);

        // 기본 마감일 = 오늘 23:59
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 0);
        deadlineSpinner = new JSpinner(new SpinnerDateModel(cal.getTime(), null, null, Calendar.MINUTE));
        deadlineSpinner.setEditor(new JSpinner.DateEditor(deadlineSpinner, "yyyy-MM-dd HH:mm"));

        setSize(420, 420);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 8));

        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
    }

    public void setOnSave(Consumer<AssignmentForm> cb) { this.onSave = cb; }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(p, c, row++, "과제명",   nameField);
        addRow(p, c, row++, "마감일시", deadlineSpinner);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        addRow(p, c, row++, "설명",     new JScrollPane(descArea));
        addRow(p, c, row++, "배점/비중", scoreField);
        addRow(p, c, row++, "제출방식", submitCombo);
        return p;
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, java.awt.Component field) {
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(70, 24));
        p.add(l, c);
        c.gridx = 1; c.weightx = 1;
        p.add(field, c);
    }

    private JPanel buildButtons() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        JButton save   = new JButton("저장");
        JButton cancel = new JButton("취소");
        save.addActionListener(e -> handleSave());
        cancel.addActionListener(e -> dispose());
        p.add(save);
        p.add(cancel);
        return p;
    }

    private void handleSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "과제명을 입력해주세요.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Date date = (Date) deadlineSpinner.getValue();
        LocalDateTime dl = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        String deadline = dl.withSecond(0).format(TS_FMT);

        // 마감일이 과거여도 생성은 허용하되 경고
        if (dl.isBefore(LocalDateTime.now())) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "마감일이 이미 지났습니다. 그래도 생성할까요?",
                    "마감일 경고", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
        }

        AssignmentForm form = new AssignmentForm(
                name, deadline, descArea.getText().trim(),
                scoreField.getText().trim(), (String) submitCombo.getSelectedItem());

        if (onSave != null) onSave.accept(form);
        dispose();
    }
}
