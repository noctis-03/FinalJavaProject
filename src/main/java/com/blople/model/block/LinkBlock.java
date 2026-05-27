package com.blople.model.block;

import com.blople.model.enums.BlockType;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 링크 블록 (FR-07).
 */
public class LinkBlock extends Block {

    private String subjectId;       // nullable
    private String url;
    private String displayName;
    private String linkType;        // "video", "doc", "web" ...

    public LinkBlock(String subjectId, String url, String displayName) {
        super(BlockType.LINK, displayName != null ? displayName : url, "#4D96FF");
        this.subjectId = subjectId;
        this.url = url;
        this.displayName = displayName;
    }

    public String getSubjectId()                       { return subjectId; }
    public void setSubjectId(String subjectId)         { this.subjectId = subjectId; touch(); }
    public String getUrl()                             { return url; }
    public void setUrl(String url)                     { this.url = url; touch(); }
    public String getDisplayName()                     { return displayName; }
    public void setDisplayName(String displayName)     { this.displayName = displayName; touch(); }
    public String getLinkType()                        { return linkType; }
    public void setLinkType(String linkType)           { this.linkType = linkType; touch(); }

    public boolean isValidUrl() {
        if (url == null || url.isBlank()) return false;
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equals("http") || scheme.equals("https"));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public void openInBrowser() throws IOException, URISyntaxException {
        if (!isValidUrl()) throw new IOException("유효하지 않은 URL: " + url);
        if (!Desktop.isDesktopSupported()) throw new IOException("Desktop API 미지원");
        Desktop.getDesktop().browse(new URI(url));
    }
}
