package com.blople.view.canvas;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 무한 캔버스 패널 (FR-08, FR-12).
 *
 * <p>마우스 분기:
 * <ul>
 *   <li>빈 곳 드래그 → 팬</li>
 *   <li>블록 위 드래그 → 이동 (격자 스냅). 단, 손 뗀 위치 아래에 다른 블록이 있으면
 *       "블록 위에 블록 드롭" 콜백 발행 → 컨트롤러가 연결 여부 판단</li>
 *   <li>블록 우하단 핸들 드래그 → 리사이즈</li>
 *   <li>빈 곳 더블클릭 → 새 텍스트 블록 생성 요청 / 블록 더블클릭 → 편집·실행 요청</li>
 *   <li>우클릭 → 컨텍스트 메뉴 요청</li>
 *   <li>Delete → 선택 블록 삭제 요청</li>
 *   <li>휠 → 커서 기준 줌</li>
 * </ul>
 */
public class InfiniteCanvasPanel extends JPanel {

    private static final Color BG = new Color(0xFAFAFC);

    private final CanvasViewport viewport = new CanvasViewport();
    private final GridRenderer gridRenderer = new GridRenderer();
    private final List<CanvasBlockComponent> blocks = new ArrayList<>();

    private enum Mode { NONE, PAN, MOVE, RESIZE }
    private Mode mode = Mode.NONE;
    private Point lastPoint;
    private CanvasBlockComponent active;
    private CanvasBlockComponent selected;

    // MOVE 시작 시점의 world 좌표 (연결 시 원위치 복귀용)
    private double dragOriginWorldX;
    private double dragOriginWorldY;

    // 콜백 (Controller 주입)
    private Consumer<Point2D> onCreateRequest;
    private Consumer<CanvasBlockComponent> onEditRequest;
    private Consumer<CanvasBlockComponent> onMovedOrResized;
    private Consumer<CanvasBlockComponent> onDeleteRequest;
    private BiConsumer<Point, Point2D> onEmptyContextMenu;
    private BiConsumer<CanvasBlockComponent, Point> onBlockContextMenu;

    /** 블록을 다른 블록 위에 떨어뜨림: (끌린 블록, 대상 블록, 끌린 블록의 원래 worldX, 원래 worldY). */
    private DropOnBlockHandler onBlockDroppedOnBlock;

    @FunctionalInterface
    public interface DropOnBlockHandler {
        void onDrop(CanvasBlockComponent dragged, CanvasBlockComponent target,
                    double originWorldX, double originWorldY);
    }

    public InfiniteCanvasPanel() {
        setBackground(BG);
        installMouseHandlers();
        installKeyBindings();
    }

    public CanvasViewport getViewport()    { return viewport; }
    public GridRenderer  getGridRenderer() { return gridRenderer; }

    public Point2D toSnappedWorld(Point screenPoint) {
        Point2D world = viewport.screenToWorld(screenPoint.x, screenPoint.y);
        return gridRenderer.snap(world.getX(), world.getY());
    }

    /** 화면 좌표에 있는 최상위 블록 반환 (없으면 null). */
    public CanvasBlockComponent blockAt(int screenX, int screenY) {
        return topBlockAt(screenX, screenY);
    }

    // ===== 블록 목록 관리 =====
    public void setBlocks(List<CanvasBlockComponent> list) {
        blocks.clear();
        if (list != null) blocks.addAll(list);
        selected = null;
        active = null;
        repaint();
    }

    public void addBlock(CanvasBlockComponent c) {
        blocks.add(c);
        repaint();
    }

    public void removeBlock(CanvasBlockComponent c) {
        blocks.remove(c);
        if (selected == c) selected = null;
        if (active == c) active = null;
        repaint();
    }

    public void clearBlocks() {
        blocks.clear();
        selected = null;
        active = null;
        repaint();
    }

    public CanvasBlockComponent getSelected() { return selected; }

    // ===== 콜백 주입 =====
    public void setOnCreateRequest(Consumer<Point2D> cb)               { this.onCreateRequest = cb; }
    public void setOnEditRequest(Consumer<CanvasBlockComponent> cb)    { this.onEditRequest = cb; }
    public void setOnMovedOrResized(Consumer<CanvasBlockComponent> cb) { this.onMovedOrResized = cb; }
    public void setOnDeleteRequest(Consumer<CanvasBlockComponent> cb)  { this.onDeleteRequest = cb; }
    public void setOnEmptyContextMenu(BiConsumer<Point, Point2D> cb)   { this.onEmptyContextMenu = cb; }
    public void setOnBlockContextMenu(BiConsumer<CanvasBlockComponent, Point> cb) {
        this.onBlockContextMenu = cb;
    }
    public void setOnBlockDroppedOnBlock(DropOnBlockHandler cb) { this.onBlockDroppedOnBlock = cb; }

    private void installMouseHandlers() {
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) { handlePopup(e); return; }
                if (!SwingUtilities.isLeftMouseButton(e)) return;

                lastPoint = e.getPoint();
                CanvasBlockComponent hit = topBlockAt(e.getX(), e.getY());

