package com.blople.controller;

import com.blople.common.EventBus;
import com.blople.model.block.TextBlock;
import com.blople.model.canvas.CanvasState;
import com.blople.model.enums.AssignmentStatus;
import com.blople.model.subject.Semester;
import com.blople.model.subject.Subject;
import com.blople.model.subject.Timetable;
import com.blople.service.AssignmentService;
import com.blople.service.CanvasService;
import com.blople.service.FileBlockService;
import com.blople.service.SubjectService;
import com.blople.service.TimetableService;
import com.blople.view.canvas.CanvasBlockComponent;
import com.blople.view.canvas.InfiniteCanvasPanel;
import com.blople.view.canvas.TextBlockComponent;
import com.blople.view.canvas.TextBlockEditDialog;
import com.blople.view.subject.AssignmentInputDialog;
import com.blople.view.subject.SubjectPagePanel;
import com.blople.view.subject.TimetableInputDialog;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Window;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 과목 페이지 컨트롤러 (FR-03, FR-04, FR-05, FR-06, FR-08, FR-09, FR-10).
 *
 * <p>좌측 2단 트리(과목 → 과제/파일) 조작 + 우측 무한 캔버스(텍스트 블록) 연동.
 */
public class SubjectController {

    private final SubjectService    subjectService;
    private final TimetableService  timetableService;
    private final AssignmentService assignmentService;
    private final FileBlockService  fileBlockService;
    private final CanvasService     canvasService;
    private final SubjectPagePanel  view;
    private final EventBus eventBus = EventBus.getInstance();

    private Semester activeSemester;
    private Subject  selectedSubject;

    public SubjectController(SubjectService subjectService,
                             TimetableService timetableService,
                             AssignmentService assignmentService,
                             FileBlockService fileBlockService,
                             CanvasService canvasService,
                             SubjectPagePanel view) {
        this.subjectService = subjectService;
        this.timetableService = timetableService;
        this.assignmentService = assignmentService;
        this.fileBlockService = fileBlockService;
        this.canvasService = canvasService;
        this.view = view;
    }

    public void initialize() {
        activeSemester = subjectService.ensureActiveSemester();

        // 트리가 과제/파일을 즉시 가져올 수 있도록 공급자 주입
        view.setSuppliers(
                subjectId -> assignmentService.getBySubject(subjectId),
                subjectId -> fileBlockService.getBySubject(subjectId));

        view.setOnTimetableClick(this::openTimetableDialog);
        view.setOnSubjectSelect(this::onSubjectSelect);
        view.setOnAddAssignment(this::onAddAssignment);
        view.setOnStatusChange(this::onStatusChange);
        view.setOnDeleteAssignment(this::onDeleteAssignment);
        view.setOnAddFile(this::onAddFile);
        view.setOnOpenFile(this::onOpenFile);
        view.setOnDeleteFile(this::onDeleteFile);

        // 캔버스 콜백
        InfiniteCanvasPanel canvas = view.getCanvasPanel();
        canvas.setOnCreateRequest(this::onCanvasCreateText);
        canvas.setOnEditRequest(this::onCanvasEditBlock);
        canvas.setOnMovedOrResized(this::onCanvasBlockMoved);

        eventBus.subscribe(EventBus.SUBJECT_CHANGED, p -> SwingUtilities.invokeLater(this::reloadSubjects));

        reloadSubjects();
    }

    private void reloadSubjects() {
        List<Subject> subjects = subjectService.getSubjectsBySemester(activeSemester.getId());
        view.setSubjects(subjects);
    }

    // ===== 과목 선택 → 우측 캔버스 로드 =====
    private void onSubjectSelect(Subject subject) {
        this.selectedSubject = subject;
        loadCanvas(subject);
    }

    private void loadCanvas(Subject subject) {
        InfiniteCanvasPanel canvas = view.getCanvasPanel();
        List<CanvasBlockComponent> comps = new ArrayList<>();
        for (CanvasService.Placed<TextBlock> p : canvasService.loadTextBlocks(subject.getId())) {
            CanvasState st = p.state();
            comps.add(new TextBlockComponent(p.block(),
                    st.getPosX(), st.getPosY(), st.getWidth(), st.getHeight()));
        }
        canvas.setBlocks(comps);
    }

    private void refreshExpanded() {
        if (selectedSubject != null) view.reloadExpandedSubject(selectedSubject);
    }

    // ===== 시간표 =====
    private void openTimetableDialog() {
        Window owner = SwingUtilities.getWindowAncestor(view);
        TimetableInputDialog dialog = new TimetableInputDialog(owner);
        List<Timetable> existing = timetableService.getBySemester(activeSemester.getId());
        dialog.loadExisting(existing, id -> subjectService.getSubjectById(id).orElse(null));
        dialog.setOnAdd(form -> handleTimetableAdd(dialog, form));
        dialog.setVisible(true);
    }

