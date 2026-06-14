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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 좌측 과목 트리 패널.
 *
 * 핵심 수정:
 *  - 파일/과제 추가 시 SUBJECT_CHANGED 이벤트로 setSubjects()가 다시 호출되면서
 *    아코디언이 닫히는 버그를 해결.
 *  - 마지막으로 펼쳐져 있던 과목 id를 기억해두고 다시 그릴 때 자동 복원.
 */
public class SubjectTreePanel extends JPanel {

    private static final int PANEL_WIDTH = 320;

    private static final Color ROOT_BG = new Color(0xF6F8FC);
    private static final Color HEADER_BG = new Color(0xF6F8FC);
    private static final Color PANEL_BORDER = new Color(0xD8E0EB);
    private static final Color ROW_BG = Color.WHITE;
    private static final Color ROW_BORDER = new Color(0xEEF2F7);
    private static final Color MUTED = new Color(0x7C8798);
    private static final Color SUBTLE = new Color(0xA0AABA);
    private static final Color DANGER = new Color(0xE5484D);

    private final JButton timetableButton = new JButton("＋ 시간표 수정");
    private final JPanel container = new JPanel();

    /** 현재 펼쳐져 있는 과목 id → 해당 CollapsiblePanel 매핑. */
    private final Map<String, CollapsiblePanel> subjectPanels = new LinkedHashMap<>();

