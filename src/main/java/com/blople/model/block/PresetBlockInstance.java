package com.blople.model.block;

/**
 * 미리블록을 캘린더에 배치했을 때 생성되는 실제 인스턴스.
 * Block 을 상속하지 않는다 (값 객체에 가까움).
 */
public class PresetBlockInstance {

    private final String id;
    private final String presetBlockId;
    private String startDatetime;
    private String endDatetime;

    public PresetBlockInstance(String id, String presetBlockId,
                               String startDatetime, String endDatetime) {
        this.id = id;
        this.presetBlockId = presetBlockId;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
    }

    public String getId()                                 { return id; }
    public String getPresetBlockId()                      { return presetBlockId; }
    public String getStartDatetime()                      { return startDatetime; }
    public void setStartDatetime(String startDatetime)    { this.startDatetime = startDatetime; }
    public String getEndDatetime()                        { return endDatetime; }
    public void setEndDatetime(String endDatetime)        { this.endDatetime = endDatetime; }
}
