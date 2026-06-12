package com.blople.dnd;

import com.blople.model.enums.BlockType;
import com.blople.view.canvas.InfiniteCanvasPanel;

import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.geom.Point2D;

/**
 * 무한 캔버스 드롭 타겟 (FR-05, FR-08, FR-11).
 *
 * <p>두 종류의 드래그를 받는다.
 * <ul>
 *   <li>팔레트(메모/링크): blockId 가 더미("palette"), {@link BlockType} 만 의미 있음 → 새 블록 생성</li>
 *   <li>트리의 파일 블록: 실제 blockId 운반 → 기존 블록을 캔버스에 배치</li>
 * </ul>
 * 드롭 시 (종류, blockId, 격자 스냅된 world 좌표)를 콜백으로 전달한다.
 */
public class CanvasDropTarget {

    /** 드롭 결과 콜백: (블록 종류, 운반된 blockId, 드롭 world 좌표). */
    @FunctionalInterface
    public interface DropHandler {
        void onDrop(BlockType type, String blockId, Point2D world);
    }

    private final InfiniteCanvasPanel canvasPanel;
    private DropHandler onDrop;

    public CanvasDropTarget(InfiniteCanvasPanel canvasPanel) {
        this.canvasPanel = canvasPanel;
        register();
    }

    public void setOnDrop(DropHandler cb) { this.onDrop = cb; }

    private void register() {
        new DropTarget(canvasPanel, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                if (dtde.isDataFlavorSupported(BlockTransferable.BLOCK_FLAVOR)) {
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

                    Point p = dtde.getLocation();
                    Point2D world = canvasPanel.toSnappedWorld(p);

                    if (onDrop != null) onDrop.onDrop(bt.getBlockType(), bt.getBlockId(), world);
                    dtde.dropComplete(true);
                } catch (Exception ex) {
                    dtde.dropComplete(false);
                }
            }
        }, true);
    }
}
