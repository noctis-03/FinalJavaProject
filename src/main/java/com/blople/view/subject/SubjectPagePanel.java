package com.blople.view.subject;

import com.blople.model.block.AssignmentBlock;
import com.blople.model.block.FileBlock;
import com.blople.model.enums.AssignmentStatus;
import com.blople.model.subject.Subject;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 과목 페이지 전체 레이아웃 (FR-03).
 * 좌측 {@link SubjectListPanel} + 우측 {@link SubjectDetailPanel}(과제/파일 탭).
 */
public class SubjectPagePanel extends JPanel {

    private final SubjectListPanel   listPanel   = new SubjectListPanel();
    private final SubjectDetailPanel detailPanel = new SubjectDetailPanel();

    public SubjectPagePanel() {
        setLayout(new BorderLayout());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, detailPanel);
        split.setDividerLocation(240);
        split.setDividerSize(1);
        add(split, BorderLayout.CENTER);
    }

    public void setSubjects(List<Subject> subjects) { listPanel.setSubjects(subjects); }

    public void showSubjectDetail(Subject subject,
                                  List<AssignmentBlock> assignments,
                                  List<FileBlock> files) {
        detailPanel.showSubject(subject, assignments, files);
    }

    public void refreshAssignments(List<AssignmentBlock> assignments) {
        detailPanel.refreshAssignments(assignments);
    }

    public void refreshFiles(List<FileBlock> files) {
        detailPanel.refreshFiles(files);
    }

    public void showEmptyDetail() { detailPanel.showEmpty(); }

    public Subject getCurrentSubject() { return detailPanel.getCurrentSubject(); }

    // 콜백 위임
    public void setOnSubjectSelect(Consumer<Subject> cb)                  { listPanel.setOnSubjectSelect(cb); }
    public void setOnTimetableClick(Runnable cb)                          { listPanel.setOnTimetableClick(cb); }
    public void setOnAddAssignment(Runnable cb)                           { detailPanel.setOnAddAssignment(cb); }
    public void setOnStatusChange(BiConsumer<String, AssignmentStatus> cb){ detailPanel.setOnStatusChange(cb); }
    public void setOnDeleteAssignment(Consumer<String> cb)                { detailPanel.setOnDeleteAssignment(cb); }
    public void setOnAddFile(Runnable cb)                                 { detailPanel.setOnAddFile(cb); }
    public void setOnOpenFile(Consumer<String> cb)                        { detailPanel.setOnOpenFile(cb); }
    public void setOnDeleteFile(Consumer<String> cb)                      { detailPanel.setOnDeleteFile(cb); }

    public SubjectListPanel   getListPanel()   { return listPanel; }
    public SubjectDetailPanel getDetailPanel() { return detailPanel; }
}
