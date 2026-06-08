package com.blople.dnd;

import com.blople.model.enums.BlockType;

import javax.swing.JComponent;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.util.function.Supplier;

/**
 * 드래그 소스 핸들러 (FR-11).
 *
 * <p>컴포넌트에 {@link DragGestureRecognizer} 를 등록해 드래그가 시작되면
 * 해당 시점의 블록 정보를 {@link BlockTransferable} 로 만들어 내보낸다.
 *
 * <p>블록 정보는 Supplier 로 받아, 드래그 시작 순간의 최신 값을 사용한다.
 */
public class BlockDragSource {

    private final DragSource dragSource = new DragSource();

    /**
     * @param component 드래그 시작 대상 컴포넌트 (예: 미리블록 카드)
     * @param idSupplier 드래그 시작 시 운반할 블록 id 공급자
     * @param blockType  운반할 블록 유형
     */
    public BlockDragSource(JComponent component,
                           Supplier<String> idSupplier,
                           BlockType blockType) {
        DragGestureListener listener = (DragGestureEvent dge) -> {
            String blockId = idSupplier.get();
            if (blockId == null) return;
            BlockTransferable transferable = new BlockTransferable(blockId, blockType);
            try {
                dge.startDrag(java.awt.dnd.DragSource.DefaultCopyDrop, transferable);
            } catch (Exception ignore) {
                // 드래그 시작 실패 시 무시 (예: 이미 드래그 중)
            }
        };

        dragSource.createDefaultDragGestureRecognizer(
                component, DnDConstants.ACTION_COPY, listener);
    }
}
