package com.blople.view.canvas;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
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
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 무한 캔버스 패널 (FR-08, FR-12) — 노드 그래프 연결 지원.
 *
 * <p>실행 블록 출력 포트(우측 ●)를 드래그하는 동안에만 포트가 표시되며,
 * 연결 가능한 블록(파일/링크)의 입력 포트(좌측 ●)가 함께 나타난다.
 * 대상 블록 위에서 놓으면 연결, "▶ 실행" 클릭 시 실행, 연결선 우클릭 시 해제.
 */
public class InfiniteCanvasPanel extends JPanel {

    private static final Color BG        = new Color(0xFAFAFC);
    private static final Color EDGE       = new Color(0x9C6BFF);
    private static final Color EDGE_TEMP  = new Color(0x9C6BFF);
    private static final int    EDGE_HIT_TOL = 6;
    private static final int    ARROW_SIZE   = 9;
    private static final int    PORT_HINT_PX = 8; // 우측 가장자리 포트 힌트 영역

    private final CanvasViewport viewport = new CanvasViewport();
    private final GridRenderer gridRenderer = new GridRenderer();
    private final List<CanvasBlockComponent> blocks = new ArrayList<>();

    private enum Mode { NONE, PAN, MOVE, RESIZE, PORT_DRAG }
    private Mode mode = Mode.NONE;
    private Point lastPoint;
    private CanvasBlockComponent active;
    private CanvasBlockComponent selected;

    private double dragOriginWorldX;
    private double dragOriginWorldY;

    private AutomationBlockComponent portSource;
    private Point portDragCurrent;

    private Consumer<Point2D> onCreateRequest;
    private Consumer<CanvasBlockComponent> onEditRequest;
    private Consumer<CanvasBlockComponent> onMovedOrResized;
    private Consumer<CanvasBlockComponent> onDeleteRequest;
    private BiConsumer<Point, Point2D> onEmptyContextMenu;
    private BiConsumer<CanvasBlockComponent, Point> onBlockContextMenu;
    private DropOnBlockHandler onBlockDroppedOnBlock;
    private Consumer<AutomationBlockComponent> onRunRequest;
    private BiConsumer<AutomationBlockComponent, CanvasBlockComponent> onConnectRequest;
    private EdgeContextHandler onEdgeContextMenu;

    @FunctionalInterface
    public interface DropOnBlockHandler {
        void onDrop(CanvasBlockComponent dragged, CanvasBlockComponent target,
                    double originWorldX, double originWorldY);
    }

    @FunctionalInterface
    public interface EdgeContextHandler {
        void onEdge(AutomationBlockComponent source, CanvasBlockComponent target, Point screen);
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

    public CanvasBlockComponent blockAt(int screenX, int screenY) {
        return topBlockAt(screenX, screenY);
    }

    /** 모든 자동화(실행) 블록에 대해 동작 수행 (컨트롤러의 일괄 갱신용). */
    public void forEachAutomation(Consumer<AutomationBlockComponent> action) {
        for (CanvasBlockComponent c : blocks) {
            if (c instanceof AutomationBlockComponent ac) action.accept(ac);
        }
    }

    // ===== 블록 목록 관리 =====
    public void setBlocks(List<CanvasBlockComponent> list) {
        blocks.clear();
        if (list != null) blocks.addAll(list);
        selected = null;
        active = null;
        repaint();
    }

    public void addBlock(CanvasBlockComponent c) { blocks.add(c); repaint(); }

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

    private CanvasBlockComponent findByBlockId(String blockId) {
        for (CanvasBlockComponent c : blocks) {
            if (c.getBlockId().equals(blockId)) return c;
        }
        return null;
    }

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
    public void setOnRunRequest(Consumer<AutomationBlockComponent> cb) { this.onRunRequest = cb; }
    public void setOnConnectRequest(BiConsumer<AutomationBlockComponent, CanvasBlockComponent> cb) {
        this.onConnectRequest = cb;
    }
    public void setOnEdgeContextMenu(EdgeContextHandler cb) { this.onEdgeContextMenu = cb; }

    private void installMouseHandlers() {
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) { handlePopup(e); return; }
                if (!SwingUtilities.isLeftMouseButton(e)) return;

                lastPoint = e.getPoint();

                // 1) 실행 블록 포트 영역(우측 가장자리)에서 드래그 시작?
                AutomationBlockComponent autoHit = automationPortStartAt(e.getX(), e.getY());
                if (autoHit != null) {
                    portSource = autoHit;
                    portDragCurrent = e.getPoint();
                    select(autoHit);
                    mode = Mode.PORT_DRAG;
                    repaint();
                    return;
                }

                CanvasBlockComponent hit = topBlockAt(e.getX(), e.getY());

                if (hit instanceof AutomationBlockComponent ac
                        && ac.inRunButton(viewport, e.getX(), e.getY())) {
                    active = ac;
                    select(ac);
                    mode = Mode.NONE;
                    return;
                }

