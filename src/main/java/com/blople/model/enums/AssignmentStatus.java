package com.blople.model.enums;

/**
 * 과제 진행 상태.
 */
public enum AssignmentStatus {
    NOT_STARTED("미시작"),
    IN_PROGRESS("진행중"),
    DONE("완료");

    private final String displayName;

    AssignmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
