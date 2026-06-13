package com.blople.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 캘린더 페이지 우측 "외부 링크" 패널에 표시되는 즐겨찾기 링크 (대학 사이트 등).
 *
 * <p>블록 시스템과는 별개의 가벼운 즐겨찾기 항목이다.
 */
public class ExternalLink {

    public static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String id;
    private String name;
    private String url;
    private int    sortOrder;
    private String createdAt;

    public ExternalLink(String name, String url) {
        this.id        = UUID.randomUUID().toString();
        this.name      = name;
        this.url       = url;
        this.sortOrder = 0;
        this.createdAt = LocalDateTime.now().format(TS_FMT);
    }

    public static ExternalLink reconstruct(String id, String name, String url,
                                           int sortOrder, String createdAt) {
        ExternalLink l = new ExternalLink(name, url);
        l.id        = id;
        l.sortOrder = sortOrder;
        l.createdAt = createdAt;
        return l;
    }

    public String getId()                    { return id; }
    public String getName()                  { return name; }
    public void   setName(String name)       { this.name = name; }
    public String getUrl()                   { return url; }
    public void   setUrl(String url)         { this.url = url; }
    public int    getSortOrder()             { return sortOrder; }
    public void   setSortOrder(int s)        { this.sortOrder = s; }
    public String getCreatedAt()             { return createdAt; }
}
