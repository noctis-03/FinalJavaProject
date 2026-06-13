package com.blople.service;

import com.blople.common.EventBus;
import com.blople.database.dao.ExternalLinkDAO;
import com.blople.model.ExternalLink;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * 캘린더 페이지 우측 "외부 링크" 즐겨찾기 서비스.
 *
 * <p>책임:
 * <ul>
 *   <li>링크 CRUD</li>
 *   <li>URL 유효성 검증 + 자동 https:// 보정</li>
 *   <li>시스템 기본 브라우저로 열기</li>
 *   <li>최초 부팅 시 기본 항목(예: 한밭대 홈페이지) 시드</li>
 * </ul>
 */
public class ExternalLinkService {

    public static final String EVENT_CHANGED = "external.link.changed";

    private final ExternalLinkDAO dao;
    private final EventBus eventBus = EventBus.getInstance();

    public ExternalLinkService(ExternalLinkDAO dao) {
        this.dao = dao;
    }

    public List<ExternalLink> getAll() {
        return dao.findAll();
    }

    public ExternalLink create(String name, String url) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }
        String normalized = normalizeUrl(url);
        ExternalLink l = new ExternalLink(name.trim(), normalized);
        // 정렬 순서는 현재 개수만큼 뒤에 붙임
        l.setSortOrder(getAll().size());
        dao.insert(l);
        eventBus.publish(EVENT_CHANGED, l);
        return l;
    }

    public void update(ExternalLink link, String name, String url) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }
        link.setName(name.trim());
        link.setUrl(normalizeUrl(url));
        dao.update(link);
        eventBus.publish(EVENT_CHANGED, link);
    }

    public void delete(String id) {
        boolean ok = dao.deleteById(id);
        if (ok) eventBus.publish(EVENT_CHANGED, id);
    }

    /** 시스템 기본 브라우저로 URL 열기. */
    public void open(ExternalLink link) throws IOException, URISyntaxException {
        if (!Desktop.isDesktopSupported()) {
            throw new IOException("이 환경에서 Desktop API 를 지원하지 않습니다.");
        }
        Desktop.getDesktop().browse(new URI(link.getUrl()));
    }

    /** 최초 실행 시 기본 항목 시드 (목록이 비어있을 때만). */
    public void seedDefaultsIfEmpty() {
        if (!dao.findAll().isEmpty()) return;
        create("한남대학교 홈페이지", "https://www.hannam.ac.kr");


    }


    // ===== 헬퍼 =====
    private String normalizeUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("URL 을 입력해주세요.");
        }
        String url = raw.trim();
        if (!url.matches("(?i)^https?://.*")) {
            url = "https://" + url;
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                throw new IllegalArgumentException("유효하지 않은 URL 입니다: " + raw);
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("유효하지 않은 URL 입니다: " + raw);
        }
        return url;
    }
}
