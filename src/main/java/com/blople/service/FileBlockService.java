package com.blople.service;

import com.blople.common.Constants;
import com.blople.common.EventBus;
import com.blople.database.dao.FileBlockDAO;
import com.blople.model.block.FileBlock;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 파일 블록 서비스 (FR-05).
 *
 * <p>책임:
 * <ul>
 *   <li>로컬 파일 → 파일 블록 등록 (확장자 검증)</li>
 *   <li>파일 열기 (시스템 기본 프로그램)</li>
 *   <li>경로 유효성 확인</li>
 * </ul>
 */
public class FileBlockService {

    private final FileBlockDAO fileBlockDAO;
    private final EventBus eventBus = EventBus.getInstance();

    public FileBlockService(FileBlockDAO fileBlockDAO) {
        this.fileBlockDAO = fileBlockDAO;
    }

    // ===== 조회 =====
    public List<FileBlock> getBySubject(String subjectId) {
        return fileBlockDAO.findBySubject(subjectId);
    }

    public Optional<FileBlock> findById(String blockId) {
        return fileBlockDAO.findById(blockId);
    }

    // ===== 등록 =====
    public FileBlock createFromFile(String subjectId, String color, File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("파일을 찾을 수 없습니다.");
        }
        String fileName = file.getName();
        String ext = extensionOf(fileName);
        if (!isSupported(ext)) {
            throw new IllegalArgumentException(
                    "지원하지 않는 파일 형식입니다: " + (ext.isEmpty() ? "(확장자 없음)" : ext));
        }

        FileBlock f = new FileBlock(
                subjectId,
                fileName,                 // 블록 이름 = 파일명
                color,
                file.getAbsolutePath(),
                fileName,
                ext,
                file.length());
        fileBlockDAO.insert(f);
        eventBus.publish(EventBus.SUBJECT_CHANGED, f);
        return f;
    }

    // ===== 열기 =====
    public void openFile(String blockId) throws IOException {
        FileBlock f = fileBlockDAO.findById(blockId).orElseThrow(
                () -> new IllegalArgumentException("파일 블록을 찾을 수 없음: " + blockId));
        f.openFile(); // FileBlock 모델이 존재성·Desktop 지원 검증 후 연다
    }

    public boolean isFileValid(String blockId) {
        return fileBlockDAO.findById(blockId).map(FileBlock::isFileValid).orElse(false);
    }

    // ===== 삭제 =====
    public void delete(String blockId) {
        boolean ok = fileBlockDAO.deleteById(blockId);
        if (ok) eventBus.publish(EventBus.SUBJECT_CHANGED, blockId);
    }

    // ===== 헬퍼 =====
    public static String extensionOf(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).toLowerCase();
    }

    public static boolean isSupported(String ext) {
        return Arrays.asList(Constants.SUPPORTED_FILE_EXTENSIONS).contains(ext.toLowerCase());
    }

    public List<String> getSupportedExtensions() {
        return Arrays.asList(Constants.SUPPORTED_FILE_EXTENSIONS);
    }
}
