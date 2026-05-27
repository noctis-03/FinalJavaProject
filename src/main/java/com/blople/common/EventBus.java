package com.blople.common;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 컴포넌트 간 느슨한 결합을 위한 간단한 이벤트 버스 (싱글톤).
 *
 * <p>Service 계층이 데이터 변경 시 이벤트를 publish 하고,
 * View 계층이 subscribe 해 UI 를 갱신하는 용도이다.
 *
 * <p>스레드 안전을 위해 ConcurrentHashMap + CopyOnWriteArrayList 사용.
 *
 * <p>이벤트 종류 (문자열 상수)는 본 클래스에 모아둔다.
 * 이벤트 데이터(payload)는 임의 객체로, 받는 쪽이 캐스팅한다.
 */
public final class EventBus {

    // ===== 이벤트 키 =====
    public static final String SCHEDULE_BLOCK_CREATED = "schedule.block.created";
    public static final String SCHEDULE_BLOCK_UPDATED = "schedule.block.updated";
    public static final String SCHEDULE_BLOCK_DELETED = "schedule.block.deleted";
    public static final String SCHEDULE_BLOCK_MOVED   = "schedule.block.moved";

    public static final String CALENDAR_VIEW_CHANGED  = "calendar.view.changed";
    public static final String CALENDAR_DATE_CHANGED  = "calendar.date.changed";

    public static final String ASSIGNMENT_CREATED     = "assignment.created";
    public static final String ASSIGNMENT_UPDATED     = "assignment.updated";
    public static final String ASSIGNMENT_DELETED     = "assignment.deleted";

    public static final String SUBJECT_CHANGED        = "subject.changed";
    public static final String AUTOMATION_EXECUTED    = "automation.executed";

    // ===== 싱글톤 =====
    private static final EventBus INSTANCE = new EventBus();
    public static EventBus getInstance() { return INSTANCE; }

    private final Map<String, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    private EventBus() { /* singleton */ }

    public void subscribe(String event, Consumer<Object> listener) {
        listeners.computeIfAbsent(event, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void unsubscribe(String event, Consumer<Object> listener) {
        List<Consumer<Object>> list = listeners.get(event);
        if (list != null) list.remove(listener);
    }

    public void publish(String event, Object payload) {
        List<Consumer<Object>> list = listeners.get(event);
        if (list == null || list.isEmpty()) return;
        for (Consumer<Object> l : list) {
            try {
                l.accept(payload);
            } catch (Exception e) {
                System.err.println("[EventBus] 리스너 실행 실패 (" + event + "): " + e.getMessage());
            }
        }
    }

    /** 테스트·종료 시 모든 구독 해제. */
    public void clear() {
        listeners.clear();
    }
}
