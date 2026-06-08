package com.blople.view.subject;

import com.blople.model.block.AssignmentBlock;
import com.blople.model.block.FileBlock;
import com.blople.model.enums.AssignmentStatus;
import com.blople.model.subject.Subject;
import com.blople.view.canvas.InfiniteCanvasPanel;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 과목 페이지 전체 레이아웃 (FR-03, FR-08).
 *
 * <p>좌측 {@link SubjectTreePanel}(2단 아코디언: 과목 → 과제/파일)
 * + 우측 {@link InfiniteCanvasPanel}(항상 표시되는 무한 캔버스).
 */
public class SubjectPagePanel extends JPanel {

    private final SubjectTreePanel    treePanel   = new SubjectTreePanel();
    private final InfiniteCanvasPanel canvasPanel = new InfiniteCanvasPanel();

    public SubjectPagePanel() {
        setLayout(new BorderLayout());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, canvasPanel);
        split.setDividerLocation(300);
        split.setDividerSize(1);
        add(split, BorderLayout.CENTER);
    }

    public void setSubjects(List<Subject> subjects) { treePanel.setSubjects(subjects); }

    public void setSuppliers(Function<String, List<AssignmentBlock>> assignmentSupplier,
                             Function<String, List<FileBlock>> fileSupplier) {
        treePanel.setSuppliers(assignmentSupplier, fileSupplier);
    }

    /** 현재 펼쳐진 과목의 과제/파일 섹션 갱신. */
    public void reloadExpandedSubject(Subject subject) {
        treePanel.reloadExpandedSubject(subject);
    }

    public InfiniteCanvasPanel getCanvasPanel() { return canvasPanel; }

    // 콜백 위임
    public void setOnTimetableClick(Runnable cb)                          { treePanel.setOnTimetableClick(cb); }
    public void setOnSubjectSelect(Consumer<Subject> cb)                  { treePanel.setOnSubjectSelect(cb); }
    public void setOnAddAssignment(Consumer<Subject> cb)                  { treePanel.setOnAddAssignment(cb); }
    public void setOnStatusChange(BiConsumer<String, AssignmentStatus> cb){ treePanel.setOnStatusChange(cb); }
    public void setOnDeleteAssignment(Consumer<String> cb)                { treePanel.setOnDeleteAssignment(cb); }
    public void setOnAddFile(Consumer<Subject> cb)                        { treePanel.setOnAddFile(cb); }
    public void setOnOpenFile(Consumer<String> cb)                        { treePanel.setOnOpenFile(cb); }
    public void setOnDeleteFile(Consumer<String> cb)                      { treePanel.setOnDeleteFile(cb); }
}
