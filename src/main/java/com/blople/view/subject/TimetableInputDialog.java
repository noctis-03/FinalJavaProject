package com.blople.view.subject;

import com.blople.common.Constants;
import com.blople.model.subject.Semester;
import com.blople.model.subject.Subject;
import com.blople.model.subject.Timetable;
import com.blople.view.component.ColorPalettePicker;

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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class TimetableInputDialog extends JDialog {

    private static final String[] DAYS = {"일", "월", "화", "수", "목", "금", "토"};

    private final JComboBox<Integer> yearCombo = new JComboBox<>();
    private final JComboBox<Semester.Term> termCombo =
            new JComboBox<>(new Semester.Term[]{Semester.Term.FIRST, Semester.Term.SECOND});

    private final JTextField subjectField   = new JTextField(12);
    private final JTextField professorField = new JTextField(10);
    private final JTextField classroomField = new JTextField(10);
    private final ColorPalettePicker colorPicker = new ColorPalettePicker(Constants.DEFAULT_BLOCK_COLOR_HEX);
    private final JComboBox<String> dayCombo   = new JComboBox<>(DAYS);
    private final JComboBox<String> startCombo = new JComboBox<>(buildTimeOptions());
    private final JComboBox<String> endCombo   = new JComboBox<>(buildTimeOptions());

    private final DefaultListModel<EntryRow> listModel = new DefaultListModel<>();
    private final JList<EntryRow> entryList = new JList<>(listModel);

    public record TimetableForm(String subjectName, String professor, String classroom,
                                String colorHex, int dayOfWeek,
                                String startTime, String endTime) {}

    public interface SemesterChangeListener {
        void onSemesterChange(int year, Semester.Term term);
    }

    public interface EntryEditListener {
        void onEdit(String timetableEntryId, String subjectId,
                    String subjectName, String professor, String classroom, String colorHex,
                    int dayOfWeek, String startTime, String endTime);
    }

    public interface EntryDeleteListener {
        boolean onDelete(String timetableEntryId, String subjectId, boolean alsoDeleteSubject);
    }

    private Consumer<TimetableForm> onAdd;
    private SemesterChangeListener onSemesterChange;
    private EntryEditListener onEntryEdit;
    private EntryDeleteListener onEntryDelete;

    public static class EntryRow {
        public final String entryId;
        public final String subjectId;
        public final String subjectName;
        public final String professor;
        public final String classroom;
        public final String colorHex;
        public final int dayOfWeek;
        public final String startTime;
        public final String endTime;

        public EntryRow(String entryId, String subjectId, String subjectName,
                        String professor, String classroom, String colorHex,
                        int dayOfWeek, String startTime, String endTime) {
            this.entryId = entryId;
            this.subjectId = subjectId;
            this.subjectName = subjectName;
            this.professor = professor;
            this.classroom = classroom;
            this.colorHex = colorHex;
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public String toString() {
            return "%s요일 %s~%s  %s%s".formatted(
                    DAYS[dayOfWeek], startTime, endTime, subjectName,
                    (professor == null || professor.isBlank()) ? "" : "  (" + professor + ")");
        }
    }

    public TimetableInputDialog(Window owner) {
        super(owner, "시간표 입력", ModalityType.APPLICATION_MODAL);
        setSize(620, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 8));

        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear - 2; y <= currentYear + 2; y++) yearCombo.addItem(y);
        yearCombo.setSelectedItem(currentYear);
        termCombo.setSelectedItem(
                LocalDate.now().getMonthValue() <= 7 ? Semester.Term.FIRST : Semester.Term.SECOND);
        termCombo.setRenderer((list, value, idx, sel, foc) -> {
            JLabel l = new JLabel(value == null ? "" : value.label);
            l.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            return l;
        });

        yearCombo.addActionListener(e -> fireSemesterChange());
        termCombo.addActionListener(e -> fireSemesterChange());

        add(buildSemesterBar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
    }

    public void setOnAdd(Consumer<TimetableForm> cb)                 { this.onAdd = cb; }
    public void setOnSemesterChange(SemesterChangeListener cb)       { this.onSemesterChange = cb; }
    public void setOnEntryEdit(EntryEditListener cb)                 { this.onEntryEdit = cb; }
    public void setOnEntryDelete(EntryDeleteListener cb)             { this.onEntryDelete = cb; }

    public int getSelectedYear()           { return (Integer) yearCombo.getSelectedItem(); }
    public Semester.Term getSelectedTerm() { return (Semester.Term) termCombo.getSelectedItem(); }

    public void setSemesterSelection(int year, Semester.Term term) {
        if (yearCombo.getActionListeners().length > 0) {
            yearCombo.removeActionListener(yearCombo.getActionListeners()[0]);
        }
        if (termCombo.getActionListeners().length > 0) {
            termCombo.removeActionListener(termCombo.getActionListeners()[0]);
        }
        yearCombo.setSelectedItem(year);
        termCombo.setSelectedItem(term);
        yearCombo.addActionListener(e -> fireSemesterChange());
        termCombo.addActionListener(e -> fireSemesterChange());
    }

    private void fireSemesterChange() {
        if (onSemesterChange != null) {
            onSemesterChange.onSemesterChange(getSelectedYear(), getSelectedTerm());
        }
    }

    private JPanel buildSemesterBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 12));
        p.add(new JLabel("학기:"));
        p.add(yearCombo);
        p.add(new JLabel("년"));
        p.add(termCombo);
        return p;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(buildForm(), BorderLayout.NORTH);
        center.add(buildList(), BorderLayout.CENTER);
        return center;
    }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("새 시간표 추가"),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        addRow(p, c, row++, "과목명",  subjectField);
        addRow(p, c, row++, "교수명",  professorField);
        addRow(p, c, row++, "강의실",  classroomField);
        addRow(p, c, row++, "색상",    colorPicker);
        addRow(p, c, row++, "요일",    dayCombo);
        addRow(p, c, row++, "시작",    startCombo);
        addRow(p, c, row++, "종료",    endCombo);

        endCombo.setSelectedIndex(Math.min(2, endCombo.getItemCount() - 1));
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

    private JPanel buildList() {
        JPanel wrap = new JPanel(new BorderLayout(0, 4));
        wrap.setBorder(BorderFactory.createTitledBorder("등록된 시간표 (선택 후 수정/삭제)"));

        entryList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        wrap.add(new JScrollPane(entryList), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        JButton edit = new JButton("수정");
        JButton del  = new JButton("삭제");
        edit.addActionListener(e -> handleEdit());
        del.addActionListener(e -> handleDelete());
        actions.add(edit);
        actions.add(del);
        wrap.add(actions, BorderLayout.SOUTH);
        return wrap;
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
                colorPicker.getSelectedHex(),
                dayCombo.getSelectedIndex(),
                start, end);

        if (onAdd != null) onAdd.accept(form);
    }

    private void handleEdit() {
        EntryRow row = entryList.getSelectedValue();
        if (row == null) {
            JOptionPane.showMessageDialog(this, "수정할 항목을 선택하세요.",
                    "선택 필요", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        EntryEditDialog d = new EntryEditDialog(this, row);
        d.setOnSave(updated -> {
            if (onEntryEdit != null) {
                onEntryEdit.onEdit(row.entryId, row.subjectId,
                        updated.subjectName, updated.professor, updated.classroom, updated.colorHex,
                        updated.dayOfWeek, updated.startTime, updated.endTime);
            }
        });
        d.setVisible(true);
    }

    private void handleDelete() {
        EntryRow row = entryList.getSelectedValue();
        if (row == null) {
            JOptionPane.showMessageDialog(this, "삭제할 항목을 선택하세요.",
                    "선택 필요", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        long sameSubjectCount = 0;
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i).subjectId.equals(row.subjectId)) sameSubjectCount++;
        }

        boolean alsoDeleteSubject = false;
        if (sameSubjectCount == 1) {
            int ans = JOptionPane.showConfirmDialog(this,
                    "이 시간표는 '" + row.subjectName + "' 과목의 마지막 항목입니다.\n과목 자체도 삭제할까요?",
                    "과목 삭제 여부", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ans == JOptionPane.CANCEL_OPTION) return;
            alsoDeleteSubject = (ans == JOptionPane.YES_OPTION);
        } else {
            int ok = JOptionPane.showConfirmDialog(this,
                    "이 시간표 항목을 삭제할까요?",
                    "시간표 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
        }

        if (onEntryDelete != null) {
            boolean ok = onEntryDelete.onDelete(row.entryId, row.subjectId, alsoDeleteSubject);
            if (ok) {
                listModel.removeElement(row);
            }
        }
    }

    public void setEntries(List<Timetable> entries, Function<String, Subject> subjectResolver) {
        listModel.clear();
        for (Timetable t : entries) {
            Subject s = subjectResolver.apply(t.getSubjectId());
            String name = (s != null) ? s.getName() : t.getSubjectId();
            String prof = (s != null) ? s.getProfessor() : "";
            String room = (s != null) ? s.getClassroom() : "";
            String color = (s != null) ? s.getColor() : Constants.DEFAULT_BLOCK_COLOR_HEX;
            listModel.addElement(new EntryRow(
                    t.getId(), t.getSubjectId(), name, prof, room, color,
                    t.getDayOfWeek(), t.getStartTime(), t.getEndTime()));
        }
    }

    public void appendEntry(EntryRow row) {
        listModel.addElement(row);
        subjectField.setText("");
        professorField.setText("");
        classroomField.setText("");
        colorPicker.setSelectedHex(Constants.DEFAULT_BLOCK_COLOR_HEX);
    }

    public static String formatLine(String subjectName, int dayOfWeek, String start, String end) {
        return "%s요일 %s~%s  %s".formatted(DAYS[dayOfWeek], start, end, subjectName);
    }

    private static String[] buildTimeOptions() {
        java.util.List<String> list = new java.util.ArrayList<>();
        for (int h = Constants.CALENDAR_START_HOUR; h <= Constants.CALENDAR_END_HOUR; h++) {
            list.add(String.format("%02d:00", h));
            if (h < Constants.CALENDAR_END_HOUR) list.add(String.format("%02d:30", h));
        }
        return list.toArray(new String[0]);
    }

    private static class EntryEditDialog extends JDialog {
        public record UpdatedForm(String subjectName, String professor, String classroom, String colorHex,
                                  int dayOfWeek, String startTime, String endTime) {}

        private final JTextField nameF;
        private final JTextField profF;
        private final JTextField roomF;
        private final ColorPalettePicker colorPicker;
        private final JComboBox<String> dayC   = new JComboBox<>(DAYS);
        private final JComboBox<String> startC = new JComboBox<>(buildTimeOptions());
        private final JComboBox<String> endC   = new JComboBox<>(buildTimeOptions());
        private Consumer<UpdatedForm> onSave;

        EntryEditDialog(Window owner, EntryRow row) {
            super(owner, "시간표 수정", ModalityType.APPLICATION_MODAL);
            setSize(400, 360);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(0, 8));

            nameF = new JTextField(row.subjectName, 14);
            profF = new JTextField(row.professor == null ? "" : row.professor, 14);
            roomF = new JTextField(row.classroom == null ? "" : row.classroom, 14);
            colorPicker = new ColorPalettePicker(
                    row.colorHex == null ? Constants.DEFAULT_BLOCK_COLOR_HEX : row.colorHex);
            dayC.setSelectedIndex(row.dayOfWeek);
            startC.setSelectedItem(row.startTime);
            endC.setSelectedItem(row.endTime);

            JPanel p = new JPanel(new GridBagLayout());
            p.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4);
            c.anchor = GridBagConstraints.WEST;
            int r = 0;
            addRow(p, c, r++, "과목명", nameF);
            addRow(p, c, r++, "교수명", profF);
            addRow(p, c, r++, "강의실", roomF);
            addRow(p, c, r++, "색상",  colorPicker);
            addRow(p, c, r++, "요일",  dayC);
            addRow(p, c, r++, "시작",  startC);
            addRow(p, c, r++, "종료",  endC);
            add(p, BorderLayout.CENTER);

            JPanel btns = new JPanel();
            btns.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
            JButton save = new JButton("저장");
            JButton cancel = new JButton("취소");
            save.addActionListener(e -> {
                String n = nameF.getText().trim();
                if (n.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "과목명을 입력해주세요.",
                            "입력 오류", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String st = (String) startC.getSelectedItem();
                String et = (String) endC.getSelectedItem();
                if (st.compareTo(et) >= 0) {
                    JOptionPane.showMessageDialog(this, "시작이 종료보다 늦거나 같습니다.",
                            "입력 오류", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (onSave != null) {
                    onSave.accept(new UpdatedForm(
                            n,
                            profF.getText().trim(),
                            roomF.getText().trim(),
                            colorPicker.getSelectedHex(),
                            dayC.getSelectedIndex(),
                            st,
                            et
                    ));
                }
                dispose();
            });
            cancel.addActionListener(e -> dispose());
            btns.add(save);
            btns.add(cancel);
            add(btns, BorderLayout.SOUTH);
        }

        void setOnSave(Consumer<UpdatedForm> cb) { this.onSave = cb; }

        private static void addRow(JPanel p, GridBagConstraints c, int row, String label, java.awt.Component field) {
            c.gridx = 0; c.gridy = row;
            JLabel l = new JLabel(label);
            l.setPreferredSize(new Dimension(60, 24));
            p.add(l, c);
            c.gridx = 1;
            p.add(field, c);
        }
    }
}
