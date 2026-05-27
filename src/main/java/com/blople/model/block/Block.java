package com.blople.model.block;

import com.blople.model.enums.BlockType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * 모든 블록의 추상 베이스 클래스.
 * DB의 block 공통 테이블과 매핑된다.
 *
 * <p>새로운 블록 유형을 추가할 때는:
 * <ol>
 *   <li>{@link BlockType} 에 enum 상수를 추가하고</li>
 *   <li>본 클래스를 상속하는 새 클래스를 작성한 뒤</li>
 *   <li>대응되는 DAO/Mapper를 작성하면 된다.</li>
 * </ol>
 * (확장성 — NFR-024)
 */
public abstract class Block {

    public static final DateTimeFormatter TS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String id;
    private final BlockType blockType;
    private String name;
    private String color;
    private String memo;
    private String createdAt;
    private String updatedAt;

    protected Block(BlockType blockType, String name, String color) {
        this(UUID.randomUUID().toString(), blockType, name, color, null,
                nowTs(), nowTs());
    }

    protected Block(String id,
                    BlockType blockType,
                    String name,
                    String color,
                    String memo,
                    String createdAt,
                    String updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.blockType = Objects.requireNonNull(blockType, "blockType must not be null");
        this.name = name;
        this.color = color;
        this.memo = memo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private static String nowTs() {
        return LocalDateTime.now().format(TS_FORMATTER);
    }

    /**
     * 수정이 일어났음을 표시. 서비스 계층에서 호출.
     */
    public void touch() {
        this.updatedAt = nowTs();
    }


    /**
     * DB 매핑 등 "재구성" 목적으로 영속 상태를 덮어쓰기 위한 protected hook.
     * 일반 비즈니스 코드는 호출하지 말 것.
     */
    protected void restoreId(String id) {
        this.id = Objects.requireNonNull(id);
    }

    protected void restoreCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    protected void restoreUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }


    // ===== getter / setter =====
    public String getId() {
        return id;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        touch();
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
        touch();
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
        touch();
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Block b)) return false;
        return id.equals(b.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "%s{id=%s, name=%s}".formatted(blockType, id, name);
    }

}

