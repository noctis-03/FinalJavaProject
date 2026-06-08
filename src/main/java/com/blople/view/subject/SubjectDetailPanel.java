package com.blople.view.subject;

import com.blople.model.block.AssignmentBlock;
import com.blople.model.block.FileBlock;
import com.blople.model.enums.AssignmentStatus;
import com.blople.model.subject.Subject;
import com.blople.view.calendar.BlockRenderer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 우측 과목 상세 패널 (FR-03, FR-04, FR-05).
 *
 * <p>상단: 과목 정보.
 * 중앙: 탭 — [과제] / [파일].
 *   - 과제 탭: 과제 목록(상태 콤보 + 삭제) + "과제 추가"
 *   - 파일 탭: 파일 목록(열기 + 삭제) + "파일 추가"
 * 무한 캔버스(FR-08)는 v0.6 에서 별도 탭/영역으로 통합 예정.
 */
public class SubjectDetailPanel extends JPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel infoLabel  = new JLabel();

    private final JTabbedPane tabs = new JTabbedPane();

    // 과제 탭
    private final JButton addAssignmentBtn = new JButton("＋ 과제 추가");
    private final JPanel assignmentList = new JPanel();

    // 파일 탭
    private final JButton addFileBtn = new JButton("＋ 파일 추가");
    private final JPanel fileList = new JPanel();

    private Subject currentSubject;

    private Runnable onAddAssignment;
    private BiConsumer<String, AssignmentStatus> onStatusChange;
    private Consumer<String> onDeleteAssignment;

    private Runnable onAddFile;
    private Consumer<String> onOpenFile;
    private Consumer<String> onDeleteFile;

    public SubjectDetailPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildHeader(), BorderLayout.NORTH);

        tabs.addTab("과제", buildAssignmentTab());
        tabs.addTab("파일", buildFileTab());
        add(tabs, BorderLayout.CENTER);

        showEmpty();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        infoLabel.setForeground(new Color(0x666666));
        info.add(titleLabel);
        info.add(Box.createVerticalStrut(4));
        info.add(infoLabel);
        header.add(info, BorderLayout.WEST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        return header;
    }

    private JPanel buildAssignmentTab() {
        JPanel tab = new JPanel(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addAssignmentBtn.setFocusPainted(false);
        addAssignmentBtn.addActionListener(e -> {
            if (onAddAssignment != null) onAddAssignment.run();
        });
        top.add(addAssignmentBtn);
        tab.add(top, BorderLayout.NORTH);

        assignmentList.setLayout(new BoxLayout(assignmentList, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(assignmentList);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        tab.add(sp, BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildFileTab() {
        JPanel tab = new JPanel(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addFileBtn.setFocusPainted(false);
        addFileBtn.addActionListener(e -> {
            if (onAddFile != null) onAddFile.run();
        });
        top.add(addFileBtn);
        tab.add(top, BorderLayout.NORTH);

        fileList.setLayout(new BoxLayout(fileList, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(fileList);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        tab.add(sp, BorderLayout.CENTER);
        return tab;
    }

    // ===== 표시 =====
    public void showSubject(Subject subject,
                            List<AssignmentBlock> assignments,
                            List<FileBlock> files) {
        this.currentSubject = subject;
        addAssignmentBtn.setEnabled(true);
        addFileBtn.setEnabled(true);

        titleLabel.setText("📚 " + subject.getName());
        titleLabel.setForeground(BlockRenderer.parseColor(subject.getColor(), new Color(0x333333)));
        infoLabel.setText("교수: " + nz(subject.getProfessor()) + "    강의실: " + nz(subject.getClassroom()));

        refreshAssignments(assignments);
        refreshFiles(files);
    }

    public void showEmpty() {
        this.currentSubject = null;
        addAssignmentBtn.setEnabled(false);
        addFileBtn.setEnabled(false);
        titleLabel.setText("과목을 선택하세요");
        titleLabel.setForeground(new Color(0x999999));
        infoLabel.setText(" ");
        assignmentList.removeAll();
        fileList.removeAll();
        assignmentList.revalidate(); assignmentList.repaint();
        fileList.revalidate();       fileList.repaint();
    }

    // ===== 과제 목록 =====
    public void refreshAssignments(List<AssignmentBlock> assignments) {
        assignmentList.removeAll();
        if (assignments.isEmpty()) {
            assignmentList.add(emptyLabel("등록된 과제가 없습니다."));
        } else {
            for (AssignmentBlock a : assignments) {
                assignmentList.add(buildAssignmentRow(a));
                assignmentList.add(Box.createVerticalStrut(6));
            }
        }
        assignmentList.revalidate();
        assignmentList.repaint();
    }

    private JPanel buildAssignmentRow(AssignmentBlock a) {
        JPanel row = baseRow();

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        JLabel name = new JLabel();
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setText(a.getStatus() == AssignmentStatus.DONE
                ? "<html><strike>" + escape(a.getName()) + "</strike></html>"
                : a.getName());
        JLabel dl = new JLabel("마감: " + a.getDeadline()
                + (a.isApproachingDeadline(3) ? "  ⚠ 임박" : "")
                + (a.isOverdue() ? "  ❗지남" : ""));
        dl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dl.setForeground(a.isOverdue() ? new Color(0xE5484D) : new Color(0x666666));
        left.add(name);
        left.add(dl);

        JPanel rightCtrl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightCtrl.setOpaque(false);
        JComboBox<AssignmentStatus> statusCombo = new JComboBox<>(AssignmentStatus.values());
        statusCombo.setSelectedItem(a.getStatus());
        statusCombo.addActionListener(e -> {
            AssignmentStatus s = (AssignmentStatus) statusCombo.getSelectedItem();
            if (s != null && s != a.getStatus() && onStatusChange != null) {
                onStatusChange.accept(a.getId(), s);
            }
        });
        JButton del = new JButton("삭제");
        del.setFocusPainted(false);
        del.addActionListener(e -> {
            if (onDeleteAssignment != null) onDeleteAssignment.accept(a.getId());
        });
        rightCtrl.add(statusCombo);
        rightCtrl.add(del);

        row.add(left, BorderLayout.CENTER);
        row.add(rightCtrl, BorderLayout.EAST);
        return row;
    }

    // ===== 파일 목록 =====
    public void refreshFiles(List<FileBlock> files) {
        fileList.removeAll();
        if (files.isEmpty()) {
            fileList.add(emptyLabel("등록된 파일이 없습니다."));
        } else {
            for (FileBlock f : files) {
                fileList.add(buildFileRow(f));
                fileList.add(Box.createVerticalStrut(6));
            }
        }
        fileList.revalidate();
        fileList.repaint();
    }

    private JPanel buildFileRow(FileBlock f) {
        JPanel row = baseRow();
        boolean valid = f.isFileValid();

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        JLabel name = new JLabel(fileIcon(f.getFileType()) + " " + f.getFileName());
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel meta = new JLabel(
                (f.getFileType() == null ? "" : f.getFileType().toUpperCase() + "  ")
                        + humanSize(f.getFileSize())
                        + (valid ? "" : "   ⚠ 파일을 찾을 수 없음"));
        meta.setFont(new Font("SansSerif", Font.PLAIN, 12));
        meta.setForeground(valid ? new Color(0x666666) : new Color(0xE5484D));
        left.add(name);
        left.add(meta);

        JPanel rightCtrl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightCtrl.setOpaque(false);
        JButton open = new JButton("열기");
        open.setFocusPainted(false);
        open.setEnabled(valid);
        open.addActionListener(e -> {
            if (onOpenFile != null) onOpenFile.accept(f.getId());
        });
        JButton del = new JButton("삭제");
        del.setFocusPainted(false);
        del.addActionListener(e -> {
            if (onDeleteFile != null) onDeleteFile.accept(f.getId());
        });
        rightCtrl.add(open);
        rightCtrl.add(del);

        row.add(left, BorderLayout.CENTER);
        row.add(rightCtrl, BorderLayout.EAST);
        return row;
    }

    // ===== 공통 헬퍼 =====
    private JPanel baseRow() {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE0E0E0)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        return row;
    }

    private JLabel emptyLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(0xAAAAAA));
        l.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
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

    private static String humanSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int i = (int) (Math.log10(bytes) / Math.log10(1024));
        i = Math.min(i, units.length - 1);
        double v = bytes / Math.pow(1024, i);
        return String.format("%.1f %s", v, units[i]);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String nz(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    public Subject getCurrentSubject() { return currentSubject; }

    // 콜백 주입
    public void setOnAddAssignment(Runnable cb)                           { this.onAddAssignment = cb; }
    public void setOnStatusChange(BiConsumer<String, AssignmentStatus> cb){ this.onStatusChange = cb; }
    public void setOnDeleteAssignment(Consumer<String> cb)                { this.onDeleteAssignment = cb; }
    public void setOnAddFile(Runnable cb)                                 { this.onAddFile = cb; }
    public void setOnOpenFile(Consumer<String> cb)                        { this.onOpenFile = cb; }
    public void setOnDeleteFile(Consumer<String> cb)                      { this.onDeleteFile = cb; }
}
