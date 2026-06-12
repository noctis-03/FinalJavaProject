package com.blople.view.canvas;

import com.blople.model.block.FileBlock;
import com.blople.view.calendar.BlockRenderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * 캔버스 위 파일 블록 (FR-05, FR-08).
 *
 * <p>좌측 트리의 파일 블록을 캔버스로 드래그해 배치한 결과를 렌더링한다.
 * 같은 블록이 트리와 캔버스 양쪽에 존재할 수 있으며(배치만 추가됨),
 * 더블클릭하면 시스템 기본 프로그램으로 파일을 연다(컨트롤러에서 처리).
 *
 * <p>원본 파일이 삭제/이동되어 찾을 수 없으면 경고를 표시한다.
 */
public class FileBlockComponent extends CanvasBlockComponent {

    private static final Color FILE_BG       = new Color(0xF3F5F9);
    private static final Color INVALID_BG    = new Color(0xFBE9E9);

    private final FileBlock model;

    public FileBlockComponent(FileBlock model, double x, double y, double w, double h) {
        super(model.getId(), x, y, w, h);
        this.model = model;
    }

    public FileBlock getModel() { return model; }

    @Override
    protected Color bodyColor() {
        return model.isFileValid() ? FILE_BG : INVALID_BG;
    }

    @Override
    protected String titleText() {
        return fileIcon(model.getFileType()) + " " + safe(model.getFileName());
    }

    @Override
    protected void paintBody(Graphics2D g2, int sx, int sy, int sw, int sh) {
        boolean valid = model.isFileValid();

        // 과목 색상 띠 (좌측)
        Color accent = BlockRenderer.parseColor(model.getColor(), new Color(0x4D96FF));
        g2.setColor(accent);
        g2.fillRect(sx + 1, sy + 1, 4, sh - 2);

        // 메타 정보
        g2.setColor(new Color(0x666666));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        String type = model.getFileType() == null ? "" : model.getFileType().toUpperCase();
        String meta = (type.isEmpty() ? "" : type + "  ") + humanSize(model.getFileSize());
        g2.drawString(clip(g2, meta, sw - 12), sx + 8, sy + 34);

        if (!valid) {
            g2.setColor(new Color(0xE5484D));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString(clip(g2, "⚠ 파일을 찾을 수 없음", sw - 12), sx + 8, sy + 50);
        } else if (sh >= 56) {
            g2.setColor(new Color(0x999999));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString("더블클릭하여 열기", sx + 8, sy + 50);
        }
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

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "파일" : s;
    }
}
