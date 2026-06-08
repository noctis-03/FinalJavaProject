package com.blople.view.canvas;

import com.blople.common.Constants;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * 무한 캔버스의 좌표계 관리 (FR-08).
 *
 * <p>두 좌표계를 변환한다.
 * <ul>
 *   <li><b>world</b>: 블록이 가진 논리 좌표 (팬/줌과 무관)</li>
 *   <li><b>screen</b>: 패널에 실제로 그려지는 픽셀 좌표</li>
 * </ul>
 *
 * <p>변환식:
 * <pre>
 *   screen = (world - offset) * scale
 *   world  = screen / scale + offset
 * </pre>
 * offset 은 "현재 화면 좌상단이 바라보는 world 좌표"이다.
 */
public class CanvasViewport {

    private double offsetX = 0;   // 화면 좌상단이 보는 world X
    private double offsetY = 0;
    private double scale   = Constants.CANVAS_DEFAULT_ZOOM;

    // ===== 좌표 변환 =====
    public Point worldToScreen(double wx, double wy) {
        int sx = (int) Math.round((wx - offsetX) * scale);
        int sy = (int) Math.round((wy - offsetY) * scale);
        return new Point(sx, sy);
    }

    public Point2D screenToWorld(int sx, int sy) {
        double wx = sx / scale + offsetX;
        double wy = sy / scale + offsetY;
        return new Point2D.Double(wx, wy);
    }

    // ===== 팬 (화면을 끌어 이동) =====
    /** 화면 픽셀 이동량(dx,dy)만큼 시점을 이동. */
    public void panByScreen(int dxScreen, int dyScreen) {
        offsetX -= dxScreen / scale;
        offsetY -= dyScreen / scale;
    }

    // ===== 줌 =====
    /**
     * 지정한 화면 좌표(anchor)를 중심으로 줌. 그 지점의 world 좌표가
     * 줌 후에도 같은 화면 위치에 남도록 offset 을 보정한다.
     */
    public void zoomAt(int anchorSx, int anchorSy, double factor) {
        Point2D before = screenToWorld(anchorSx, anchorSy);

        double newScale = clampScale(scale * factor);
        if (newScale == scale) return;
        scale = newScale;

        Point2D after = screenToWorld(anchorSx, anchorSy);
        // anchor 의 world 좌표가 before 와 같도록 offset 보정
        offsetX += before.getX() - after.getX();
        offsetY += before.getY() - after.getY();
    }

    private double clampScale(double s) {
        return Math.max(Constants.CANVAS_MIN_ZOOM, Math.min(Constants.CANVAS_MAX_ZOOM, s));
    }

    public void resetZoom() { scale = Constants.CANVAS_DEFAULT_ZOOM; }

    // ===== 현재 보이는 world 영역 =====
    public Rectangle2D visibleWorldBounds(int viewW, int viewH) {
        Point2D tl = screenToWorld(0, 0);
        Point2D br = screenToWorld(viewW, viewH);
        return new Rectangle2D.Double(
                tl.getX(), tl.getY(),
                br.getX() - tl.getX(), br.getY() - tl.getY());
    }

    // ===== getter / setter =====
    public double getScale()              { return scale; }
    public double getOffsetX()            { return offsetX; }
    public double getOffsetY()            { return offsetY; }
    public void setOffset(double x, double y) { this.offsetX = x; this.offsetY = y; }
    public void setScale(double s)        { this.scale = clampScale(s); }
}
