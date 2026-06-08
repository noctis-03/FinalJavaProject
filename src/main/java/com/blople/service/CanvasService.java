package com.blople.service;

import com.blople.common.EventBus;
import com.blople.database.dao.CanvasStateDAO;
import com.blople.database.dao.TextBlockDAO;
import com.blople.model.block.TextBlock;
import com.blople.model.canvas.CanvasState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 무한 캔버스 서비스 (FR-06, FR-08).
 *
 * <p>현재는 텍스트 블록 + 배치 상태(canvas_state)를 담당한다.
 * 링크/파일 블록 배치는 후속 단계에서 확장한다.
 *
 * <p>블록 생성 시 canvas_state 행을 함께 만들고, 이동/리사이즈 시 갱신한다(NFR-018).
 */
public class CanvasService {

    public static final double DEFAULT_W = 160;
    public static final double DEFAULT_H = 110;

    private final TextBlockDAO textBlockDAO;
    private final CanvasStateDAO canvasStateDAO;
    private final EventBus eventBus = EventBus.getInstance();

    public CanvasService(TextBlockDAO textBlockDAO, CanvasStateDAO canvasStateDAO) {
        this.textBlockDAO = textBlockDAO;
        this.canvasStateDAO = canvasStateDAO;
    }

    /** 한 과목 캔버스의 텍스트 블록 + 각 배치 상태를 함께 조회. */
    public List<Placed<TextBlock>> loadTextBlocks(String subjectId) {
        List<Placed<TextBlock>> result = new ArrayList<>();
        for (TextBlock t : textBlockDAO.findBySubject(subjectId)) {
            CanvasState st = canvasStateDAO.findByBlock(t.getId())
                    .orElseGet(() -> new CanvasState(subjectId, t.getId(), 40, 40, DEFAULT_W, DEFAULT_H));
            result.add(new Placed<>(t, st));
        }
        return result;
    }

    /** 새 텍스트 블록을 지정 위치에 생성. */
    public Placed<TextBlock> createTextBlock(String subjectId, double worldX, double worldY) {
        TextBlock t = new TextBlock(subjectId, "메모", "");
        textBlockDAO.insert(t);

        CanvasState st = new CanvasState(subjectId, t.getId(), worldX, worldY, DEFAULT_W, DEFAULT_H);
        canvasStateDAO.insert(st);

        eventBus.publish(EventBus.SUBJECT_CHANGED, "canvas-block-created");
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

    /** 위치/크기 저장 (이동·리사이즈 후 호출). */
    public void saveState(CanvasState state) {
        canvasStateDAO.update(state);
    }

    public java.util.Optional<CanvasState> findStateByBlock(String blockId) {
        return canvasStateDAO.findByBlock(blockId);
    }

    public void deleteBlock(String blockId) {
        canvasStateDAO.deleteByBlock(blockId);
        textBlockDAO.deleteById(blockId);
        eventBus.publish(EventBus.SUBJECT_CHANGED, "canvas-block-deleted");
    }

    /** 블록 모델 + 배치 상태를 함께 들고 다니는 묶음. */
    public record Placed<T>(T block, CanvasState state) {}
}