                if (hit != null && hit.inResizeHandle(viewport, e.getX(), e.getY())) {
                    active = hit;
                    select(hit);
                    mode = Mode.RESIZE;
                } else if (hit != null) {
                    active = hit;
                    select(hit);
                    mode = Mode.MOVE;
                    // 이동 시작 위치 기억 (연결 시 복귀용)
                    dragOriginWorldX = hit.getWorldX();
                    dragOriginWorldY = hit.getWorldY();
                } else {
                    select(null);
                    mode = Mode.PAN;
                }
            }

            @Override public void mouseDragged(MouseEvent e) {
                if (lastPoint == null) return;
                int dx = e.getX() - lastPoint.x;
                int dy = e.getY() - lastPoint.y;

                switch (mode) {
                    case PAN -> viewport.panByScreen(dx, dy);
                    case MOVE -> {
                        if (active != null) {
                            double dwx = dx / viewport.getScale();
                            double dwy = dy / viewport.getScale();
                            active.setWorldPos(active.getWorldX() + dwx, active.getWorldY() + dwy);
                        }
                    }
                    case RESIZE -> {
                        if (active != null) {
                            double dww = dx / viewport.getScale();
                            double dwh = dy / viewport.getScale();
                            active.setWorldSize(active.getWorldW() + dww, active.getWorldH() + dwh);
                        }
                    }
                    default -> { }
                }
                lastPoint = e.getPoint();
                repaint();
            }

            @Override public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) { handlePopup(e); return; }

                if (mode == Mode.MOVE && active != null) {
                    // 손 뗀 지점 아래에 자기 자신이 아닌 다른 블록이 있으면 "드롭-온-블록"
                    CanvasBlockComponent target = topBlockExcept(e.getX(), e.getY(), active);
                    if (target != null && onBlockDroppedOnBlock != null) {
                        onBlockDroppedOnBlock.onDrop(active, target, dragOriginWorldX, dragOriginWorldY);
                        // 위치 확정/복귀는 컨트롤러가 판단하므로 여기서는 저장하지 않는다.
                        mode = Mode.NONE;
                        active = null;
                        lastPoint = null;
                        repaint();
                        return;
                    }
                    // 일반 이동 종료: 격자 스냅 후 저장
                    Point2D snapped = gridRenderer.snap(active.getWorldX(), active.getWorldY());
                    active.setWorldPos(snapped.getX(), snapped.getY());
                    if (onMovedOrResized != null) onMovedOrResized.accept(active);
                    repaint();

                } else if (mode == Mode.RESIZE && active != null) {
                    if (onMovedOrResized != null) onMovedOrResized.accept(active);
                    repaint();
                }

                mode = Mode.NONE;
                active = null;
                lastPoint = null;
            }

            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    CanvasBlockComponent hit = topBlockAt(e.getX(), e.getY());
                    if (hit != null) {
                        if (onEditRequest != null) onEditRequest.accept(hit);
                    } else {
                        Point2D world = viewport.screenToWorld(e.getX(), e.getY());
                        Point2D snapped = gridRenderer.snap(world.getX(), world.getY());
                        if (onCreateRequest != null) onCreateRequest.accept(snapped);
                    }
                }
            }

            @Override public void mouseMoved(MouseEvent e) {
                CanvasBlockComponent hit = topBlockAt(e.getX(), e.getY());
                if (hit != null && hit.inResizeHandle(viewport, e.getX(), e.getY())) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                } else if (hit != null) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);

        addMouseWheelListener((MouseWheelEvent e) -> {
            double factor = (e.getWheelRotation() < 0) ? 1.1 : 1 / 1.1;
            viewport.zoomAt(e.getX(), e.getY(), factor);
            repaint();
        });
    }

    private void handlePopup(MouseEvent e) {
        requestFocusInWindow();
        Point screen = e.getPoint();
        CanvasBlockComponent hit = topBlockAt(e.getX(), e.getY());
        if (hit != null) {
            select(hit);
            if (onBlockContextMenu != null) onBlockContextMenu.accept(hit, screen);
        } else {
            select(null);
            Point2D snapped = toSnappedWorld(screen);
            if (onEmptyContextMenu != null) onEmptyContextMenu.accept(screen, snapped);
        }
    }

    private void installKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelectedBlock");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deleteSelectedBlock");
        am.put("deleteSelectedBlock", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (selected != null && onDeleteRequest != null) {
                    onDeleteRequest.accept(selected);
                }
            }
        });
    }

    private void select(CanvasBlockComponent c) {
        if (selected != null) selected.setSelected(false);
        selected = c;
        if (c != null) c.setSelected(true);
        repaint();
    }

    /** 화면 좌표에서 가장 위(나중에 추가된) 블록 찾기. */
    private CanvasBlockComponent topBlockAt(int sx, int sy) {
        for (int i = blocks.size() - 1; i >= 0; i--) {
            if (blocks.get(i).contains(viewport, sx, sy)) return blocks.get(i);
        }
        return null;
    }

    /** 특정 블록(except)을 제외하고 화면 좌표 위 최상위 블록 찾기. */
    private CanvasBlockComponent topBlockExcept(int sx, int sy, CanvasBlockComponent except) {
        for (int i = blocks.size() - 1; i >= 0; i--) {
            CanvasBlockComponent c = blocks.get(i);
            if (c != except && c.contains(viewport, sx, sy)) return c;
        }
        return null;
    }

    // ===== 줌 버튼용 =====
    public void zoomIn()  { centerZoom(1.2); }
    public void zoomOut() { centerZoom(1 / 1.2); }
    public void resetView() {
        viewport.resetZoom();
        viewport.setOffset(0, 0);
        repaint();
    }
    private void centerZoom(double factor) {
        viewport.zoomAt(getWidth() / 2, getHeight() / 2, factor);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        gridRenderer.paint(g2, viewport, getWidth(), getHeight());

        for (CanvasBlockComponent c : blocks) {
            c.paint(g2, viewport);
        }

        g2.setColor(new Color(0x999999));
        g2.drawString(String.format("zoom %.0f%%", viewport.getScale() * 100), 8, getHeight() - 8);
    }
}
