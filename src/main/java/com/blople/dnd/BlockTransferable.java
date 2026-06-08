package com.blople.dnd;

import com.blople.model.enums.BlockType;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * 드래그 데이터 운반 객체 (FR-11).
 *
 * <p>블록 id 와 유형을 담아 DragSource → DropTarget 으로 전달한다.
 * 식별을 위해 JVM 로컬 객체 플레이버({@link #BLOCK_FLAVOR})를 사용한다.
 */
public class BlockTransferable implements Transferable {

    /** 본 앱 내부에서만 통용되는 블록 데이터 플레이버. */
    public static final DataFlavor BLOCK_FLAVOR =
            new DataFlavor(BlockTransferable.class, "Blople Block");

    private static final DataFlavor[] FLAVORS = { BLOCK_FLAVOR };

    private final String blockId;
    private final BlockType blockType;

    public BlockTransferable(String blockId, BlockType blockType) {
        this.blockId = blockId;
        this.blockType = blockType;
    }

    public String getBlockId()      { return blockId; }
    public BlockType getBlockType() { return blockType; }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return FLAVORS.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return BLOCK_FLAVOR.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return this; // 운반 객체 자신을 넘김 (id/type 를 꺼내 쓴다)
    }
}