                if (hit != null && hit.inResizeHandle(viewport, e.getX(), e.getY())) {
                    active = hit;
                    select(hit);
                    mode = Mode.RESIZE;
                } else if (hit != null) {
                    active = hit;
                    select(hit);
                    mode = Mode.MOVE;
                    dragOriginWorldX = hit.getWorldX();
                    dragOriginWorldY = hit.getWorldY();
                } else {
                    select(null);
                    mode = Mode.PAN;
                }
            }

            @Override public void mouseDragged(MouseEvent e) {
                if (lastPoint == null) return;

                if (mode == Mode.PORT_DRAG) {
                    portDragCurrent = e.getPoint();
                    repaint();
                    return;
                }

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

                if (mode == Mode.PORT_DRAG && portSource != null) {
                    CanvasBlockComponent target = topBlockExcept(e.getX(), e.getY(), portSource);
                    boolean connectable = (target != null) && target.hasInputPort();
                    if (connectable && onConnectRequest != null) {
                        onConnectRequest.accept(portSource, target);
                    }
                    portSource = null;
                    portDragCurrent = null;
                    mode = Mode.NONE;
                    lastPoint = null;
                    repaint();
                    return;
                }

                if (mode == Mode.MOVE && active != null) {
                    CanvasBlockComponent target = topBlockExcept(e.getX(), e.getY(), active);
                    if (target != null && onBlockDroppedOnBlock != null) {
                        onBlockDroppedOnBlock.onDrop(active, target, dragOriginWorldX, dragOriginWorldY);
                        mode = Mode.NONE;
                        active = null;
                        lastPoint = null;
                        repaint();
                        return;
                    }
                    Point2D snapped = gridRenderer.snap(active.getWorldX(), active.getWorldY());
                    active.setWorldPos(snapped.getX(), snapped.getY());
                    if (onMovedOrResized != null) onMovedOrResized.accept(active);
                    repaint();

                } else if (mode == Mode.RESIZE && active != null) {
                    // (7) 리사이즈 종료 시 크기도 격자에 스냅
                    Point2D corner = gridRenderer.snap(
                            active.getWorldX() + active.getWorldW(),
                            active.getWorldY() + active.getWorldH());
                    double newW = corner.getX() - active.getWorldX();
                    double newH = corner.getY() - active.getWorldY();
                    active.setWorldSize(newW, newH);
                    if (onMovedOrResized != null) onMovedOrResized.accept(active);
                    repaint();
                }

                mode = Mode.NONE;
                active = null;
                lastPoint = null;
            }

            @Override public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;

                CanvasBlockComponent hit = topBlockAt(e.getX(), e.getY());
                if (e.getClickCount() == 1 && hit instanceof AutomationBlockComponent ac
                        && ac.inRunButton(viewport, e.getX(), e.getY())) {
                    if (onRunRequest != null) onRunRequest.accept(ac);
                    return;
                }

