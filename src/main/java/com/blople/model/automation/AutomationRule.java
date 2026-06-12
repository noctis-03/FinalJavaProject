package com.blople.model.automation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 자동화 규칙 (FR-12). automation_rule 테이블과 매핑.
 *
 * <p>과목 페이지 자동화에서는 "실행 블록이 열 블록들의 묶음" 으로 쓰인다.
 * 설계 스키마의 command/slot 중간 단계는 DAO 가 내부적으로 1개씩 만들어 끼우며,
 * 본 모델은 단순화를 위해 {@link OpenBlockRef} 목록을 직접 들고 있는다.
 *
 * <p>캘린더 페이지 자동화(문장형 슬롯)는 후속 단계에서 별도 확장한다.
 */
public class AutomationRule {

    public static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** context 값. */
    public static final String CONTEXT_SUBJECT  = "SUBJECT";
    public static final String CONTEXT_CALENDAR = "CALENDAR";

    private String id;
    private String name;
    private String context;     // SUBJECT / CALENDAR
    private String subjectId;   // 과목 자동화일 때 소속 과목
    private boolean active;
    private String createdAt;

    /** 과목 자동화: 이 규칙이 열 블록(파일/링크)들. */
    private final List<OpenBlockRef> openBlocks = new ArrayList<>();

    /** 신규 생성 — 과목 자동화용. */
    public AutomationRule(String name, String subjectId) {
        this.id        = UUID.randomUUID().toString();
        this.name      = name;
        this.context   = CONTEXT_SUBJECT;
        this.subjectId = subjectId;
        this.active    = true;
        this.createdAt = LocalDateTime.now().format(TS_FMT);
    }

    /** DAO 복원용. */
    public static AutomationRule reconstruct(String id, String name, String context,
                                             String subjectId, boolean active, String createdAt) {
        AutomationRule r = new AutomationRule(name, subjectId);
        r.id        = id;
        r.context   = context;
        r.active    = active;
        r.createdAt = createdAt;
        return r;
    }

    public String getId()                       { return id; }
    public String getName()                     { return name; }
    public void   setName(String name)          { this.name = name; }
    public String getContext()                  { return context; }
    public String getSubjectId()                { return subjectId; }
    public boolean isActive()                   { return active; }
    public void   setActive(boolean active)     { this.active = active; }
    public String getCreatedAt()                { return createdAt; }

    public List<OpenBlockRef> getOpenBlocks()   { return openBlocks; }

    public void addOpenBlock(OpenBlockRef ref) {
        // 중복 방지 (같은 block_id 가 두 번 담기지 않도록)
        boolean exists = openBlocks.stream()
                .anyMatch(o -> o.getBlockId().equals(ref.getBlockId()));
        if (!exists) openBlocks.add(ref);
    }

    public void removeOpenBlock(String blockId) {
        openBlocks.removeIf(o -> o.getBlockId().equals(blockId));
    }

    @Override
    public String toString() {
        return "AutomationRule{name=%s, context=%s, openBlocks=%d}"
                .formatted(name, context, openBlocks.size());
    }
}
