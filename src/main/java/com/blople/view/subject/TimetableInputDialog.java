package com.blople.view.subject;

import com.blople.common.Constants;
import com.blople.model.subject.Subject;
import com.blople.model.subject.Timetable;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import java.util.function.Consumer;

/**
 * 시간표 입력 다이얼로그 (FR-09).
 *
 * <p>과목명/교수/강의실/색상 + 요일/시작/종료 시간을 입력해 시간표 항목을 추가한다.
 * 추가 버튼을 누르면 {@link #onAdd} 콜백으로 입력값을 넘긴다.
 * 등록된 항목 목록은 하단 리스트에 누적 표시된다.
 *
 * <p>요일 콤보 인덱스 = 프로젝트 요일 규약(0=일 … 6=토)과 동일하게 맞춘다.
 */
public class TimetableInputDialog extends JDialog {

    private static final String[] DAYS = {"일", "월", "화", "수", "목", "금", "토"};

    private final JTextField subjectField   = new JTextField(12);
    private final JTextField professorField  = new JTextField(10);
    private final JTextField classroomField  = new JTextField(10);
    private final JComboBox<String> colorCombo = new JComboBox<>();
    private final JComboBox<String> dayCombo   = new JComboBox<>(DAYS);
    private final JComboBox<String> startCombo = new JComboBox<>(buildTimeOptions());
    private final JComboBox<String> endCombo   = new JComboBox<>(buildTimeOptions());

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> entryList = new JList<>(listModel);

    /** 입력값 묶음. */
    public record TimetableForm(String subjectName, String professor, String classroom,
                                String colorHex, int dayOfWeek,
                                String startTime, String endTime) {}

    private Consumer<TimetableForm> onAdd;

    public TimetableInputDialog(Window owner) {
        super(owner, "시간표 입력", ModalityType.APPLICATION_MODAL);
        setSize(560, 520);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 8));

        add(buildForm(), BorderLayout.NORTH);
        add(buildList(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
    }

    public void setOnAdd(Consumer<TimetableForm> cb) { this.onAdd = cb; }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        // 색상 콤보 채우기 (팔레트 hex)
        for (var color : Constants.BLOCK_COLOR_PALETTE) {
            colorCombo.addItem(String.format("#%06X", color.getRGB() & 0xFFFFFF));
        }

        int row = 0;
        addRow(p, c, row++, "과목명",  subjectField);
        addRow(p, c, row++, "교수명",  professorField);
        addRow(p, c, row++, "강의실",  classroomField);
        addRow(p, c, row++, "색상",    colorCombo);
        addRow(p, c, row++, "요일",    dayCombo);
        addRow(p, c, row++, "시작",    startCombo);
        addRow(p, c, row++, "종료",    endCombo);

        endCombo.setSelectedIndex(Math.min(2, endCombo.getItemCount() - 1)); // 기본 1시간 뒤쯤
        return p;
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, java.awt.Component field) {
        c.gridx = 0; c.gridy = row;
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(60, 24));
        p.add(l, c);
        c.gridx = 1;
        p.add(field, c);
    }

    private JScrollPane buildList() {
        entryList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JScrollPane sp = new JScrollPane(entryList);
        sp.setBorder(BorderFactory.createTitledBorder("등록된 시간표"));
        return sp;
    }

    private JPanel buildButtons() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        JButton addBtn   = new JButton("추가");
        JButton closeBtn = new JButton("닫기");

        addBtn.addActionListener(e -> handleAdd());
        closeBtn.addActionListener(e -> dispose());

        p.add(addBtn);
        p.add(closeBtn);
        return p;
    }

    private void handleAdd() {
        String name = subjectField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "과목명을 입력해주세요.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String start = (String) startCombo.getSelectedItem();
        String end   = (String) endCombo.getSelectedItem();
        if (start.compareTo(end) >= 0) {
            JOptionPane.showMessageDialog(this, "시작 시간이 종료 시간보다 늦거나 같습니다.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        TimetableForm form = new TimetableForm(
                name,
                professorField.getText().trim(),
                classroomField.getText().trim(),
                (String) colorCombo.getSelectedItem(),
                dayCombo.getSelectedIndex(),
                start, end);

        if (onAdd != null) onAdd.accept(form);
    }

    /** 컨트롤러가 추가 성공 후 호출 — 리스트에 한 줄 누적. */
    public void appendEntryLine(String text) {
        listModel.addElement(text);
        subjectField.setText("");
        professorField.setText("");
        classroomField.setText("");
    }

    /** 다이얼로그 열 때 기존 시간표를 보여주기 위한 초기화. */
    public void loadExisting(List<Timetable> entries, java.util.function.Function<String, Subject> subjectResolver) {
        listModel.clear();
        for (Timetable t : entries) {
            Subject s = subjectResolver.apply(t.getSubjectId());
            String subjName = (s != null) ? s.getName() : t.getSubjectId();
            listModel.addElement(formatLine(subjName, t.getDayOfWeek(), t.getStartTime(), t.getEndTime()));
        }
    }

    public static String formatLine(String subjectName, int dayOfWeek, String start, String end) {
        return "%s요일 %s~%s  %s".formatted(DAYS[dayOfWeek], start, end, subjectName);
    }

    /** "08:00" ~ "22:30" 30분 간격 옵션. */
    private static String[] buildTimeOptions() {
        java.util.List<String> list = new java.util.ArrayList<>();
        for (int h = Constants.CALENDAR_START_HOUR; h <= Constants.CALENDAR_END_HOUR; h++) {
            list.add(String.format("%02d:00", h));
            if (h < Constants.CALENDAR_END_HOUR) list.add(String.format("%02d:30", h));
        }
        return list.toArray(new String[0]);
    }
}
