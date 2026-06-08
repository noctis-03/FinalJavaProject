package com.blople.view.canvas;

import com.blople.common.Constants;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * 무한 캔버스 격자 렌더링 + 격자 스냅 (FR-08, NFR-012).
 *
 * <p>격자는 world 좌표 기준 {@link Constants#CANVAS_GRID_SIZE} 간격이며,
 * 현재 줌/팬 상태에 맞춰 화면에 보이는 영역만 그린다.
 */
public class GridRenderer {

    private static final Color GRID_COLOR = new Color(0xEDEDF2);
    private static final Color GRID_COLOR_MAJOR = new Color(0xDDDDE5);

    private final int gridSize = Constants.CANVAS_GRID_SIZE; // world 단위
    private boolean showGrid = true;

    public void setShowGrid(boolean show) { this.showGrid = show; }
    public boolean isShowGrid()           { return showGrid; }
    public int getGridSize()              { return gridSize; }

    /** 현재 뷰포트에 보이는 격자선을 그린다. */
    public void paint(Graphics2D g2, CanvasViewport vp, int viewW, int viewH) {
        if (!showGrid) return;

        // 화면 좌상단의 world 좌표
        Point2D tl = vp.screenToWorld(0, 0);
        Point2D br = vp.screenToWorld(viewW, viewH);

        double startX = Math.floor(tl.getX() / gridSize) * gridSize;
        double startY = Math.floor(tl.getY() / gridSize) * gridSize;

        // 세로선
        for (double wx = startX; wx <= br.getX(); wx += gridSize) {
            Point top = vp.worldToScreen(wx, tl.getY());
            Point bot = vp.worldToScreen(wx, br.getY());
            boolean major = Math.round(wx / gridSize) % 5 == 0;
            g2.setColor(major ? GRID_COLOR_MAJOR : GRID_COLOR);
            g2.drawLine(top.x, 0, bot.x, viewH);
        }
        // 가로선
        for (double wy = startY; wy <= br.getY(); wy += gridSize) {
            Point left  = vp.worldToScreen(tl.getX(), wy);
            Point right = vp.worldToScreen(br.getX(), wy);
            boolean major = Math.round(wy / gridSize) % 5 == 0;
            g2.setColor(major ? GRID_COLOR_MAJOR : GRID_COLOR);
            g2.drawLine(0, left.y, viewW, right.y);
        }
    }

    /** world 좌표를 가장 가까운 격자점으로 스냅. */
    public Point2D snap(double wx, double wy) {
        double sx = Math.round(wx / gridSize) * gridSize;
        double sy = Math.round(wy / gridSize) * gridSize;
        return new Point2D.Double(sx, sy);
    }
}
