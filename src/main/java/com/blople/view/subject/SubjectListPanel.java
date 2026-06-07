package com.blople.view.subject;

import com.blople.model.subject.Subject;
import com.blople.view.calendar.BlockRenderer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.function.Consumer;

/**
 * 좌측 과목 목록 패널 (FR-03).
 *
 * <p>과목별 색상 점 + 이름을 리스트로 표시하고, 선택 시 콜백을 발행한다.
 * 상단에 "시간표 입력" 버튼을 둔다.
 */
public class SubjectListPanel extends JPanel {

    private final DefaultListModel<Subject> listModel = new DefaultListModel<>();
    private final JList<Subject> subjectList = new JList<>(listModel);
    private final JButton timetableButton = new JButton("＋ 시간표 입력");

    private Consumer<Subject> onSubjectSelect;
    private Runnable onTimetableClick;

    public SubjectListPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(240, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xDDDDDD)));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildList(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        p.setBackground(new Color(0xF7F7F9));
        timetableButton.setFocusPainted(false);
        timetableButton.addActionListener(e -> {
            if (onTimetableClick != null) onTimetableClick.run();
        });
        p.add(timetableButton);
        return p;
    }

    private JScrollPane buildList() {
        subjectList.setCellRenderer(new SubjectCellRenderer());
        subjectList.setFixedCellHeight(40);
        subjectList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && onSubjectSelect != null) {
                Subject s = subjectList.getSelectedValue();
                if (s != null) onSubjectSelect.accept(s);
            }
        });
        return new JScrollPane(subjectList);
    }

    public void setSubjects(List<Subject> subjects) {
        listModel.clear();
        for (Subject s : subjects) listModel.addElement(s);
    }

    public Subject getSelectedSubject() { return subjectList.getSelectedValue(); }

    public void setOnSubjectSelect(Consumer<Subject> cb) { this.onSubjectSelect = cb; }
    public void setOnTimetableClick(Runnable cb)         { this.onTimetableClick = cb; }

    /** 색상 점 + 이름을 그리는 셀 렌더러. */
    private static class SubjectCellRenderer extends JPanel implements ListCellRenderer<Subject> {
        private final JLabel dot  = new JLabel("●");
        private final JLabel name = new JLabel();

        SubjectCellRenderer() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
            name.setFont(new Font("SansSerif", Font.PLAIN, 14));
            add(dot);
            add(name);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Subject> list, Subject value,
                                                      int index, boolean selected, boolean focus) {
            dot.setForeground(BlockRenderer.parseColor(value.getColor(), new Color(0x4D96FF)));
            name.setText(value.getName());
            setBackground(selected ? new Color(0xE3EEFF) : Color.WHITE);
            return this;
        }
    }
}
