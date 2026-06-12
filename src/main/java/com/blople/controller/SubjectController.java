package com.blople.controller;

import com.blople.common.EventBus;
import com.blople.dnd.CanvasDropTarget;
import com.blople.model.automation.AutomationRule;
import com.blople.model.automation.ExecutionLog;
import com.blople.model.automation.OpenBlockRef;
import com.blople.model.block.AutomationBlock;
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
import com.blople.service.AutomationService;
import com.blople.service.CanvasService;
import com.blople.service.FileBlockService;
import com.blople.service.SubjectService;
import com.blople.service.TimetableService;
import com.blople.view.canvas.AutomationBlockComponent;
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
 * 과목 페이지 컨트롤러 (FR-03 ~ FR-13).
 */
public class SubjectController {

    private final SubjectService    subjectService;
    private final TimetableService  timetableService;
    private final AssignmentService assignmentService;
    private final FileBlockService  fileBlockService;
    private final CanvasService     canvasService;
    private final AutomationService automationService;
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
                             AutomationService automationService,
                             SubjectPagePanel view) {
        this.subjectService = subjectService;
        this.timetableService = timetableService;
        this.assignmentService = assignmentService;
        this.fileBlockService = fileBlockService;
        this.canvasService = canvasService;
        this.automationService = automationService;
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

        InfiniteCanvasPanel canvas = view.getCanvasPanel();
        canvas.setOnCreateRequest(this::onCanvasCreateText);
        canvas.setOnEditRequest(this::onCanvasDoubleClickBlock);
        canvas.setOnMovedOrResized(this::onCanvasBlockMoved);
        canvas.setOnDeleteRequest(this::onCanvasDeleteBlock);
        canvas.setOnEmptyContextMenu(this::showEmptyContextMenu);
        canvas.setOnBlockContextMenu(this::showBlockContextMenu);
        canvas.setOnBlockDroppedOnBlock(this::onBlockDroppedOnBlock);

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
        for (AutomationService.Placed p : automationService.loadAutomationBlocks(subject.getId())) {
            CanvasState st = p.state();
            comps.add(new AutomationBlockComponent(p.block(), p.rule(),
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

    private void onDeleteFile(String blockId) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        int ok = JOptionPane.showConfirmDialog(owner,
                "이 파일 블록을 삭제할까요?\n(원본 파일은 삭제되지 않으며, 캔버스 배치도 함께 제거됩니다)",
                "파일 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        fileBlockService.delete(blockId);
        refreshExpanded();
        if (selectedSubject != null) loadCanvas(selectedSubject);
    }

    // ===== 캔버스 생성 =====
    private void onCanvasCreateText(Point2D world) {
        if (selectedSubject == null) return;
        createTextAt(world.getX(), world.getY());
    }

    private void onCanvasDropped(BlockType type, String blockId, Point2D world) {
        if (selectedSubject == null) {
            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(view),
                    "먼저 과목을 선택하세요.",
                    "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (type == BlockType.FILE || type == BlockType.LINK) {
            AutomationBlockComponent autoComp = automationBlockAtWorld(world);
            if (autoComp != null) {
                connectToAutomation(autoComp, blockId, type);
                return;
            }
        }

        switch (type) {
            case TEXT       -> createTextAt(world.getX(), world.getY());
            case LINK       -> createLinkAt(world.getX(), world.getY());
            case FILE       -> placeFileAt(blockId, world.getX(), world.getY());
            case AUTOMATION -> createAutomationAt(world.getX(), world.getY());
            default         -> { }
        }
    }

    private AutomationBlockComponent automationBlockAtWorld(Point2D world) {
        InfiniteCanvasPanel canvas = view.getCanvasPanel();
        Point sp = canvas.getViewport().worldToScreen(world.getX(), world.getY());
        CanvasBlockComponent hit = canvas.blockAt(sp.x, sp.y);
        return (hit instanceof AutomationBlockComponent ac) ? ac : null;
    }

    // ===== 캔버스 내부 블록을 다른 블록 위에 놓음 (연결 보완) =====
    private void onBlockDroppedOnBlock(CanvasBlockComponent dragged,
                                       CanvasBlockComponent target,
                                       double originWorldX, double originWorldY) {
        // 파일/링크를 자동화 블록 위에 놓으면 연결
        boolean draggedIsConnectable =
                (dragged instanceof FileBlockComponent) || (dragged instanceof LinkBlockComponent);
        if (draggedIsConnectable && target instanceof AutomationBlockComponent autoComp) {
            BlockType type = (dragged instanceof FileBlockComponent)
                    ? BlockType.FILE : BlockType.LINK;
            connectToAutomation(autoComp, dragged.getBlockId(), type);

            // 끌린 블록은 원래 위치로 복귀 (실행 블록 위에 가려지지 않게)
            dragged.setWorldPos(originWorldX, originWorldY);
            view.getCanvasPanel().repaint();
            return;
        }

        // 연결 대상이 아니면 일반 이동으로 처리 (놓은 자리에 격자 스냅하여 저장)
        Point2D snapped = view.getCanvasPanel().getGridRenderer()
                .snap(dragged.getWorldX(), dragged.getWorldY());
        dragged.setWorldPos(snapped.getX(), snapped.getY());
        onCanvasBlockMoved(dragged);
        view.getCanvasPanel().repaint();
    }

    private void connectToAutomation(AutomationBlockComponent autoComp, String targetBlockId, BlockType type) {
        automationService.connectBlock(autoComp.getRuleId(), targetBlockId, type)
                .ifPresent(ref -> {
                    automationService.getRule(autoComp.getRuleId())
                            .ifPresent(autoComp::setRule);
                    view.getCanvasPanel().repaint();
                });
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

    private void placeFileAt(String fileBlockId, double x, double y) {
        boolean already = canvasService.isFilePlaced(fileBlockId);
        canvasService.placeFileBlock(selectedSubject.getId(), fileBlockId, x, y)
                .ifPresent(placed -> {
                    if (already) {
                        loadCanvas(selectedSubject);
                    } else {
                        CanvasState st = placed.state();
                        view.getCanvasPanel().addBlock(new FileBlockComponent(
                                placed.block(), st.getPosX(), st.getPosY(),
                                st.getWidth(), st.getHeight()));
                    }
                });
    }

    private void createAutomationAt(double x, double y) {
        AutomationService.Placed placed =
                automationService.createAutomationBlock(selectedSubject.getId(), x, y);
        CanvasState st = placed.state();
        view.getCanvasPanel().addBlock(new AutomationBlockComponent(
                placed.block(), placed.rule(), st.getPosX(), st.getPosY(), st.getWidth(), st.getHeight()));
    }

    // ===== 더블클릭: 메모/링크=편집, 파일=열기, 자동화=실행 =====
    private void onCanvasDoubleClickBlock(CanvasBlockComponent comp) {
        if (comp instanceof FileBlockComponent fc) {
            openFileFromCanvas(fc);
        } else if (comp instanceof AutomationBlockComponent ac) {
            executeAutomation(ac);
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

    private void executeAutomation(AutomationBlockComponent ac) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        ExecutionLog log = automationService.execute(ac.getRuleId());
        if (!log.isSuccess()) {
            JOptionPane.showMessageDialog(owner, log.getResultMessage(),
                    "자동화 실행 결과", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ===== 편집 (메모/링크) =====
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

    // ===== 이동/리사이즈 저장 =====
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

    // ===== 삭제 =====
    private void onCanvasDeleteBlock(CanvasBlockComponent comp) {
        Window owner = SwingUtilities.getWindowAncestor(view);

        String message;
        String title;
        if (comp instanceof FileBlockComponent) {
            message = "이 파일 블록을 캔버스에서 치울까요?\n(파일 목록과 원본 파일은 그대로 유지됩니다)";
            title = "캔버스에서 치우기";
        } else if (comp instanceof AutomationBlockComponent) {
            message = "이 실행 블록을 삭제할까요?\n(연결 목록도 함께 삭제됩니다)";
            title = "실행 블록 삭제";
        } else {
            message = "이 블록을 삭제할까요?";
            title = "블록 삭제";
        }

        int ok = JOptionPane.showConfirmDialog(owner, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        if (comp instanceof TextBlockComponent) {
            canvasService.deleteTextBlock(comp.getBlockId());
        } else if (comp instanceof LinkBlockComponent) {
            canvasService.deleteLinkBlock(comp.getBlockId());
        } else if (comp instanceof FileBlockComponent) {
            canvasService.removeFilePlacement(comp.getBlockId());
        } else if (comp instanceof AutomationBlockComponent) {
            automationService.deleteAutomationBlock(comp.getBlockId());
        }
        view.getCanvasPanel().removeBlock(comp);
    }

    // ===== 우클릭 메뉴 =====
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
        JMenuItem addAuto = new JMenuItem("⚡ 실행 블록 추가");
        addAuto.addActionListener(e -> createAutomationAt(world.getX(), world.getY()));

        menu.add(addText);
        menu.add(addLink);
        menu.add(addAuto);
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

        } else if (comp instanceof AutomationBlockComponent ac) {
            JMenuItem run = new JMenuItem("⚡ 실행 (한 번에 열기)");
            run.addActionListener(e -> executeAutomation(ac));
            menu.add(run);
            JMenuItem manage = new JMenuItem("연결 목록 보기/제거");
            manage.addActionListener(e -> showAutomationConnections(ac));
            menu.add(manage);
            menu.addSeparator();
            JMenuItem delete = new JMenuItem("실행 블록 삭제");
            delete.addActionListener(e -> onCanvasDeleteBlock(comp));
            menu.add(delete);

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

    private void showAutomationConnections(AutomationBlockComponent ac) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        AutomationRule rule = automationService.getRule(ac.getRuleId()).orElse(null);
        if (rule == null || rule.getOpenBlocks().isEmpty()) {
            JOptionPane.showMessageDialog(owner,
                    "연결된 블록이 없습니다.\n파일/링크를 실행 블록 위로 끌어다 넣으세요.",
                    "연결 목록", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<OpenBlockRef> refs = rule.getOpenBlocks();
        String[] labels = refs.stream()
                .map(r -> (r.getBlockType() == BlockType.LINK ? "🔗 " : "📄 ") + r.getDisplayName())
                .toArray(String[]::new);

        String chosen = (String) JOptionPane.showInputDialog(owner,
                "제거할 블록을 선택하세요:", "연결 목록 관리",
                JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]);
        if (chosen == null) return;

        int idx = java.util.Arrays.asList(labels).indexOf(chosen);
        if (idx < 0) return;
        OpenBlockRef target = refs.get(idx);

        automationService.disconnectBlock(ac.getRuleId(), target.getBlockId());
        automationService.getRule(ac.getRuleId()).ifPresent(ac::setRule);
        view.getCanvasPanel().repaint();
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
