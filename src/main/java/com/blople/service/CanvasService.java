package com.blople.service;

import com.blople.common.EventBus;
import com.blople.database.dao.CanvasStateDAO;
import com.blople.database.dao.FileBlockDAO;
import com.blople.database.dao.LinkBlockDAO;
import com.blople.database.dao.TextBlockDAO;
import com.blople.model.block.FileBlock;
import com.blople.model.block.LinkBlock;
import com.blople.model.block.TextBlock;
import com.blople.model.canvas.CanvasState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 무한 캔버스 서비스 (FR-05, FR-06, FR-07, FR-08).
 *
 * <p>텍스트/링크 블록은 캔버스에서 생성되며, 생성 시 canvas_state 도 함께 만든다.
 * 파일 블록은 좌측 트리에 존재하는 기존 블록을 드래그로 "배치" 하는 방식이라,
 * canvas_state 가 있는 파일만 캔버스에 표시한다(트리에는 항상 남는다).
 *
 * <p>블록 이동/리사이즈 시 canvas_state 를 갱신한다(NFR-018).
 */
public class CanvasService {

    public static final double DEFAULT_W = 160;
    public static final double DEFAULT_H = 110;

    public static final double LINK_DEFAULT_W = 180;
    public static final double LINK_DEFAULT_H = 64;

    public static final double FILE_DEFAULT_W = 180;
    public static final double FILE_DEFAULT_H = 72;

    private final TextBlockDAO   textBlockDAO;
    private final LinkBlockDAO   linkBlockDAO;
    private final FileBlockDAO   fileBlockDAO;
    private final CanvasStateDAO canvasStateDAO;
    private final EventBus eventBus = EventBus.getInstance();

    public CanvasService(TextBlockDAO textBlockDAO,
                         LinkBlockDAO linkBlockDAO,
                         FileBlockDAO fileBlockDAO,
                         CanvasStateDAO canvasStateDAO) {
        this.textBlockDAO = textBlockDAO;
        this.linkBlockDAO = linkBlockDAO;
        this.fileBlockDAO = fileBlockDAO;
        this.canvasStateDAO = canvasStateDAO;
    }

    // ===== 텍스트 블록 =====
    public List<Placed<TextBlock>> loadTextBlocks(String subjectId) {
        List<Placed<TextBlock>> result = new ArrayList<>();
        for (TextBlock t : textBlockDAO.findBySubject(subjectId)) {
            CanvasState st = canvasStateDAO.findByBlock(t.getId())
                    .orElseGet(() -> new CanvasState(subjectId, t.getId(), 40, 40, DEFAULT_W, DEFAULT_H));
            result.add(new Placed<>(t, st));
        }
        return result;
    }

    public Placed<TextBlock> createTextBlock(String subjectId, double worldX, double worldY) {
        TextBlock t = new TextBlock(subjectId, "메모", "");
        textBlockDAO.insert(t);

        CanvasState st = new CanvasState(subjectId, t.getId(), worldX, worldY, DEFAULT_W, DEFAULT_H);
        canvasStateDAO.insert(st);

        eventBus.publish(EventBus.SUBJECT_CHANGED, "canvas-text-created");
        return new Placed<>(t, st);
    }

    public void updateTextContent(String blockId, String title, String content) {
        Optional<TextBlock> opt = textBlockDAO.findById(blockId);
        if (opt.isEmpty()) return;
        TextBlock t = opt.get();
        t.setTitle(title);
        t.setContent(content);
        textBlockDAO.update(t);
    }

    // ===== 링크 블록 (FR-07) =====
    public List<Placed<LinkBlock>> loadLinkBlocks(String subjectId) {
        List<Placed<LinkBlock>> result = new ArrayList<>();
        for (LinkBlock l : linkBlockDAO.findBySubject(subjectId)) {
            CanvasState st = canvasStateDAO.findByBlock(l.getId())
                    .orElseGet(() -> new CanvasState(subjectId, l.getId(),
                            40, 40, LINK_DEFAULT_W, LINK_DEFAULT_H));
            result.add(new Placed<>(l, st));
        }
        return result;
    }

