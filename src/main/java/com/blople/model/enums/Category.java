package com.blople.model.enums;

/**
 * 미리블록 카테고리.
 */
public enum Category {
    STUDY("학업"),
    EXERCISE("운동"),
    PART_TIME("아르바이트"),
    CLUB("동아리"),
    PERSONAL("개인"),
    OTHER("기타");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
