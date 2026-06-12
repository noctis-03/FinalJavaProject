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
 * 무한 캔버스 패널 (FR-08).
 *
 * <p>격자 + 팬/줌 위에 {@link CanvasBlockComponent} 들을 그린다.
 * 마우스 분기:
 * <ul>
 *   <li>빈 곳 드래그 → 팬</li>
 *   <li>블록 위 드래그 → 이동 (격자 스냅)</li>
 *   <li>블록 우하단 핸들 드래그 → 리사이즈</li>
 *   <li>빈 곳 더블클릭 → 새 텍스트 블록 생성 요청</li>
 *   <li>블록 더블클릭 → 편집 요청</li>
 *   <li>우클릭(빈 곳/블록) → 컨텍스트 메뉴 요청</li>
 *   <li>Delete 키 → 선택 블록 삭제 요청</li>
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

    // 콜백 (Controller 주입)
    private Consumer<Point2D> onCreateRequest;                 // 빈 곳 더블클릭(world 좌표)
    private Consumer<CanvasBlockComponent> onEditRequest;      // 블록 더블클릭
    private Consumer<CanvasBlockComponent> onMovedOrResized;   // 이동·리사이즈 종료 → 저장
    private Consumer<CanvasBlockComponent> onDeleteRequest;    // Delete 키 / 메뉴 삭제

    /** 빈 곳 우클릭 — (화면 좌표, 그 지점의 world 좌표). */
    private BiConsumer<Point, Point2D> onEmptyContextMenu;
    /** 블록 우클릭 — (블록, 화면 좌표). */
    private BiConsumer<CanvasBlockComponent, Point> onBlockContextMenu;

    public InfiniteCanvasPanel() {
        setBackground(BG);
        installMouseHandlers();
        installKeyBindings();
    }

    public CanvasViewport getViewport()    { return viewport; }
    public GridRenderer  getGridRenderer() { return gridRenderer; }

    /** 화면 좌표(드롭 지점)를 격자 스냅된 world 좌표로 변환. (DnD 배치용) */
    public Point2D toSnappedWorld(Point screenPoint) {
        Point2D world = viewport.screenToWorld(screenPoint.x, screenPoint.y);
        return gridRenderer.snap(world.getX(), world.getY());
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

    private void installMouseHandlers() {
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                // 우클릭(또는 팝업 트리거)은 컨텍스트 메뉴 처리
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                    return;
                }
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
                if (e.isPopupTrigger()) {       // 일부 OS 는 release 에서 팝업 트리거
                    handlePopup(e);
                    return;
                }
                if ((mode == Mode.MOVE || mode == Mode.RESIZE) && active != null) {
                    if (mode == Mode.MOVE) {
                        Point2D snapped = gridRenderer.snap(active.getWorldX(), active.getWorldY());
                        active.setWorldPos(snapped.getX(), snapped.getY());
                    }
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

    /** 우클릭 위치가 블록 위면 블록 메뉴, 빈 곳이면 빈 곳 메뉴 요청. */
    private void handlePopup(MouseEvent e) {
        requestFocusInWindow(); // Delete 키가 먹도록 포커스 확보
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