    /** 가장 최근에 펼쳐져 있던 과목 id. 재구성 후 복원에 사용. */
    private String expandedSubjectId;

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
        setPreferredSize(new Dimension(PANEL_WIDTH, 0));
        setBackground(ROOT_BG);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, PANEL_BORDER));

        add(buildHeader(), BorderLayout.NORTH);

        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(ROOT_BG);
        container.setBorder(BorderFactory.createEmptyBorder(10, 10, 16, 10));

        JScrollPane sp = new JScrollPane(container);
        sp.setBorder(null);
        sp.getViewport().setBackground(ROOT_BG);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.setBackground(HEADER_BG);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, PANEL_BORDER));

        stylePrimaryButton(timetableButton);
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

    /**
     * 과목 목록을 다시 그린다. 기존에 열려 있던 과목이 새 목록에도 존재한다면
     * 그 과목을 자동으로 다시 펼친다.
     */
    public void setSubjects(List<Subject> subjects) {
        // 새로 그리기 전 "지금 열려있는 과목 id"를 한 번 더 확인해 둔다.
        // (외부에서 expand 콜백을 거치지 않고 닫혔을 가능성 대비)
        String idToRestore = expandedSubjectId;

        container.removeAll();
        subjectPanels.clear();

        if (subjects.isEmpty()) {
            container.add(buildEmptyState());
            container.revalidate();
            container.repaint();
            return;
        }

        Subject toExpand = null;

        for (int i = 0; i < subjects.size(); i++) {
            Subject s = subjects.get(i);
            CollapsiblePanel sp = buildSubjectPanel(s);
            subjectPanels.put(s.getId(), sp);
            container.add(sp);
            if (i < subjects.size() - 1) {
                container.add(Box.createVerticalStrut(10));
            }
            if (idToRestore != null && idToRestore.equals(s.getId())) {
                toExpand = s;
            }
        }
        container.add(Box.createVerticalGlue());

        container.revalidate();
        container.repaint();

        // ★ 핵심: 열려 있던 과목 복원 (외부 콜백은 호출하지 않음)
        if (toExpand != null) {
            CollapsiblePanel sp = subjectPanels.get(toExpand.getId());
            if (sp != null) {
                expandSubjectPanel(sp, toExpand, false);
            }
        }
    }

    private JComponent buildEmptyState() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE4EAF2), 1),
                BorderFactory.createEmptyBorder(22, 18, 22, 18)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        JLabel icon = new JLabel("📚", SwingConstants.CENTER);
        icon.setAlignmentX(CENTER_ALIGNMENT);
        icon.setFont(new Font("SansSerif", Font.PLAIN, 26));

        JLabel title = new JLabel("아직 과목이 없습니다", SwingConstants.CENTER);
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(new Color(0x334155));

        JLabel desc = new JLabel(
                "<html><div style='text-align:center;'>상단의 <b>시간표 수정</b> 버튼으로<br>과목을 먼저 추가해 주세요.</div></html>",
                SwingConstants.CENTER);
        desc.setAlignmentX(CENTER_ALIGNMENT);
        desc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        desc.setForeground(MUTED);

        card.add(icon);
        card.add(Box.createVerticalStrut(8));
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(desc);
        return card;
    }

    private CollapsiblePanel buildSubjectPanel(Subject subject) {
        CollapsiblePanel panel = new CollapsiblePanel("● " + subject.getName(), false, 0);
        panel.setTitleColor(BlockRenderer.parseColor(subject.getColor(), new Color(0x333333)));

        CollapsiblePanel assignmentSection = new CollapsiblePanel("📝 과제", true, 1);
        JButton addAssignBtn = iconButton("＋");
        addAssignBtn.addActionListener(e -> {
            if (onAddAssignment != null) onAddAssignment.accept(subject);
        });
        assignmentSection.addHeaderAction(addAssignBtn);

        CollapsiblePanel fileSection = new CollapsiblePanel("📁 파일", true, 1);
        JButton addFileBtn = iconButton("＋");
        addFileBtn.addActionListener(e -> {
            if (onAddFile != null) onAddFile.accept(subject);
        });
        fileSection.addHeaderAction(addFileBtn);

        panel.addContent(assignmentSection);
        panel.addContent(Box.createVerticalStrut(8));
        panel.addContent(fileSection);

        panel.setOnHeaderClick(() -> {
            // CollapsiblePanel 가 이미 토글한 "이후" 상태가 expanded
            boolean nowExpanded = panel.isExpanded();
            if (nowExpanded) {
                collapseAllExcept(panel);
                expandedSubjectId = subject.getId();
                loadSections(subject, assignmentSection, fileSection);
                if (onSubjectSelect != null) onSubjectSelect.accept(subject);
            } else {
                // 사용자가 다시 눌러서 닫은 경우
                if (subject.getId().equals(expandedSubjectId)) {
                    expandedSubjectId = null;
                }
            }
        });

        return panel;
    }

    /**
     * 코드상에서 특정 과목 패널을 펼친다.
     * @param fireSelectCallback true 면 onSubjectSelect 콜백도 호출 (사용자 클릭처럼 동작)
     */
    private void expandSubjectPanel(CollapsiblePanel panel, Subject subject, boolean fireSelectCallback) {
        collapseAllExcept(panel);
        panel.setExpanded(true);
        expandedSubjectId = subject.getId();

        var comps = panel.getContentPanel().getComponents();
        CollapsiblePanel as = null, fs = null;
        for (var c : comps) {
            if (c instanceof CollapsiblePanel cp) {
                if (as == null) as = cp;
                else { fs = cp; break; }
            }
        }
        if (as != null && fs != null) {
            loadSections(subject, as, fs);
        }
        if (fireSelectCallback && onSubjectSelect != null) {
            onSubjectSelect.accept(subject);
        }
    }

    private void collapseAllExcept(CollapsiblePanel keep) {
        for (CollapsiblePanel p : subjectPanels.values()) {
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
            assignmentSection.addContent(emptyLabel("등록된 과제가 없습니다"));
        } else {
            for (AssignmentBlock a : assignments) {
                assignmentSection.addContent(buildAssignmentRow(a));
                assignmentSection.addContent(Box.createVerticalStrut(6));
            }
        }

        fileSection.clearContent();
        List<FileBlock> files =
                fileSupplier != null ? fileSupplier.apply(subject.getId()) : List.of();
        if (files.isEmpty()) {
            fileSection.addContent(emptyLabel("등록된 파일이 없습니다"));
        } else {
            for (FileBlock f : files) {
                fileSection.addContent(buildFileRow(f));
                fileSection.addContent(Box.createVerticalStrut(6));
            }
        }
        revalidate();
        repaint();
    }

    /**
     * 외부에서 호출. 현재 펼쳐져 있는 과목의 과제/파일 섹션만 다시 로드.
     */
    public void reloadExpandedSubject(Subject subject) {
        if (subject == null) return;
        CollapsiblePanel sp = subjectPanels.get(subject.getId());
        if (sp == null || !sp.isExpanded()) return;

        var comps = sp.getContentPanel().getComponents();
        CollapsiblePanel as = null, fs = null;
        for (var c : comps) {
            if (c instanceof CollapsiblePanel cp) {
                if (as == null) as = cp;
                else { fs = cp; break; }
            }
        }
        if (as != null && fs != null) {
            loadSections(subject, as, fs);
        }
    }

    // ===== 행 빌더 =====
    private JPanel buildAssignmentRow(AssignmentBlock a) {
        JPanel row = rowBase();

        JPanel textBox = vbox();
        JLabel name = new JLabel(a.getStatus() == AssignmentStatus.DONE
                ? "<html><strike>" + escape(a.getName()) + "</strike></html>"
                : escape(a.getName()));
        name.setFont(new Font("SansSerif", Font.BOLD, 12));
        name.setForeground(new Color(0x1F2937));
        name.setToolTipText(a.getName());

        JLabel deadline = new JLabel(buildDeadlineText(a));
        deadline.setFont(new Font("SansSerif", Font.PLAIN, 11));
        deadline.setForeground(a.isOverdue() ? DANGER : MUTED);
        deadline.setToolTipText(a.getDeadline());

        textBox.add(name);
        textBox.add(Box.createVerticalStrut(3));
        textBox.add(deadline);

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        ctrl.setOpaque(false);

        JComboBox<AssignmentStatus> status = new JComboBox<>(AssignmentStatus.values());
        status.setSelectedItem(a.getStatus());
        status.setFont(new Font("SansSerif", Font.PLAIN, 11));
        status.setFocusable(false);
        status.setPreferredSize(new Dimension(108, 28));
        status.addActionListener(e -> {
            AssignmentStatus s = (AssignmentStatus) status.getSelectedItem();
            if (s != null && s != a.getStatus() && onStatusChange != null) {
                onStatusChange.accept(a.getId(), s);
            }
        });

        JButton del = iconButton("✕");
        del.addActionListener(e -> {
            if (onDeleteAssignment != null) onDeleteAssignment.accept(a.getId());
        });

        ctrl.add(status);
        ctrl.add(del);

        row.add(textBox, BorderLayout.CENTER);
        row.add(ctrl, BorderLayout.EAST);
        return row;
    }

    private JPanel buildFileRow(FileBlock f) {
        JPanel row = rowBase();
        boolean valid = f.isFileValid();

        JPanel textBox = vbox();

        JLabel name = new JLabel(fileIcon(f.getFileType()) + " " + escape(f.getFileName()));
        name.setFont(new Font("SansSerif", Font.BOLD, 12));
        name.setForeground(valid ? new Color(0x1F2937) : DANGER);
        name.setToolTipText(f.getFileName());

        JLabel meta = new JLabel(valid ? "캔버스로 드래그 가능" : "파일 경로를 찾을 수 없습니다");
        meta.setFont(new Font("SansSerif", Font.PLAIN, 11));
        meta.setForeground(valid ? MUTED : DANGER);

        textBox.add(name);
        textBox.add(Box.createVerticalStrut(3));
        textBox.add(meta);

        new BlockDragSource(row, f::getId, BlockType.FILE);
        new BlockDragSource(name, f::getId, BlockType.FILE);

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        ctrl.setOpaque(false);

        JButton open = pillButton("열기");
        open.setEnabled(valid);
        open.addActionListener(e -> {
            if (onOpenFile != null) onOpenFile.accept(f.getId());
        });

        JButton del = iconButton("✕");
        del.addActionListener(e -> {
            if (onDeleteFile != null) onDeleteFile.accept(f.getId());
        });

        ctrl.add(open);
        ctrl.add(del);

        row.add(textBox, BorderLayout.CENTER);
        row.add(ctrl, BorderLayout.EAST);
        return row;
    }

    // ===== 헬퍼 =====
    private JPanel rowBase() {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(ROW_BG);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ROW_BORDER, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 10)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        row.setPreferredSize(new Dimension(PANEL_WIDTH - 36, 72));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        return row;
    }

    private JPanel vbox() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    private JButton iconButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(new Color(0x56657A));
        b.setBackground(Color.WHITE);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD9E2EE), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        b.setPreferredSize(new Dimension(30, 26));
        return b;
    }

    private JButton pillButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setFont(new Font("SansSerif", Font.PLAIN, 11));
        b.setForeground(new Color(0x2D5BBA));
        b.setBackground(new Color(0xF3F8FF));
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCFE0FF), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        return b;
    }

    private void stylePrimaryButton(JButton b) {
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(new Color(0x23406A));
        b.setBackground(Color.WHITE);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD6E0EE), 1),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
    }

    private JLabel emptyLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(SUBTLE);
        l.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        return l;
    }

    private String buildDeadlineText(AssignmentBlock a) {
        String deadline = trimDeadline(a.getDeadline());
        String badge = "";
        if (a.isOverdue()) {
            badge = "  ·  지남";
        } else if (a.isApproachingDeadline(3)) {
            badge = "  ·  임박";
        }
        return "마감 " + deadline + badge;
    }

    private String trimDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) return "-";
        String v = deadline.trim();
        if (v.length() >= 16) return v.substring(0, 16);
        return v;
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
