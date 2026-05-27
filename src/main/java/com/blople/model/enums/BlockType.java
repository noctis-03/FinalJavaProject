package com.blople.model.enums;

/**
 * 블록 유형.
 * DB의 block.block_type 컬럼과 1:1로 매핑된다.
 */
public enum BlockType {
    SCHEDULE,    // 일정 블록 (FR-01)
    PRESET,      // 미리블록 (FR-02)
    ASSIGNMENT,  // 과제블록 (FR-04)
    FILE,        // 파일블록 (FR-05)
    TEXT,        // 텍스트블록 (FR-06)
    LINK,        // 링크블록 (FR-07)
    AUTOMATION   // 자동화블록 (FR-13)
}
