package com.blople.service;

import com.blople.common.EventBus;
import com.blople.database.dao.PresetBlockDAO;
import com.blople.model.block.PresetBlock;
import com.blople.model.block.ScheduleBlock;
import com.blople.model.enums.Category;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 미리블록 서비스 (FR-02).
 *
 * <p>책임:
 * <ul>
 *   <li>미리블록 템플릿 CRUD</li>
 *   <li>템플릿을 캘린더 특정 시각에 "배치" → {@link ScheduleBlock} 생성
 *       (배치 결과는 일정 블록이며, 템플릿 수정은 기존 배치에 영향 없음)</li>
 * </ul>
 */
public class PresetBlockService {

    public static final DateTimeFormatter TS_FMT = ScheduleBlock.TS_FORMATTER;

    private final PresetBlockDAO presetBlockDAO;
    private final CalendarService calendarService;
    private final EventBus eventBus = EventBus.getInstance();

    public PresetBlockService(PresetBlockDAO presetBlockDAO, CalendarService calendarService) {
        this.presetBlockDAO = presetBlockDAO;
        this.calendarService = calendarService;
    }

    // ===== 조회 =====
    public List<PresetBlock> getAll() {
        return presetBlockDAO.findAll();
    }

    public Optional<PresetBlock> findById(String blockId) {
        return presetBlockDAO.findById(blockId);
    }

    // ===== CRUD =====
    public PresetBlock create(String name, String color, int durationMinutes, Category category) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("미리블록 이름을 입력해주세요.");
        if (durationMinutes <= 0)
            throw new IllegalArgumentException("소요 시간은 1분 이상이어야 합니다.");
        if (presetBlockDAO.findByName(name).isPresent())
            throw new IllegalArgumentException("이미 존재하는 이름입니다: " + name);

        PresetBlock p = new PresetBlock(name, color, durationMinutes, category);
        presetBlockDAO.insert(p);
        eventBus.publish(EventBus.SUBJECT_CHANGED, "preset-created");
        return p;
    }

    public void delete(String blockId) {
        boolean ok = presetBlockDAO.deleteById(blockId);
        if (ok) eventBus.publish(EventBus.SUBJECT_CHANGED, "preset-deleted");
    }

    // ===== 캘린더 배치 =====

    /**
     * 미리블록을 캘린더의 지정 시각에 배치한다.
     * 시작 시각 + 템플릿 소요시간 = 종료 시각으로 일정 블록을 만들어 등록한다.
     *
     * @param presetBlockId 템플릿 id
     * @param startDatetime "yyyy-MM-dd HH:mm:ss"
     * @return 생성된 일정 블록
     */
    public ScheduleBlock placeOnCalendar(String presetBlockId, String startDatetime) {
        PresetBlock preset = presetBlockDAO.findById(presetBlockId).orElseThrow(
                () -> new IllegalArgumentException("미리블록을 찾을 수 없음: " + presetBlockId));

        LocalDateTime start = LocalDateTime.parse(startDatetime, TS_FMT);
        String end = start.plusMinutes(preset.getDurationMinutes()).format(TS_FMT);

        ScheduleBlock block = new ScheduleBlock(
                preset.getName(), preset.getColor(), startDatetime, end);
        calendarService.addScheduleBlock(block); // 내부에서 SCHEDULE_BLOCK_CREATED 발행 → 캘린더 갱신
        return block;
    }
}
