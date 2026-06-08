package com.blople.dnd;

import com.blople.view.calendar.WeekViewPanel;

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.Point;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;

/**
 * 캘린더(주간 뷰) 드롭 타겟 (FR-11).
 *
 * <p>{@link WeekViewPanel} 에 DropTarget 을 걸어, 미리블록이 드롭되면
 * 드롭 좌표의 시각(30분 스냅)과 블록 id 를 콜백으로 전달한다.
 *
 * <p>BLOCK_FLAVOR 가 아닌 드래그는 거부한다.
 */
public class CalendarDropTarget {

    private final WeekViewPanel weekViewPanel;

    /** (blockId, 드롭 시각) 처리 콜백 — Controller 가 주입. */
    private BiConsumer<String, LocalDateTime> onDrop;

    public CalendarDropTarget(WeekViewPanel weekViewPanel) {
        this.weekViewPanel = weekViewPanel;
        register();
    }

    public void setOnDrop(BiConsumer<String, LocalDateTime> cb) { this.onDrop = cb; }

    private void register() {
        new DropTarget(weekViewPanel, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                if (accepts(dtde)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    if (!dtde.isDataFlavorSupported(BlockTransferable.BLOCK_FLAVOR)) {
                        dtde.rejectDrop();
                        return;
                    }
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);

                    Object data = dtde.getTransferable()
                            .getTransferData(BlockTransferable.BLOCK_FLAVOR);
                    if (!(data instanceof BlockTransferable bt)) {
                        dtde.dropComplete(false);
                        return;
                    }

                    Point p = dtde.getLocation(); // 이미 weekViewPanel 기준 좌표
                    LocalDateTime when = weekViewPanel.datetimeAt(p.x, p.y);
                    if (when == null) {
                        dtde.dropComplete(false);
                        return;
                    }

                    if (onDrop != null) onDrop.accept(bt.getBlockId(), when);
                    dtde.dropComplete(true);
                } catch (Exception ex) {
                    dtde.dropComplete(false);
                }
            }
        }, true);
    }

    private boolean accepts(DropTargetDragEvent dtde) {
        return dtde.isDataFlavorSupported(BlockTransferable.BLOCK_FLAVOR);
    }
}
