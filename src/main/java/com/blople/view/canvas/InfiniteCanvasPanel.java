package com.blople.view.canvas;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class InfiniteCanvasPanel extends JPanel {

    private static final Color BG        = new Color(0xFAFAFC);
    private static final Color EDGE       = new Color(0x9C6BFF);
    private static final Color EDGE_TEMP  = new Color(0x9C6BFF);
    private static final int    EDGE_HIT_TOL = 6;
    private static final int    ARROW_SIZE   = 9;

    private final CanvasViewport viewport = new CanvasViewport();
    private final GridRenderer gridRenderer = new GridRenderer();

    /**
     * 블록 목록. 그리기/히트 테스트 시 "정렬된 순서"를 따로 계산하여 사용한다.
     * 우선순위 규칙:
     *   - priority 가 작을수록(1 → 2 → 3) 위에 그려진다.
     *   - 같은 priority 내에서는 lastClickOrder 가 큰 쪽(=최근 클릭)이 위에 그려진다.
     */
    private final List<CanvasBlockComponent> blocks = new ArrayList<>();

    /** 같은 priority 그룹 안에서의 클릭 순서. 클수록 최근. */
    private final java.util.Map<CanvasBlockComponent, Long> lastClickOrder = new java.util.IdentityHashMap<>();
    private long clickSeq = 0;

    private enum Mode { NONE, PAN, MOVE, RESIZE, PORT_DRAG, UNPLUG }
    private Mode mode = Mode.NONE;
    private Point lastPoint;
    private CanvasBlockComponent active;
    private CanvasBlockComponent selected;

    private double dragOriginWorldX;
    private double dragOriginWorldY;

    private AutomationBlockComponent portSource;
    private Point portDragCurrent;

    private AutomationBlockComponent unplugSource;
    private CanvasBlockComponent     unplugTarget;
    private Point unplugCurrent;

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
    private BiConsumer<AutomationBlockComponent, CanvasBlockComponent> onDisconnectRequest;

    /** 우선순위 메뉴 (선택된 블록 위에 떠 있는 1/2/3 버튼). */
    private final PriorityMenu priorityMenu = new PriorityMenu();

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
        setLayout(null); // priorityMenu 를 절대 좌표로 배치하기 위해
        add(priorityMenu);
        priorityMenu.setVisible(false);

        installMouseHandlers();
        installKeyBindings();

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { updatePriorityMenuPosition(); }
        });
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

    public void forEachAutomation(Consumer<AutomationBlockComponent> action) {
        for (CanvasBlockComponent c : blocks) {
            if (c instanceof AutomationBlockComponent ac) action.accept(ac);
        }
    }

    // ===== 블록 목록 관리 =====
    public void setBlocks(List<CanvasBlockComponent> list) {
        blocks.clear();
        lastClickOrder.clear();
        if (list != null) blocks.addAll(list);
        selected = null;
        active = null;
        hidePriorityMenu();
        repaint();
    }

    public void addBlock(CanvasBlockComponent c) { blocks.add(c); repaint(); }

    public void removeBlock(CanvasBlockComponent c) {
        blocks.remove(c);
        lastClickOrder.remove(c);
        if (selected == c) { selected = null; hidePriorityMenu(); }
        if (active == c) active = null;
        repaint();
    }

    public void clearBlocks() {
        blocks.clear();
        lastClickOrder.clear();
        selected = null;
        active = null;
        hidePriorityMenu();
        repaint();
    }

    public List<CanvasBlockComponent> snapshotBlocks() {
        return new ArrayList<>(blocks);
    }

    public CanvasBlockComponent getSelected() { return selected; }

    private CanvasBlockComponent findByBlockId(String blockId) {
        for (CanvasBlockComponent c : blocks) {
            if (c.getBlockId().equals(blockId)) return c;
        }
        return null;
    }

    /**
     * 화면 표시 순서로 정렬된 사본을 반환.
     * 리스트의 뒤쪽이 "위에" 그려진다.
     */
    private List<CanvasBlockComponent> orderedForDisplay() {
        List<CanvasBlockComponent> sorted = new ArrayList<>(blocks);
        sorted.sort(displayComparator());
        return sorted;
    }

    private Comparator<CanvasBlockComponent> displayComparator() {
        // priority 가 큰 것(=3, 아래쪽)이 먼저 그려지고, 작은 것(=1, 위)이 나중에 그려져야 한다.
        // 같은 priority 면 lastClickOrder 가 작은 것이 먼저(아래), 큰 것이 나중(위).
        return (a, b) -> {
            int pa = a.getPriority();
            int pb = b.getPriority();
            if (pa != pb) return Integer.compare(pb, pa); // 큰 priority 먼저
            long la = lastClickOrder.getOrDefault(a, 0L);
            long lb = lastClickOrder.getOrDefault(b, 0L);
            return Long.compare(la, lb); // 작은 것 먼저
        };
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
    public void setOnDisconnectRequest(BiConsumer<AutomationBlockComponent, CanvasBlockComponent> cb) {
        this.onDisconnectRequest = cb;
    }

    private void installMouseHandlers() {
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) { handlePopup(e); return; }
                if (!SwingUtilities.isLeftMouseButton(e)) return;

                lastPoint = e.getPoint();

                AutomationBlockComponent outHit = outputPortAt(e.getX(), e.getY());
                if (outHit != null) {
                    portSource = outHit;
                    portDragCurrent = e.getPoint();
                    select(outHit);
                    mode = Mode.PORT_DRAG;
                    repaint();
                    return;
                }

                CanvasBlockComponent inHit = inputPortAt(e.getX(), e.getY());
                if (inHit != null) {
                    AutomationBlockComponent src = sourceConnectedTo(inHit);
                    if (src != null) {
                        unplugSource = src;
                        unplugTarget = inHit;
                        unplugCurrent = e.getPoint();
                        select(inHit);
                        mode = Mode.UNPLUG;
                        repaint();
                        return;
                    }
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
                if (mode == Mode.UNPLUG) {
                    unplugCurrent = e.getPoint();
                    repaint();
                    return;
                }

                int dx = e.getX() - lastPoint.x;
                int dy = e.getY() - lastPoint.y;

                switch (mode) {
                    case PAN -> {
                        viewport.panByScreen(dx, dy);
                        updatePriorityMenuPosition();
                    }
                    case MOVE -> {
                        if (active != null) {
                            double dwx = dx / viewport.getScale();
                            double dwy = dy / viewport.getScale();
                            active.setWorldPos(active.getWorldX() + dwx, active.getWorldY() + dwy);
                            updatePriorityMenuPosition();
                        }
                    }
                    case RESIZE -> {
                        if (active != null) {
                            double dww = dx / viewport.getScale();
                            double dwh = dy / viewport.getScale();
                            active.setWorldSize(active.getWorldW() + dww, active.getWorldH() + dwh);
                            updatePriorityMenuPosition();
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

                if (mode == Mode.UNPLUG && unplugSource != null && unplugTarget != null) {
                    CanvasBlockComponent dropTarget = topBlockExcept(e.getX(), e.getY(), unplugSource);
                    boolean reconnectable = (dropTarget != null) && dropTarget.hasInputPort();
                    if (reconnectable && dropTarget != unplugTarget) {
                        if (onDisconnectRequest != null) onDisconnectRequest.accept(unplugSource, unplugTarget);
                        if (onConnectRequest != null)    onConnectRequest.accept(unplugSource, dropTarget);
                    } else if (reconnectable && dropTarget == unplugTarget) {
                        // 변화 없음
                    } else {
                        if (onDisconnectRequest != null) onDisconnectRequest.accept(unplugSource, unplugTarget);
                    }
                    unplugSource = null;
                    unplugTarget = null;
                    unplugCurrent = null;
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
                        updatePriorityMenuPosition();
                        repaint();
                        return;
                    }
                    Point2D snapped = gridRenderer.snap(active.getWorldX(), active.getWorldY());
                    active.setWorldPos(snapped.getX(), snapped.getY());
                    if (onMovedOrResized != null) onMovedOrResized.accept(active);
                    updatePriorityMenuPosition();
                    repaint();

                } else if (mode == Mode.RESIZE && active != null) {
                    Point2D corner = gridRenderer.snap(
                            active.getWorldX() + active.getWorldW(),
                            active.getWorldY() + active.getWorldH());
                    double newW = corner.getX() - active.getWorldX();
                    double newH = corner.getY() - active.getWorldY();
                    active.setWorldSize(newW, newH);
                    if (onMovedOrResized != null) onMovedOrResized.accept(active);
                    updatePriorityMenuPosition();
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
                if (outputPortAt(e.getX(), e.getY()) != null
                        || inputPortAt(e.getX(), e.getY()) != null) {
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
            updatePriorityMenuPosition();
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

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "recenterCanvas");
        am.put("recenterCanvas", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                recenter();
            }
        });
    }

    private void select(CanvasBlockComponent c) {
        if (selected != null) selected.setSelected(false);
        selected = c;
        if (c != null) {
            c.setSelected(true);
            // 같은 priority 그룹 안에서 최근에 클릭한 쪽이 위에 오도록 순서 갱신
            lastClickOrder.put(c, ++clickSeq);
            showPriorityMenuFor(c);
        } else {
            hidePriorityMenu();
        }
        repaint();
    }

    // ===== 우선순위 메뉴 위치/표시 =====
    private void showPriorityMenuFor(CanvasBlockComponent c) {
        priorityMenu.bindTo(c);
        priorityMenu.setVisible(true);
        updatePriorityMenuPosition();
    }

    private void hidePriorityMenu() {
        priorityMenu.bindTo(null);
        priorityMenu.setVisible(false);
    }

    private void updatePriorityMenuPosition() {
        if (!priorityMenu.isVisible() || selected == null) return;

        Dimension size = priorityMenu.getPreferredSize();
        priorityMenu.setSize(size);

        Point tl = viewport.worldToScreen(selected.getWorldX(), selected.getWorldY());
        Point tr = viewport.worldToScreen(selected.getWorldX() + selected.getWorldW(),
                selected.getWorldY());

        // 블록과 겹치지 않도록 블록의 오른쪽 바깥, 상단에 정렬
        int gap = 6;
        int x = tr.x + gap;
        int y = tl.y;

        // 오른쪽 공간이 부족하면 왼쪽으로 보낸다
        if (x + size.width > getWidth() - 2) {
            x = tl.x - size.width - gap;
        }
        // 그래도 음수면(블록이 너무 좌측) 블록 위쪽에 띄운다
        if (x < 2) {
            x = tl.x;
            y = tl.y - size.height - gap;
            if (y < 2) {
                // 위쪽도 없으면 오른쪽 안쪽으로 클램프
                x = Math.max(2, Math.min(getWidth() - size.width - 2, tr.x + gap));
                y = Math.max(2, tl.y);
            }
        }
        // 하단/상단 클램프
        y = Math.max(2, Math.min(getHeight() - size.height - 2, y));

        priorityMenu.setLocation(x, y);
        priorityMenu.refreshSelection();
    }

    private CanvasBlockComponent topBlockAt(int sx, int sy) {
        List<CanvasBlockComponent> ordered = orderedForDisplay();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            if (ordered.get(i).contains(viewport, sx, sy)) return ordered.get(i);
        }
        return null;
    }

    private CanvasBlockComponent topBlockExcept(int sx, int sy, CanvasBlockComponent except) {
        List<CanvasBlockComponent> ordered = orderedForDisplay();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            CanvasBlockComponent c = ordered.get(i);
            if (c != except && c.contains(viewport, sx, sy)) return c;
        }
        return null;
    }

    private AutomationBlockComponent outputPortAt(int sx, int sy) {
        List<CanvasBlockComponent> ordered = orderedForDisplay();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            if (ordered.get(i) instanceof AutomationBlockComponent ac
                    && ac.inPort(viewport, sx, sy)) {
                return ac;
            }
        }
        return null;
    }

    private CanvasBlockComponent inputPortAt(int sx, int sy) {
        List<CanvasBlockComponent> ordered = orderedForDisplay();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            CanvasBlockComponent c = ordered.get(i);
            if (!c.hasInputPort()) continue;
            Point p = c.inputPortCenterScreen(viewport);
            int r = CanvasBlockComponent.IN_PORT_R + 4;
            int dx = sx - p.x, dy = sy - p.y;
            if (dx * dx + dy * dy <= r * r) return c;
        }
        return null;
    }

    private AutomationBlockComponent sourceConnectedTo(CanvasBlockComponent target) {
        AutomationBlockComponent found = null;
        for (CanvasBlockComponent c : blocks) {
            if (c instanceof AutomationBlockComponent ac
                    && ac.getConnectedBlockIds().contains(target.getBlockId())) {
                found = ac;
            }
        }
        return found;
    }

    private CanvasBlockComponent inputPortHostAt(int sx, int sy, CanvasBlockComponent except) {
        List<CanvasBlockComponent> ordered = orderedForDisplay();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            CanvasBlockComponent c = ordered.get(i);
            if (c != except && c.hasInputPort() && c.contains(viewport, sx, sy)) return c;
        }
        return null;
    }

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

    public void zoomIn()  { centerZoom(1.2); }
    public void zoomOut() { centerZoom(1 / 1.2); }

    public void resetView() {
        viewport.resetZoom();
        viewport.setOffset(0, 0);
        updatePriorityMenuPosition();
        repaint();
    }

    public void recenter() {
        if (blocks.isEmpty()) {
            resetView();
            return;
        }

        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (CanvasBlockComponent c : blocks) {
            double x1 = c.getWorldX();
            double y1 = c.getWorldY();
            double x2 = x1 + c.getWorldW();
            double y2 = y1 + c.getWorldH();
            if (x1 < minX) minX = x1;
            if (y1 < minY) minY = y1;
            if (x2 > maxX) maxX = x2;
            if (y2 > maxY) maxY = y2;
        }

        int viewW = Math.max(1, getWidth());
        int viewH = Math.max(1, getHeight());
        double contentW = Math.max(1, maxX - minX);
        double contentH = Math.max(1, maxY - minY);

        double scaleX = (viewW * 0.85) / contentW;
        double scaleY = (viewH * 0.85) / contentH;
        double scale = Math.min(scaleX, scaleY);
        viewport.setScale(scale);

        double cx = (minX + maxX) / 2.0;
        double cy = (minY + maxY) / 2.0;
        double newOffsetX = cx - (viewW / 2.0) / viewport.getScale();
        double newOffsetY = cy - (viewH / 2.0) / viewport.getScale();
        viewport.setOffset(newOffsetX, newOffsetY);

        updatePriorityMenuPosition();
        repaint();
    }

    private void centerZoom(double factor) {
        viewport.zoomAt(getWidth() / 2, getHeight() / 2, factor);
        updatePriorityMenuPosition();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        gridRenderer.paint(g2, viewport, getWidth(), getHeight());

        // 1) 블록 본체 — 우선순위(1>2>3) 순으로 정렬해서 그린다
        List<CanvasBlockComponent> ordered = orderedForDisplay();
        for (CanvasBlockComponent c : ordered) {
            c.paint(g2, viewport);
        }

        // 2) 노드 그래프 선 — 항상 블록 위에 그린다
        paintEdges(g2);

        // 3) 포트 표시 (드래그/언플러그 중일 때 입력 포트, 자동화 출력 포트)
        boolean dragging = (mode == Mode.PORT_DRAG && portSource != null);
        boolean unplugging = (mode == Mode.UNPLUG && unplugSource != null);

        if (dragging) {
            for (CanvasBlockComponent c : ordered) {
                if (c != portSource) c.paintInputPort(g2, viewport, true);
            }
        }
        if (unplugging) {
            for (CanvasBlockComponent c : ordered) {
                if (c.hasInputPort()) c.paintInputPort(g2, viewport, true);
            }
        }

        for (CanvasBlockComponent c : ordered) {
            if (c instanceof AutomationBlockComponent ac) ac.paintPort(g2, viewport, true);
        }

        // 4) 드래그 중인 임시 선
        if (dragging && portDragCurrent != null) {
            Point p1 = portSource.portCenterScreen(viewport);
            Point end = portDragCurrent;
            CanvasBlockComponent host = inputPortHostAt(portDragCurrent.x, portDragCurrent.y, portSource);
            if (host != null) end = host.inputPortCenterScreen(viewport);
            drawTempEdge(g2, p1.x, p1.y, end.x, end.y);
        }
        if (unplugging && unplugCurrent != null) {
            Point p1 = unplugSource.portCenterScreen(viewport);
            Point end = unplugCurrent;
            CanvasBlockComponent host = inputPortHostAt(unplugCurrent.x, unplugCurrent.y, unplugSource);
            if (host != null) end = host.inputPortCenterScreen(viewport);
            drawTempEdge(g2, p1.x, p1.y, end.x, end.y);
        }

        g2.setColor(new Color(0x999999));
        g2.drawString(String.format("zoom %.0f%%", viewport.getScale() * 100), 8, getHeight() - 8);
    }


    private void drawTempEdge(Graphics2D g2, int x1, int y1, int x2, int y2) {
        g2.setColor(EDGE_TEMP);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{6, 6}, 0));
        drawCurve(g2, x1, y1, x2, y2);
        g2.setStroke(new BasicStroke(2f));
        drawArrowHead(g2, x2, y2);
    }

    private void paintEdges(Graphics2D g2) {
        for (CanvasBlockComponent c : blocks) {
            if (!(c instanceof AutomationBlockComponent ac)) continue;
            Point p1 = ac.portCenterScreen(viewport);
            for (String id : ac.getConnectedBlockIds()) {
                CanvasBlockComponent t = findByBlockId(id);
                if (t == null) continue;
                if (mode == Mode.UNPLUG && ac == unplugSource && t == unplugTarget) continue;
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

    private void drawArrowHead(Graphics2D g2, int x2, int y2) {
        double angle = 0;
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

    // ============================================================
    //  우선순위 메뉴 컴포넌트 (선택된 블록 옆에 떠 있는 1/2/3 버튼)
    // ============================================================
    // ============================================================
//  우선순위 메뉴 컴포넌트 (선택된 블록 옆에 떠 있는 1/2/3 버튼)
// ============================================================
    private class PriorityMenu extends JPanel {

        private final PriorityButton btn1 = new PriorityButton(1);
        private final PriorityButton btn2 = new PriorityButton(2);
        private final PriorityButton btn3 = new PriorityButton(3);

        private CanvasBlockComponent target;

        PriorityMenu() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
            setBackground(new Color(0xFFFFFF));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xCFD6E4), 1),
                    BorderFactory.createEmptyBorder(2, 4, 2, 4)));
            add(btn1);
            add(btn2);
            add(btn3);
            setOpaque(true);
        }

        void bindTo(CanvasBlockComponent c) {
            this.target = c;
        }

        void refreshSelection() {
            int p = (target == null) ? CanvasBlockComponent.PRIORITY_DEFAULT : target.getPriority();
            btn1.setActive(p == 1);
            btn2.setActive(p == 2);
            btn3.setActive(p == 3);
        }

        private void applyPriority(int p) {
            if (target == null) return;
            target.setPriority(p);
            lastClickOrder.put(target, ++clickSeq);
            if (onMovedOrResized != null) onMovedOrResized.accept(target);
            refreshSelection();
            InfiniteCanvasPanel.this.repaint();
        }

        /**
         * 직접 그리는 우선순위 버튼.
         * 시스템 L&F에 영향을 받지 않도록 paintComponent 에서 배경/글자를 직접 그린다.
         * (이전에 setBackground 만으로는 흰색 위에 흰 글자가 되어 안 보이는 문제 해결)
         */
        private class PriorityButton extends JButton {
            private static final Color ACTIVE_BG   = new Color(0x4D96FF);
            private static final Color ACTIVE_FG   = Color.WHITE;
            private static final Color INACTIVE_BG = new Color(0xF3F5FA);
            private static final Color INACTIVE_FG = new Color(0x334155);
            private static final Color BORDER_COL  = new Color(0xCFD6E4);
            private static final Color BORDER_ACT  = new Color(0x3A78D6);

            private final int value;
            private boolean active;

            PriorityButton(int value) {
                super(String.valueOf(value));
                this.value = value;
                setFocusable(false);
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);   // 직접 그리므로 L&F 그림 끔
                setOpaque(false);
                setRolloverEnabled(false);
                setMargin(new Insets(0, 0, 0, 0));
                setPreferredSize(new Dimension(26, 22));
                setFont(new Font("SansSerif", Font.BOLD, 11));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setToolTipText("우선순위 " + value
                        + (value == 1 ? " (가장 위)" : value == 3 ? " (가장 아래)" : ""));
                addActionListener(e -> applyPriority(value));
            }

            void setActive(boolean active) {
                this.active = active;
                repaint();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int arc = 8;

                g2.setColor(active ? ACTIVE_BG : INACTIVE_BG);
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                g2.setColor(active ? BORDER_ACT : BORDER_COL);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                String text = String.valueOf(value);
                g2.setColor(active ? ACTIVE_FG : INACTIVE_FG);
                g2.setFont(getFont());
                int tw = g2.getFontMetrics().stringWidth(text);
                int th = g2.getFontMetrics().getAscent();
                g2.drawString(text, (w - tw) / 2, (h + th) / 2 - 2);

                g2.dispose();
            }
        }
    }

}
