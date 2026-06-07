package com.blople.controller;

import com.blople.common.EventBus;
import com.blople.model.block.AssignmentBlock;
import com.blople.model.block.FileBlock;
import com.blople.model.enums.AssignmentStatus;
import com.blople.model.subject.Semester;
import com.blople.model.subject.Subject;
import com.blople.model.subject.Timetable;
import com.blople.service.AssignmentService;
import com.blople.service.FileBlockService;
import com.blople.service.SubjectService;
import com.blople.service.TimetableService;
import com.blople.view.subject.AssignmentInputDialog;
import com.blople.view.subject.SubjectPagePanel;
import com.blople.view.subject.TimetableInputDialog;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 과목 페이지 컨트롤러 (FR-03, FR-04, FR-05, FR-09, FR-10).
 */
public class SubjectController {

    private final SubjectService    subjectService;
    private final TimetableService  timetableService;
    private final AssignmentService assignmentService;
    private final FileBlockService  fileBlockService;
    private final SubjectPagePanel  view;
    private final EventBus eventBus = EventBus.getInstance();

    private Semester activeSemester;
    private Subject  selectedSubject;

    public SubjectController(SubjectService subjectService,
                             TimetableService timetableService,
                             AssignmentService assignmentService,
                             FileBlockService fileBlockService,
                             SubjectPagePanel view) {
        this.subjectService = subjectService;
        this.timetableService = timetableService;
        this.assignmentService = assignmentService;
        this.fileBlockService = fileBlockService;
        this.view = view;
    }

    public void initialize() {
        activeSemester = subjectService.ensureActiveSemester();

        view.setOnSubjectSelect(this::onSubjectSelect);
        view.setOnTimetableClick(this::openTimetableDialog);
        view.setOnAddAssignment(this::openAssignmentDialog);
        view.setOnStatusChange(this::onStatusChange);
        view.setOnDeleteAssignment(this::onDeleteAssignment);
        view.setOnAddFile(this::onAddFile);
        view.setOnOpenFile(this::onOpenFile);
        view.setOnDeleteFile(this::onDeleteFile);

        eventBus.subscribe(EventBus.SUBJECT_CHANGED, p -> SwingUtilities.invokeLater(this::reloadSubjects));

        reloadSubjects();
    }

    private void reloadSubjects() {
        List<Subject> subjects = subjectService.getSubjectsBySemester(activeSemester.getId());
        view.setSubjects(subjects);
    }

    // ===== 과목 선택 =====
    private void onSubjectSelect(Subject subject) {
        this.selectedSubject = subject;
        List<AssignmentBlock> assignments = assignmentService.getBySubject(subject.getId());
        List<FileBlock> files = fileBlockService.getBySubject(subject.getId());
        view.showSubjectDetail(subject, assignments, files);
    }

    private void refreshAssignmentsForSelected() {
        if (selectedSubject == null) return;
        view.refreshAssignments(assignmentService.getBySubject(selectedSubject.getId()));
    }

    private void refreshFilesForSelected() {
        if (selectedSubject == null) return;
        view.refreshFiles(fileBlockService.getBySubject(selectedSubject.getId()));
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
    private void openAssignmentDialog() {
        if (selectedSubject == null) return;
        Window owner = SwingUtilities.getWindowAncestor(view);
        AssignmentInputDialog dialog = new AssignmentInputDialog(owner);
        dialog.setOnSave(this::handleAssignmentSave);
        dialog.setVisible(true);
    }

    private void handleAssignmentSave(AssignmentInputDialog.AssignmentForm form) {
        try {
            assignmentService.createAssignment(
                    selectedSubject.getId(), form.name(), selectedSubject.getColor(),
                    form.deadline(), form.description());
            refreshAssignmentsForSelected();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(view),
                    "과제 생성 실패: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onStatusChange(String blockId, AssignmentStatus status) {
        assignmentService.updateStatus(blockId, status);
        refreshAssignmentsForSelected();
    }

    private void onDeleteAssignment(String blockId) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        int ok = JOptionPane.showConfirmDialog(owner,
                "이 과제를 삭제할까요?\n(캘린더의 마감 표시도 함께 삭제됩니다)",
                "과제 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        assignmentService.deleteAssignment(blockId);
        refreshAssignmentsForSelected();
    }

    // ===== 파일 =====
    private void onAddFile() {
        if (selectedSubject == null) return;
        Window owner = SwingUtilities.getWindowAncestor(view);

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
                "지원 파일 (" + String.join(", ", fileBlockService.getSupportedExtensions()) + ")",
                fileBlockService.getSupportedExtensions().toArray(new String[0])));

        if (chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        try {
            fileBlockService.createFromFile(selectedSubject.getId(), selectedSubject.getColor(), file);
            refreshFilesForSelected();
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
        refreshFilesForSelected();
    }
}
