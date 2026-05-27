package com.blople.model.block;

import com.blople.model.enums.BlockType;
import com.blople.model.enums.Category;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 미리블록 템플릿 (FR-02). 실제 캘린더에 배치될 때는 PresetBlockInstance 가 생성된다.
 */
public class PresetBlock extends Block {

    private int durationMinutes;
    private Category category;

    public PresetBlock(String name, String color, int durationMinutes, Category category) {
        super(BlockType.PRESET, name, color);
        this.durationMinutes = durationMinutes;
        this.category = category;
    }

    public int getDurationMinutes()                  { return durationMinutes; }
    public void setDurationMinutes(int minutes)      { this.durationMinutes = minutes; touch(); }
    public Category getCategory()                    { return category; }
    public void setCategory(Category category)       { this.category = category; touch(); }

    /** 캘린더에 배치할 인스턴스를 생성. */
    public PresetBlockInstance createInstance(String startDatetime) {
        LocalDateTime start = LocalDateTime.parse(startDatetime, TS_FORMATTER);
        String endDatetime = start.plusMinutes(durationMinutes).format(TS_FORMATTER);
        return new PresetBlockInstance(
                UUID.randomUUID().toString(),
                this.getId(),
                startDatetime,
                endDatetime
        );
    }
}
