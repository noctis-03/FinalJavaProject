package com.blople.view.subject;

import com.blople.model.block.AssignmentBlock;
import com.blople.model.block.FileBlock;
import com.blople.model.enums.AssignmentStatus;
import com.blople.model.subject.Subject;
import com.blople.view.canvas.InfiniteCanvasPanel;
import com.blople.view.canvas.SubjectCanvasHeaderPanel;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 과목 페이지 전체 레이아웃 (FR-03, FR-07, FR-08).
 *
 * <p>좌측 {@link SubjectTreePanel}(2단 아코디언)
 * + 우측 [헤더(과목 표시 + 팔레트) + {@link InfiniteCanvasPanel}].
 */
public class SubjectPagePanel extends JPanel {

    private final SubjectTreePanel         treePanel   = new SubjectTreePanel();
    private final SubjectCanvasHeaderPanel headerPanel = new SubjectCanvasHeaderPanel();
    private final InfiniteCanvasPanel      canvasPanel = new InfiniteCanvasPanel();

    public SubjectPagePanel() {
        setLayout(new BorderLayout());

        JPanel rightArea = new JPanel(new BorderLayout());
        rightArea.add(headerPanel, BorderLayout.NORTH);
        rightArea.add(canvasPanel, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, rightArea);
        split.setDividerLocation(300);
        split.setDividerSize(1);
        add(split, BorderLayout.CENTER);
    }

    public void setSubjects(List<Subject> subjects) { treePanel.setSubjects(subjects); }

    public void setSuppliers(Function<String, List<AssignmentBlock>> assignmentSupplier,
                             Function<String, List<FileBlock>> fileSupplier) {
        treePanel.setSuppliers(assignmentSupplier, fileSupplier);
    }

    public void reloadExpandedSubject(Subject subject) {
        treePanel.reloadExpandedSubject(subject);
    }

    public InfiniteCanvasPanel      getCanvasPanel() { return canvasPanel; }
    public SubjectCanvasHeaderPanel getHeaderPanel() { return headerPanel; }

    public void showSubjectInHeader(Subject subject) { headerPanel.showSubject(subject); }

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
