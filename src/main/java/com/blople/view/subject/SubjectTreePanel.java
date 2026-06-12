package com.blople.view.subject;

import com.blople.dnd.BlockDragSource;
import com.blople.model.block.AssignmentBlock;
import com.blople.model.block.FileBlock;
import com.blople.model.enums.AssignmentStatus;
import com.blople.model.enums.BlockType;
import com.blople.model.subject.Subject;
import com.blople.view.calendar.BlockRenderer;
import com.blople.view.component.CollapsiblePanel;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 좌측 과목 트리 패널 (FR-03, FR-04, FR-05, FR-08, FR-11).
 *
 * <p>2단 아코디언(과목 → 과제/파일). 파일 행은 무한 캔버스로 드래그하여 배치할 수 있다.
 */
public class SubjectTreePanel extends JPanel {

    private final JButton timetableButton = new JButton("＋ 시간표 입력");
    private final JPanel container = new JPanel();

    private final List<CollapsiblePanel> subjectPanels = new ArrayList<>();

    // 콜백
    private Runnable onTimetableClick;
    private Consumer<Subject> onSubjectSelect;
    private Consumer<Subject> onAddAssignment;
    private BiConsumer<String, AssignmentStatus> onStatusChange;
    private Consumer<String> onDeleteAssignment;
    private Consumer<Subject> onAddFile;
    private Consumer<String> onOpenFile;
    private Consumer<String> onDeleteFile;

    // 데이터 공급자
    private java.util.function.Function<String, List<AssignmentBlock>> assignmentSupplier;
    private java.util.function.Function<String, List<FileBlock>> fileSupplier;

