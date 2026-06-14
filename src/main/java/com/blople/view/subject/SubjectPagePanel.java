package com.blople.view.subject;

import com.blople.model.block.AssignmentBlock;
import com.blople.model.block.FileBlock;
import com.blople.model.enums.AssignmentStatus;
import com.blople.model.subject.Subject;
import com.blople.view.canvas.InfiniteCanvasPanel;
import com.blople.view.canvas.SubjectCanvasHeaderPanel;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 과목 페이지 전체 레이아웃 (FR-03, FR-07, FR-08).
 *
 * <p>좌측 {@link SubjectTreePanel}(과목 카드 트리)
 * + 우측 [헤더(과목 표시 + 팔레트 + 캔버스 액션) + {@link InfiniteCanvasPanel}].
 */
public class SubjectPagePanel extends JPanel {

    private final SubjectTreePanel         treePanel   = new SubjectTreePanel();
    private final SubjectCanvasHeaderPanel headerPanel = new SubjectCanvasHeaderPanel();
    private final InfiniteCanvasPanel      canvasPanel = new InfiniteCanvasPanel();

    public SubjectPagePanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel rightArea = new JPanel(new BorderLayout());
        rightArea.setBackground(Color.WHITE);
        rightArea.add(headerPanel, BorderLayout.NORTH);
        rightArea.add(canvasPanel, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, rightArea);
        split.setDividerLocation(320);
        split.setDividerSize(1);
        split.setBorder(null);
        split.setContinuousLayout(true);
        split.setBackground(new Color(0xD8E0EB));

        add(split, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    }

    public void setSubjects(List<Subject> subjects) {
        treePanel.setSubjects(subjects);
    }

    public void setSuppliers(Function<String, List<AssignmentBlock>> assignmentSupplier,
                             Function<String, List<FileBlock>> fileSupplier) {
        treePanel.setSuppliers(assignmentSupplier, fileSupplier);
    }

    public void reloadExpandedSubject(Subject subject) {
        treePanel.reloadExpandedSubject(subject);
    }

    public InfiniteCanvasPanel      getCanvasPanel() { return canvasPanel; }
    public SubjectCanvasHeaderPanel getHeaderPanel() { return headerPanel; }

    public void showSubjectInHeader(Subject subject) {
        headerPanel.showSubject(subject);
    }

    // ===== 좌측 트리 콜백 위임 =====
    public void setOnTimetableClick(Runnable cb)                          { treePanel.setOnTimetableClick(cb); }
    public void setOnSubjectSelect(Consumer<Subject> cb)                  { treePanel.setOnSubjectSelect(cb); }
    public void setOnAddAssignment(Consumer<Subject> cb)                  { treePanel.setOnAddAssignment(cb); }
    public void setOnStatusChange(BiConsumer<String, AssignmentStatus> cb){ treePanel.setOnStatusChange(cb); }
    public void setOnDeleteAssignment(Consumer<String> cb)                { treePanel.setOnDeleteAssignment(cb); }
    public void setOnAddFile(Consumer<Subject> cb)                        { treePanel.setOnAddFile(cb); }
    public void setOnOpenFile(Consumer<String> cb)                        { treePanel.setOnOpenFile(cb); }
    public void setOnDeleteFile(Consumer<String> cb)                      { treePanel.setOnDeleteFile(cb); }

    // ===== 우측 캔버스 헤더 콜백 위임 =====
    /** 캔버스 "중심으로" 버튼 클릭 콜백. */
    public void setOnCanvasRecenter(Runnable cb) {
        headerPanel.setOnRecenterClick(cb);
    }

    /** 캔버스 "비우기" 버튼 클릭 콜백 (해당 과목 캔버스 전체 삭제). */
    public void setOnCanvasClearAll(Runnable cb) {
        headerPanel.setOnClearCanvasClick(cb);
    }
}