                if (e.getClickCount() == 2) {
                    if (hit instanceof AutomationBlockComponent ac2
                            && ac2.inRunButton(viewport, e.getX(), e.getY())) {
                        if (onRunRequest != null) onRunRequest.accept(ac2);
                        return;
                    }
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
                // 실행 블록 우측 가장자리(포트 위치) 근처면 연결 힌트 커서
                if (automationPortStartAt(e.getX(), e.getY()) != null) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    return;
                }
                CanvasBlockComponent hit = topBlockAt(e.getX(), e.getY());
                if (hit instanceof AutomationBlockComponent ac
                        && ac.inRunButton(viewport, e.getX(), e.getY())) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else if (hit != null && hit.inResizeHandle(viewport, e.getX(), e.getY())) {
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

        EdgeHit eh = edgeAt(e.getX(), e.getY());
        if (eh != null && onEdgeContextMenu != null) {
            onEdgeContextMenu.onEdge(eh.source, eh.target, screen);
            return;
        }

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

    private CanvasBlockComponent topBlockAt(int sx, int sy) {
        for (int i = blocks.size() - 1; i >= 0; i--) {
            if (blocks.get(i).contains(viewport, sx, sy)) return blocks.get(i);
        }
        return null;
    }

    private CanvasBlockComponent topBlockExcept(int sx, int sy, CanvasBlockComponent except) {
        for (int i = blocks.size() - 1; i >= 0; i--) {
            CanvasBlockComponent c = blocks.get(i);
            if (c != except && c.contains(viewport, sx, sy)) return c;
        }
        return null;
    }

    /**
     * 포트 드래그를 시작할 수 있는 실행 블록 찾기.
     * 포트가 평소 안 보이므로, 블록 우측 가장자리 ±PORT_HINT_PX 영역을 포트로 간주한다.
     */
    private AutomationBlockComponent automationPortStartAt(int sx, int sy) {
        for (int i = blocks.size() - 1; i >= 0; i--) {
            if (!(blocks.get(i) instanceof AutomationBlockComponent ac)) continue;
            Point c = ac.portCenterScreen(viewport);
            if (Math.abs(sx - c.x) <= PORT_HINT_PX && Math.abs(sy - c.y) <= PORT_HINT_PX + 6) {
                return ac;
            }
        }
        return null;
    }

    // ===== 엣지 히트 =====
    private static final class EdgeHit {
        final AutomationBlockComponent source;
        final CanvasBlockComponent target;
        EdgeHit(AutomationBlockComponent s, CanvasBlockComponent t) { source = s; target = t; }
    }

    private EdgeHit edgeAt(int sx, int sy) {
        for (CanvasBlockComponent c : blocks) {
            if (!(c instanceof AutomationBlockComponent ac)) continue;
            Point p1 = ac.portCenterScreen(viewport);
            for (String id : ac.getConnectedBlockIds()) {
                CanvasBlockComponent t = findByBlockId(id);
                if (t == null) continue;
                Point p2 = t.inputPortCenterScreen(viewport);
                if (distanceToSegment(sx, sy, p1.x, p1.y, p2.x, p2.y) <= EDGE_HIT_TOL) {
                    return new EdgeHit(ac, t);
                }
            }
        }
        return null;
    }

    private static double distanceToSegment(int px, int py, int x1, int y1, int x2, int y2) {
        double dx = x2 - x1, dy = y2 - y1;
        double len2 = dx * dx + dy * dy;
        if (len2 == 0) return Math.hypot(px - x1, py - y1);
        double t = ((px - x1) * dx + (py - y1) * dy) / len2;
        t = Math.max(0, Math.min(1, t));
        double cx = x1 + t * dx, cy = y1 + t * dy;
        return Math.hypot(px - cx, py - cy);
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

        // 1) 엣지(연결선 + 화살표)
        paintEdges(g2);

        // 2) 블록
        for (CanvasBlockComponent c : blocks) {
            c.paint(g2, viewport);
        }

        // 3) 포트 드래그 중에만: 연결 가능한 블록 입력 포트 + 출발 출력 포트 표시
        boolean dragging = (mode == Mode.PORT_DRAG && portSource != null);
        if (dragging) {
            for (CanvasBlockComponent c : blocks) {
                if (c != portSource) c.paintInputPort(g2, viewport, true); // (가): 대상 입력 포트만
            }
            portSource.paintPort(g2, viewport, true); // 출발 출력 포트
        }

        // 4) 포트 드래그 임시 선
        if (dragging && portDragCurrent != null) {
            Point p1 = portSource.portCenterScreen(viewport);
            Point end = portDragCurrent;
            CanvasBlockComponent host =
                    inputPortHostAt(portDragCurrent.x, portDragCurrent.y, portSource);
            if (host != null) end = host.inputPortCenterScreen(viewport);

            g2.setColor(EDGE_TEMP);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{6, 6}, 0));
            drawCurve(g2, p1.x, p1.y, end.x, end.y);
            g2.setStroke(new BasicStroke(2f));
            drawArrowHead(g2, end.x, end.y);
        }

        g2.setColor(new Color(0x999999));
        g2.drawString(String.format("zoom %.0f%%", viewport.getScale() * 100), 8, getHeight() - 8);
    }

    private CanvasBlockComponent inputPortHostAt(int sx, int sy, CanvasBlockComponent except) {
        for (int i = blocks.size() - 1; i >= 0; i--) {
            CanvasBlockComponent c = blocks.get(i);
            if (c != except && c.hasInputPort() && c.contains(viewport, sx, sy)) return c;
        }
        return null;
    }

    private void paintEdges(Graphics2D g2) {
        for (CanvasBlockComponent c : blocks) {
            if (!(c instanceof AutomationBlockComponent ac)) continue;
            Point p1 = ac.portCenterScreen(viewport);
            for (String id : ac.getConnectedBlockIds()) {
                CanvasBlockComponent t = findByBlockId(id);
                if (t == null) continue;
                Point p2 = t.inputPortCenterScreen(viewport);
                g2.setColor(EDGE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawCurve(g2, p1.x, p1.y, p2.x, p2.y);
                drawArrowHead(g2, p2.x, p2.y);
            }
        }
    }

    private void drawCurve(Graphics2D g2, int x1, int y1, int x2, int y2) {
        int dx = Math.max(40, Math.abs(x2 - x1) / 2);
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x1, y1);
        path.curveTo(x1 + dx, y1, x2 - dx, y2, x2, y2);
        g2.draw(path);
    }

    /** 입력 포트로 들어오는 방향(좌→우)으로 화살표 머리를 그린다. */
    private void drawArrowHead(Graphics2D g2, int x2, int y2) {
        double angle = 0; // 입력 포트는 좌측 → 화살표는 오른쪽(블록 안쪽)을 향함
        double a1 = angle + Math.toRadians(150);
        double a2 = angle - Math.toRadians(150);
        int xA = (int) Math.round(x2 + ARROW_SIZE * Math.cos(a1));
        int yA = (int) Math.round(y2 + ARROW_SIZE * Math.sin(a1));
        int xB = (int) Math.round(x2 + ARROW_SIZE * Math.cos(a2));
        int yB = (int) Math.round(y2 + ARROW_SIZE * Math.sin(a2));
        Path2D.Double head = new Path2D.Double();
        head.moveTo(x2, y2);
        head.lineTo(xA, yA);
        head.lineTo(xB, yB);
        head.closePath();
        g2.fill(head);
    }
}