    public SubjectTreePanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xDDDDDD)));

        add(buildHeader(), BorderLayout.NORTH);

        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(Color.WHITE);
        JScrollPane sp = new JScrollPane(container);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        add(sp, BorderLayout.CENTER);
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

    public void setSuppliers(java.util.function.Function<String, List<AssignmentBlock>> assignmentSupplier,
                             java.util.function.Function<String, List<FileBlock>> fileSupplier) {
        this.assignmentSupplier = assignmentSupplier;
        this.fileSupplier = fileSupplier;
    }

    public void setSubjects(List<Subject> subjects) {
        container.removeAll();
        subjectPanels.clear();

        if (subjects.isEmpty()) {
            JLabel empty = new JLabel("<html><div style='color:#aaa;'>시간표 입력으로<br>과목을 추가하세요</div></html>");
            empty.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
            container.add(empty);
        } else {
            for (Subject s : subjects) {
                CollapsiblePanel sp = buildSubjectPanel(s);
                subjectPanels.add(sp);
                container.add(sp);
            }
            container.add(javax.swing.Box.createVerticalGlue());
        }
        container.revalidate();
        container.repaint();
    }

    private CollapsiblePanel buildSubjectPanel(Subject subject) {
        CollapsiblePanel panel = new CollapsiblePanel("📚 " + subject.getName(), false, 0);
        panel.setTitleColor(BlockRenderer.parseColor(subject.getColor(), new Color(0x333333)));

        CollapsiblePanel assignmentSection = new CollapsiblePanel("과제", true, 1);
        JButton addAssignBtn = smallButton("＋");
        addAssignBtn.addActionListener(e -> {
            if (onAddAssignment != null) onAddAssignment.accept(subject);
        });
        assignmentSection.addHeaderAction(addAssignBtn);

        CollapsiblePanel fileSection = new CollapsiblePanel("파일", true, 1);
        JButton addFileBtn = smallButton("＋");
        addFileBtn.addActionListener(e -> {
            if (onAddFile != null) onAddFile.accept(subject);
        });
        fileSection.addHeaderAction(addFileBtn);

        panel.addContent(assignmentSection);
        panel.addContent(fileSection);

        panel.setOnHeaderClick(() -> {
            boolean willExpand = !panel.isExpanded();
            collapseAllExcept(panel);
            panel.setExpanded(willExpand);
            if (willExpand) {
                loadSections(subject, assignmentSection, fileSection);
                if (onSubjectSelect != null) onSubjectSelect.accept(subject);
            }
        });

        return panel;
    }

    private void collapseAllExcept(CollapsiblePanel keep) {
        for (CollapsiblePanel p : subjectPanels) {
            if (p != keep) p.setExpanded(false);
        }
    }

    private void loadSections(Subject subject,
                              CollapsiblePanel assignmentSection,
                              CollapsiblePanel fileSection) {
        assignmentSection.clearContent();
        List<AssignmentBlock> assignments =
                assignmentSupplier != null ? assignmentSupplier.apply(subject.getId()) : List.of();
        if (assignments.isEmpty()) {
            assignmentSection.addContent(emptyLabel("과제 없음"));
        } else {
            for (AssignmentBlock a : assignments) {
                assignmentSection.addContent(buildAssignmentRow(a));
            }
        }

        fileSection.clearContent();
        List<FileBlock> files =
                fileSupplier != null ? fileSupplier.apply(subject.getId()) : List.of();
        if (files.isEmpty()) {
            fileSection.addContent(emptyLabel("파일 없음"));
        } else {
            for (FileBlock f : files) {
                fileSection.addContent(buildFileRow(f));
            }
        }
        revalidate();
        repaint();
    }

    public void reloadExpandedSubject(Subject subject) {
        for (CollapsiblePanel sp : subjectPanels) {
            if (sp.isExpanded()) {
                var comps = sp.getContentPanel().getComponents();
                if (comps.length >= 2
                        && comps[0] instanceof CollapsiblePanel as
                        && comps[1] instanceof CollapsiblePanel fs) {
                    loadSections(subject, as, fs);
                }
                return;
            }
        }
    }

    // ===== 행 빌더 =====
    private JPanel buildAssignmentRow(AssignmentBlock a) {
        JPanel row = rowBase();
        JPanel left = vbox();
        JLabel name = new JLabel(a.getStatus() == AssignmentStatus.DONE
                ? "<html><strike>" + escape(a.getName()) + "</strike></html>" : a.getName());
        name.setFont(new Font("SansSerif", Font.BOLD, 12));
        JLabel dl = new JLabel("마감 " + a.getDeadline()
                + (a.isApproachingDeadline(3) ? " ⚠" : "") + (a.isOverdue() ? " ❗" : ""));
        dl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        dl.setForeground(a.isOverdue() ? new Color(0xE5484D) : new Color(0x888888));
        left.add(name);
        left.add(dl);

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        ctrl.setOpaque(false);
        JComboBox<AssignmentStatus> status = new JComboBox<>(AssignmentStatus.values());
        status.setSelectedItem(a.getStatus());
        status.setFont(new Font("SansSerif", Font.PLAIN, 10));
        status.addActionListener(e -> {
            AssignmentStatus s = (AssignmentStatus) status.getSelectedItem();
            if (s != null && s != a.getStatus() && onStatusChange != null) {
                onStatusChange.accept(a.getId(), s);
            }
        });
        JButton del = smallButton("✕");
        del.addActionListener(e -> {
            if (onDeleteAssignment != null) onDeleteAssignment.accept(a.getId());
        });
        ctrl.add(status);
        ctrl.add(del);

        row.add(left, BorderLayout.CENTER);
        row.add(ctrl, BorderLayout.EAST);
        return row;
    }

    private JPanel buildFileRow(FileBlock f) {
        JPanel row = rowBase();
        boolean valid = f.isFileValid();

        JLabel name = new JLabel(fileIcon(f.getFileType()) + " " + f.getFileName());
        name.setFont(new Font("SansSerif", Font.PLAIN, 12));
        if (!valid) name.setForeground(new Color(0xE5484D));

        // ★ 파일 행을 캔버스로 드래그 — 실제 block_id 운반 (FR-05 + FR-08 + FR-11)
        new BlockDragSource(row,  f::getId, BlockType.FILE);
        new BlockDragSource(name, f::getId, BlockType.FILE);

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        ctrl.setOpaque(false);
        JButton open = smallButton("열기");
        open.setEnabled(valid);
        open.addActionListener(e -> {
            if (onOpenFile != null) onOpenFile.accept(f.getId());
        });
        JButton del = smallButton("✕");
        del.addActionListener(e -> {
            if (onDeleteFile != null) onDeleteFile.accept(f.getId());
        });
        ctrl.add(open);
        ctrl.add(del);

        row.add(name, BorderLayout.CENTER);
        row.add(ctrl, BorderLayout.EAST);
        return row;
    }

    // ===== 헬퍼 =====
    private JPanel rowBase() {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(4, 24, 4, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        return row;
    }

    private JPanel vbox() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    private JButton smallButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setMargin(new java.awt.Insets(1, 4, 1, 4));
        b.setFont(new Font("SansSerif", Font.PLAIN, 10));
        return b;
    }

    private JLabel emptyLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(0xBBBBBB));
        l.setBorder(BorderFactory.createEmptyBorder(4, 24, 4, 8));
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        return l;
    }

    private static String fileIcon(String type) {
        if (type == null) return "📄";
        return switch (type.toLowerCase()) {
            case "pdf" -> "📕";
            case "hwp", "docx", "txt", "md" -> "📝";
            case "pptx" -> "📊";
            case "xlsx" -> "📈";
            case "png", "jpg", "jpeg", "gif" -> "🖼";
            default -> "📄";
        };
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ===== 콜백 주입 =====
    public void setOnTimetableClick(Runnable cb)                          { this.onTimetableClick = cb; }
    public void setOnSubjectSelect(Consumer<Subject> cb)                  { this.onSubjectSelect = cb; }
    public void setOnAddAssignment(Consumer<Subject> cb)                  { this.onAddAssignment = cb; }
    public void setOnStatusChange(BiConsumer<String, AssignmentStatus> cb){ this.onStatusChange = cb; }
    public void setOnDeleteAssignment(Consumer<String> cb)                { this.onDeleteAssignment = cb; }
    public void setOnAddFile(Consumer<Subject> cb)                        { this.onAddFile = cb; }
    public void setOnOpenFile(Consumer<String> cb)                        { this.onOpenFile = cb; }
    public void setOnDeleteFile(Consumer<String> cb)                      { this.onDeleteFile = cb; }
}
