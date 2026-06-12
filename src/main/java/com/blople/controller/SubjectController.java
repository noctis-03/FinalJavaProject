package com.blople.controller;

import com.blople.common.EventBus;
import com.blople.dnd.CanvasDropTarget;
import com.blople.model.block.FileBlock;
import com.blople.model.block.LinkBlock;
import com.blople.model.block.TextBlock;
import com.blople.model.canvas.CanvasState;
import com.blople.model.enums.AssignmentStatus;
import com.blople.model.enums.BlockType;
import com.blople.model.subject.Semester;
import com.blople.model.subject.Subject;
import com.blople.model.subject.Timetable;
import com.blople.service.AssignmentService;
import com.blople.service.CanvasService;
import com.blople.service.FileBlockService;
import com.blople.service.SubjectService;
import com.blople.service.TimetableService;
import com.blople.view.canvas.CanvasBlockComponent;
import com.blople.view.canvas.FileBlockComponent;
import com.blople.view.canvas.InfiniteCanvasPanel;
import com.blople.view.canvas.LinkBlockComponent;
import com.blople.view.canvas.LinkBlockEditDialog;
import com.blople.view.canvas.TextBlockComponent;
import com.blople.view.canvas.TextBlockEditDialog;
import com.blople.view.subject.AssignmentInputDialog;
import com.blople.view.subject.SubjectPagePanel;
import com.blople.view.subject.TimetableInputDialog;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Point;
import java.awt.Window;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 과목 페이지 컨트롤러 (FR-03 ~ FR-11).
 *
 * <p>좌측 2단 트리(과목 → 과제/파일) + 우측 무한 캔버스(텍스트/링크/파일 블록) 연동.
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

    private CanvasDropTarget canvasDropTarget;

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
        canvas.setOnEditRequest(this::onCanvasDoubleClickBlock);
        canvas.setOnMovedOrResized(this::onCanvasBlockMoved);
        canvas.setOnDeleteRequest(this::onCanvasDeleteBlock);
        canvas.setOnEmptyContextMenu(this::showEmptyContextMenu);
        canvas.setOnBlockContextMenu(this::showBlockContextMenu);

        // 팔레트/파일 → 캔버스 드롭 (FR-11)
        canvasDropTarget = new CanvasDropTarget(canvas);
        canvasDropTarget.setOnDrop(this::onCanvasDropped);

        eventBus.subscribe(EventBus.SUBJECT_CHANGED, p -> SwingUtilities.invokeLater(this::reloadSubjects));

        reloadSubjects();
    }

    private void reloadSubjects() {
        List<Subject> subjects = subjectService.getSubjectsBySemester(activeSemester.getId());
        view.setSubjects(subjects);
    }

    // ===== 과목 선택 → 헤더 + 캔버스 로드 =====
    private void onSubjectSelect(Subject subject) {
        this.selectedSubject = subject;
        view.showSubjectInHeader(subject);
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
        for (CanvasService.Placed<LinkBlock> p : canvasService.loadLinkBlocks(subject.getId())) {
            CanvasState st = p.state();
            comps.add(new LinkBlockComponent(p.block(),
                    st.getPosX(), st.getPosY(), st.getWidth(), st.getHeight()));
        }
        for (CanvasService.Placed<FileBlock> p : canvasService.loadFileBlocks(subject.getId())) {
            CanvasState st = p.state();
            comps.add(new FileBlockComponent(p.block(),
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

    // ===== 파일 (트리) =====
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

    /** 트리에서 파일 블록 자체 삭제 — 캔버스 배치도 함께 사라짐(canvas_state CASCADE). */
    private void onDeleteFile(String blockId) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        int ok = JOptionPane.showConfirmDialog(owner,
                "이 파일 블록을 삭제할까요?\n(원본 파일은 삭제되지 않으며, 캔버스 배치도 함께 제거됩니다)",
                "파일 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        fileBlockService.delete(blockId);
        refreshExpanded();
        // 캔버스에 떠 있던 동일 블록 컴포넌트가 있으면 다시 로드해 정리
        if (selectedSubject != null) loadCanvas(selectedSubject);
    }

    // ===== 캔버스 생성 (FR-06, FR-07, FR-08, FR-11) =====
    private void onCanvasCreateText(Point2D world) {
        if (selectedSubject == null) return;
        createTextAt(world.getX(), world.getY());
    }

    /** 팔레트(메모/링크) 또는 트리 파일 블록이 캔버스에 드롭됨. */
    private void onCanvasDropped(BlockType type, String blockId, Point2D world) {
        if (selectedSubject == null) {
            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(view),
                    "먼저 과목을 선택하세요.",
                    "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        switch (type) {
            case TEXT -> createTextAt(world.getX(), world.getY());
            case LINK -> createLinkAt(world.getX(), world.getY());
            case FILE -> placeFileAt(blockId, world.getX(), world.getY());
            default -> { /* 미지원 종류 무시 */ }
        }
    }

    private void createTextAt(double x, double y) {
        CanvasService.Placed<TextBlock> placed =
                canvasService.createTextBlock(selectedSubject.getId(), x, y);
        CanvasState st = placed.state();
        view.getCanvasPanel().addBlock(new TextBlockComponent(
                placed.block(), st.getPosX(), st.getPosY(), st.getWidth(), st.getHeight()));
    }

    private void createLinkAt(double x, double y) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        LinkBlockEditDialog dialog = new LinkBlockEditDialog(owner);
        dialog.setOnSave((url, displayName) -> {
            try {
                CanvasService.Placed<LinkBlock> placed =
                        canvasService.createLinkBlock(selectedSubject.getId(), url, displayName, x, y);
                CanvasState st = placed.state();
                view.getCanvasPanel().addBlock(new LinkBlockComponent(
                        placed.block(), st.getPosX(), st.getPosY(), st.getWidth(), st.getHeight()));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(owner, ex.getMessage(),
                        "링크 생성 실패", JOptionPane.WARNING_MESSAGE);
            }
        });
        dialog.setVisible(true);
    }

    /** 트리의 기존 파일 블록을 캔버스에 배치 (FR-05 + FR-08). */
    private void placeFileAt(String fileBlockId, double x, double y) {
        // 이미 캔버스에 떠 있으면 위치만 갱신 (중복 컴포넌트 방지)
        boolean already = canvasService.isFilePlaced(fileBlockId);

        canvasService.placeFileBlock(selectedSubject.getId(), fileBlockId, x, y)
                .ifPresent(placed -> {
                    if (already) {
                        // 이미 화면에 있던 컴포넌트는 다시 로드해 새 위치로 반영
                        loadCanvas(selectedSubject);
                    } else {
                        CanvasState st = placed.state();
                        view.getCanvasPanel().addBlock(new FileBlockComponent(
                                placed.block(), st.getPosX(), st.getPosY(),
                                st.getWidth(), st.getHeight()));
                    }
                });
    }

    // ===== 캔버스 더블클릭: 메모/링크는 편집, 파일은 열기 =====
    private void onCanvasDoubleClickBlock(CanvasBlockComponent comp) {
        if (comp instanceof FileBlockComponent fc) {
            openFileFromCanvas(fc);
        } else {
            onCanvasEditBlock(comp);
        }
    }

    private void openFileFromCanvas(FileBlockComponent fc) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        try {
            fc.getModel().openFile();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(owner, ex.getMessage(),
                    "파일 열기 실패", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ===== 캔버스 편집 (메모/링크) =====
    private void onCanvasEditBlock(CanvasBlockComponent comp) {
        Window owner = SwingUtilities.getWindowAncestor(view);

        if (comp instanceof TextBlockComponent tc) {
            TextBlock model = tc.getModel();
            TextBlockEditDialog dialog =
                    new TextBlockEditDialog(owner, model.getTitle(), model.getContent());
            dialog.setOnSave((title, content) -> {
                canvasService.updateTextContent(model.getId(), title, content);
                model.setTitle(title);
                model.setContent(content);
                view.getCanvasPanel().repaint();
            });
            dialog.setVisible(true);

        } else if (comp instanceof LinkBlockComponent lc) {
            LinkBlock model = lc.getModel();
            LinkBlockEditDialog dialog =
                    new LinkBlockEditDialog(owner, model.getUrl(), model.getDisplayName());
            dialog.setOnSave((url, displayName) -> {
                try {
                    canvasService.updateLink(model.getId(), url, displayName);
                    model.setUrl(url);
                    String name = (displayName == null || displayName.isBlank()) ? url : displayName;
                    model.setDisplayName(name);
                    model.setName(name);
                    view.getCanvasPanel().repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(owner, ex.getMessage(),
                            "링크 수정 실패", JOptionPane.WARNING_MESSAGE);
                }
            });
            dialog.setVisible(true);
        }
    }

    // ===== 캔버스 이동/리사이즈 저장 =====
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

    // ===== 캔버스 삭제 (Delete 키 / 메뉴) =====
    private void onCanvasDeleteBlock(CanvasBlockComponent comp) {
        Window owner = SwingUtilities.getWindowAncestor(view);

        // 파일 블록은 "캔버스에서만 치우기"(트리엔 남김)
        boolean isFile = comp instanceof FileBlockComponent;
        String message = isFile
                ? "이 파일 블록을 캔버스에서 치울까요?\n(파일 목록과 원본 파일은 그대로 유지됩니다)"
                : "이 블록을 삭제할까요?";

        int ok = JOptionPane.showConfirmDialog(owner, message,
                isFile ? "캔버스에서 치우기" : "블록 삭제",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        if (comp instanceof TextBlockComponent) {
            canvasService.deleteTextBlock(comp.getBlockId());
        } else if (comp instanceof LinkBlockComponent) {
            canvasService.deleteLinkBlock(comp.getBlockId());
        } else if (comp instanceof FileBlockComponent) {
            canvasService.removeFilePlacement(comp.getBlockId());  // 배치만 제거
        }
        view.getCanvasPanel().removeBlock(comp);
    }

    // ===== 캔버스 우클릭 메뉴 =====
    private void showEmptyContextMenu(Point screen, Point2D world) {
        if (selectedSubject == null) {
            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(view),
                    "먼저 과목을 선택하세요.",
                    "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JPopupMenu menu = new JPopupMenu();

        JMenuItem addText = new JMenuItem("📝 메모 추가");
        addText.addActionListener(e -> createTextAt(world.getX(), world.getY()));

        JMenuItem addLink = new JMenuItem("🔗 링크 추가");
        addLink.addActionListener(e -> createLinkAt(world.getX(), world.getY()));

        menu.add(addText);
        menu.add(addLink);
        menu.show(view.getCanvasPanel(), screen.x, screen.y);
    }

    private void showBlockContextMenu(CanvasBlockComponent comp, Point screen) {
        JPopupMenu menu = new JPopupMenu();

        if (comp instanceof FileBlockComponent fc) {
            JMenuItem open = new JMenuItem("📂 파일 열기");
            open.addActionListener(e -> openFileFromCanvas(fc));
            open.setEnabled(fc.getModel().isFileValid());
            menu.add(open);

            menu.addSeparator();

            JMenuItem remove = new JMenuItem("캔버스에서 치우기");
            remove.addActionListener(e -> onCanvasDeleteBlock(comp));
            menu.add(remove);

        } else {
            JMenuItem edit = new JMenuItem("편집");
            edit.addActionListener(e -> onCanvasEditBlock(comp));
            menu.add(edit);

            if (comp instanceof LinkBlockComponent lc) {
                JMenuItem openLink = new JMenuItem("🔗 브라우저에서 열기");
                openLink.addActionListener(e -> openLinkInBrowser(lc));
                menu.add(openLink);
            }

            menu.addSeparator();

            JMenuItem delete = new JMenuItem("삭제");
            delete.addActionListener(e -> onCanvasDeleteBlock(comp));
            menu.add(delete);
        }

        menu.show(view.getCanvasPanel(), screen.x, screen.y);
    }

    private void openLinkInBrowser(LinkBlockComponent lc) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        try {
            lc.getModel().openInBrowser();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner,
                    "브라우저를 열 수 없습니다: " + ex.getMessage(),
                    "링크 열기 실패", JOptionPane.WARNING_MESSAGE);
        }
    }
}
