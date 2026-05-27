package com.blople.common;

import java.awt.Color;

/**
 * 프로젝트 전역에서 사용하는 상수.
 * 색상, 크기, 경로, 자동 저장 주기 등을 모아둔다.
 */
public final class Constants {

    private Constants() { /* no instance */ }

    // ===== 애플리케이션 =====
    public static final String APP_NAME    = "Blople";
    public static final String APP_VERSION = "0.1.0";

    // ===== 윈도우 기본 크기 =====
    public static final int MAIN_WINDOW_WIDTH  = 1440;
    public static final int MAIN_WINDOW_HEIGHT = 900;
    public static final int MIN_WINDOW_WIDTH   = 1280;
    public static final int MIN_WINDOW_HEIGHT  = 720;

    // ===== 데이터베이스 =====
    public static final String DB_DIRECTORY = "data";
    public static final String DB_FILE_NAME = "blople.db";
    public static final String DB_URL       = "jdbc:sqlite:" + DB_DIRECTORY + "/" + DB_FILE_NAME;

    // ===== 자동 저장 =====
    public static final int AUTO_SAVE_INTERVAL_MS = 3_000;  // 3초
    public static final int UNDO_HISTORY_LIMIT    = 20;

    // ===== 캘린더 =====
    public static final int CALENDAR_TIME_SLOT_MINUTES = 30;
    public static final int CALENDAR_START_HOUR        = 8;
    public static final int CALENDAR_END_HOUR          = 23;

    // ===== 캔버스 =====
    public static final int    CANVAS_GRID_SIZE     = 20;
    public static final double CANVAS_MIN_ZOOM      = 0.25;
    public static final double CANVAS_MAX_ZOOM      = 3.0;
    public static final double CANVAS_DEFAULT_ZOOM  = 1.0;

    // ===== 블록 기본 색상 팔레트 =====
    public static final Color[] BLOCK_COLOR_PALETTE = {
            new Color(0xFF6B6B), // red
            new Color(0xFFB86B), // orange
            new Color(0xFFD93D), // yellow
            new Color(0x6BCB77), // green
            new Color(0x4D96FF), // blue
            new Color(0x9C6BFF), // purple
            new Color(0xFF6BC1), // pink
            new Color(0x6BB5FF)  // sky
    };

    public static final String DEFAULT_BLOCK_COLOR_HEX     = "#4D96FF";
    public static final String DEFAULT_TEXT_BLOCK_BG_HEX   = "#FFFF99";

    // ===== 과제 마감일 임박 기준 (일) =====
    public static final int DEADLINE_APPROACHING_DAYS = 3;

    // ===== 지원 파일 형식 =====
    public static final String[] SUPPORTED_FILE_EXTENSIONS = {
            "pdf", "hwp", "docx", "pptx", "xlsx",
            "png", "jpg", "jpeg", "gif",
            "txt", "md"
    };
}