    private void handleTimetableAdd(TimetableInputDialog dialog,
                                    TimetableInputDialog.TimetableForm form) {
        try {
            Subject subject = subjectService.createSubject(
                    activeSemester.getId(), form.subjectName(),
                    form.professor(), form.classroom(), form.colorHex());

            Timetable candidate = new Timetable(
                    subject.getId(), form.dayOfWeek(), form.startTime(), form.endTime());
            List<Timetable> conflicts =
                    timetableService.detectConflicts(candidate, activeSemester.getId());
            if (!conflicts.isEmpty()) {
                int ok = JOptionPane.showConfirmDialog(dialog,
                        "같은 시간대에 이미 일정이 있습니다. 그래도 추가할까요?",
                        "시간표 충돌", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ok != JOptionPane.YES_OPTION) return;
            }

            timetableService.addEntry(subject.getId(),
                    form.dayOfWeek(), form.startTime(), form.endTime());
            dialog.appendEntryLine(TimetableInputDialog.formatLine(
                    subject.getName(), form.dayOfWeek(), form.startTime(), form.endTime()));
            reloadSubjects();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dialog, "추가 실패: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== 과제 =====
    private void onAddAssignment(Subject subject) {
        this.selectedSubject = subject;
        Window owner = SwingUtilities.getWindowAncestor(view);
        AssignmentInputDialog dialog = new AssignmentInputDialog(owner);
        dialog.setOnSave(form -> {
            try {
                assignmentService.createAssignment(
                        subject.getId(), form.name(), subject.getColor(),
                        form.deadline(), form.description());
                view.reloadExpandedSubject(subject);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(owner, "과제 생성 실패: " + ex.getMessage(),
                        "오류", JOptionPane.ERROR_MESSAGE);
            }
        });
        dialog.setVisible(true);
    }

    private void onStatusChange(String blockId, AssignmentStatus status) {
        assignmentService.updateStatus(blockId, status);
        refreshExpanded();
    }

    private void onDeleteAssignment(String blockId) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        int ok = JOptionPane.showConfirmDialog(owner,
                "이 과제를 삭제할까요?\n(캘린더의 마감 표시도 함께 삭제됩니다)",
                "과제 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        assignmentService.deleteAssignment(blockId);
        refreshExpanded();
    }

    // ===== 파일 =====
    private void onAddFile(Subject subject) {
        this.selectedSubject = subject;
        Window owner = SwingUtilities.getWindowAncestor(view);

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
                "지원 파일 (" + String.join(", ", fileBlockService.getSupportedExtensions()) + ")",
                fileBlockService.getSupportedExtensions().toArray(new String[0])));

        if (chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        try {
            fileBlockService.createFromFile(subject.getId(), subject.getColor(), file);
            view.reloadExpandedSubject(subject);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, ex.getMessage(),
                    "파일 등록 실패", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onOpenFile(String blockId) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        try {
            fileBlockService.openFile(blockId);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(owner, ex.getMessage(),
                    "파일 열기 실패", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onDeleteFile(String blockId) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        int ok = JOptionPane.showConfirmDialog(owner,
                "이 파일 블록을 삭제할까요?\n(원본 파일은 삭제되지 않습니다)",
                "파일 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        fileBlockService.delete(blockId);
        refreshExpanded();
    }

    // ===== 캔버스 (FR-06, FR-08) =====
    private void onCanvasCreateText(Point2D world) {
        if (selectedSubject == null) return;
        CanvasService.Placed<TextBlock> placed =
                canvasService.createTextBlock(selectedSubject.getId(), world.getX(), world.getY());
        CanvasState st = placed.state();
        view.getCanvasPanel().addBlock(new TextBlockComponent(
                placed.block(), st.getPosX(), st.getPosY(), st.getWidth(), st.getHeight()));
    }

    private void onCanvasEditBlock(CanvasBlockComponent comp) {
        if (!(comp instanceof TextBlockComponent tc)) return;
        TextBlock model = tc.getModel();
        Window owner = SwingUtilities.getWindowAncestor(view);
        TextBlockEditDialog dialog =
                new TextBlockEditDialog(owner, model.getTitle(), model.getContent());
        dialog.setOnSave((title, content) -> {
            canvasService.updateTextContent(model.getId(), title, content);
            model.setTitle(title);
            model.setContent(content);
            view.getCanvasPanel().repaint();
        });
        dialog.setVisible(true);
    }

    private void onCanvasBlockMoved(CanvasBlockComponent comp) {
        if (selectedSubject == null) return;
        canvasService.findStateByBlock(comp.getBlockId()).ifPresentOrElse(
                existing -> {
                    existing.setPosX(comp.getWorldX());
                    existing.setPosY(comp.getWorldY());
                    existing.setWidth(comp.getWorldW());
                    existing.setHeight(comp.getWorldH());
                    canvasService.saveState(existing);
                },
                () -> {
                    CanvasState fresh = new CanvasState(
                            selectedSubject.getId(), comp.getBlockId(),
                            comp.getWorldX(), comp.getWorldY(),
                            comp.getWorldW(), comp.getWorldH());
                    canvasService.saveState(fresh);
                });
    }
}
