package com.blople.model.automation;

import com.blople.model.enums.BlockType;

import java.util.UUID;

/**
 * 자동화 규칙이 "열 블록" 한 건 (FR-12).
 * slot_open_blocks 테이블의 한 행에 대응한다.
 *
 * <p>실행 시 blockType 으로 파일/링크를 구분해 적절히 연다.
 * displayName 은 관리 UI 표시용 캐시(없어도 동작에는 지장 없음).
 */
public class OpenBlockRef {

    private final String id;
    private final String blockId;     // 열 대상 블록(파일/링크)의 id
    private BlockType blockType;      // FILE / LINK
    private String displayName;       // 표시용 (파일명 또는 링크 이름)

    public OpenBlockRef(String blockId, BlockType blockType, String displayName) {
        this(UUID.randomUUID().toString(), blockId, blockType, displayName);
    }

    public OpenBlockRef(String id, String blockId, BlockType blockType, String displayName) {
        this.id = id;
        this.blockId = blockId;
        this.blockType = blockType;
        this.displayName = displayName;
    }

    public String getId()                          { return id; }
    public String getBlockId()                     { return blockId; }
    public BlockType getBlockType()                { return blockType; }
    public void setBlockType(BlockType t)          { this.blockType = t; }
    public String getDisplayName()                 { return displayName; }
    public void setDisplayName(String name)        { this.displayName = name; }

    @Override
    public String toString() {
        return "OpenBlockRef{type=%s, name=%s}".formatted(blockType, displayName);
    }
}
