package com.blople.model.canvas;

import java.util.UUID;

/**
 * 캔버스 위 블록의 배치 상태(위치/크기/zOrder)를 표현한다.
 * canvas_state 테이블과 매핑된다. (FR-08)
 *
 *  - blockId  : 어떤 블록(text_block 등)의 배치인지
 *  - subjectId: 어느 과목 캔버스에 속하는지
 *  - posX/posY: 월드 좌표
 *  - width/height: 월드 크기
 *  - zOrder   : 겹침 순서(클수록 위)
 */
public class CanvasState {
    private String id;
    private String subjectId;
    private String blockId;
    private double posX;
    private double posY;
    private double width;
    private double height;
    private int zOrder;

    /** 신규 생성 – id 자동 발급, zOrder 기본 0 */
    public CanvasState(String subjectId, String blockId,
                       double posX, double posY, double width, double height) {
        this.id = UUID.randomUUID().toString();
        this.subjectId = subjectId;
        this.blockId = blockId;
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
        this.zOrder = 0;
    }

    /** DAO 복원용 – 모든 필드를 DB에서 받아 재구성 */
    public static CanvasState reconstruct(String id, String subjectId, String blockId,
                                          double posX, double posY,
                                          double width, double height, int zOrder) {
        CanvasState s = new CanvasState(subjectId, blockId, posX, posY, width, height);
        s.id = id;
        s.zOrder = zOrder;
        return s;
    }

    // ----- getters / setters -----
    public String getId() { return id; }
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    public String getBlockId() { return blockId; }
    public void setBlockId(String blockId) { this.blockId = blockId; }

    public double getPosX() { return posX; }
    public void setPosX(double posX) { this.posX = posX; }
    public double getPosY() { return posY; }
    public void setPosY(double posY) { this.posY = posY; }

    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    public int getZOrder() { return zOrder; }
    public void setZOrder(int zOrder) { this.zOrder = zOrder; }

    @Override
    public String toString() {
        return "CanvasState{block=%s, x=%.1f, y=%.1f, w=%.1f, h=%.1f, z=%d}"
                .formatted(blockId, posX, posY, width, height, zOrder);
    }
}