    public Placed<LinkBlock> createLinkBlock(String subjectId, String url, String displayName,
                                             double worldX, double worldY) {
        if (url == null || url.isBlank())
            throw new IllegalArgumentException("URL을 입력해주세요.");

        String name = (displayName == null || displayName.isBlank()) ? url : displayName;
        LinkBlock l = new LinkBlock(subjectId, url, name);
        l.setLinkType("web");

        if (!l.isValidUrl())
            throw new IllegalArgumentException("유효하지 않은 URL입니다. (http:// 또는 https:// 포함)");

        linkBlockDAO.insert(l);

        CanvasState st = new CanvasState(subjectId, l.getId(),
                worldX, worldY, LINK_DEFAULT_W, LINK_DEFAULT_H);
        canvasStateDAO.insert(st);

        eventBus.publish(EventBus.SUBJECT_CHANGED, "canvas-link-created");
        return new Placed<>(l, st);
    }

    public void updateLink(String blockId, String url, String displayName) {
        Optional<LinkBlock> opt = linkBlockDAO.findById(blockId);
        if (opt.isEmpty()) return;
        LinkBlock l = opt.get();
        l.setUrl(url);
        String name = (displayName == null || displayName.isBlank()) ? url : displayName;
        l.setDisplayName(name);
        l.setName(name);
        linkBlockDAO.update(l);
    }

    // ===== 파일 블록 (FR-05 + FR-08) =====

    /**
     * 캔버스에 배치된 파일 블록만 로드.
     * 파일 블록은 트리에 항상 존재하지만, canvas_state 가 있는 것만 캔버스에 그린다.
     */
    public List<Placed<FileBlock>> loadFileBlocks(String subjectId) {
        List<Placed<FileBlock>> result = new ArrayList<>();
        for (FileBlock f : fileBlockDAO.findBySubject(subjectId)) {
            Optional<CanvasState> st = canvasStateDAO.findByBlock(f.getId());
            if (st.isPresent()) {                 // 배치된 것만 캔버스에 표시
                result.add(new Placed<>(f, st.get()));
            }
        }
        return result;
    }

    /**
     * 기존 파일 블록을 캔버스의 지정 위치에 배치.
     * 이미 배치돼 있으면 위치만 갱신하고, 없으면 새 배치를 만든다(중복 방지).
     *
     * @return 배치 결과(블록 + 상태). 파일 블록이 존재하지 않으면 빈 Optional.
     */
    public Optional<Placed<FileBlock>> placeFileBlock(String subjectId, String fileBlockId,
                                                      double worldX, double worldY) {
        Optional<FileBlock> opt = fileBlockDAO.findById(fileBlockId);
        if (opt.isEmpty()) return Optional.empty();
        FileBlock f = opt.get();

        Optional<CanvasState> existing = canvasStateDAO.findByBlock(fileBlockId);
        CanvasState st;
        if (existing.isPresent()) {
            st = existing.get();
            st.setPosX(worldX);
            st.setPosY(worldY);
            canvasStateDAO.update(st);
        } else {
            st = new CanvasState(subjectId, fileBlockId, worldX, worldY, FILE_DEFAULT_W, FILE_DEFAULT_H);
            canvasStateDAO.insert(st);
        }

        eventBus.publish(EventBus.SUBJECT_CHANGED, "canvas-file-placed");
        return Optional.of(new Placed<>(f, st));
    }

    /** 이미 캔버스에 배치돼 있는지. (중복 컴포넌트 추가 방지용) */
    public boolean isFilePlaced(String fileBlockId) {
        return canvasStateDAO.findByBlock(fileBlockId).isPresent();
    }

    /**
     * 캔버스에서 파일 블록 "배치만" 제거. 파일 블록 자체(트리)는 남긴다.
     */
    public void removeFilePlacement(String fileBlockId) {
        canvasStateDAO.deleteByBlock(fileBlockId);
        eventBus.publish(EventBus.SUBJECT_CHANGED, "canvas-file-removed");
    }

    // ===== 공통 배치 상태 =====
    public void saveState(CanvasState state) {
        canvasStateDAO.update(state);
    }

    public Optional<CanvasState> findStateByBlock(String blockId) {
        return canvasStateDAO.findByBlock(blockId);
    }

    public void deleteTextBlock(String blockId) {
        canvasStateDAO.deleteByBlock(blockId);
        textBlockDAO.deleteById(blockId);
        eventBus.publish(EventBus.SUBJECT_CHANGED, "canvas-block-deleted");
    }

    public void deleteLinkBlock(String blockId) {
        canvasStateDAO.deleteByBlock(blockId);
        linkBlockDAO.deleteById(blockId);
        eventBus.publish(EventBus.SUBJECT_CHANGED, "canvas-block-deleted");
    }

    /** 블록 모델 + 배치 상태를 함께 들고 다니는 묶음. */
    public record Placed<T>(T block, CanvasState state) {}
}
