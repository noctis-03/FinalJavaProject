package com.blople.model.block;

import com.blople.common.Constants;
import com.blople.model.enums.BlockType;

/**
 * 텍스트 블록 (FR-06) — 무한 캔버스 위 포스트잇 형태.
 */
public class TextBlock extends Block {

    private String subjectId;        // nullable
    private String title;
    private String content;
    private String backgroundColor;  // hex

    public TextBlock(String subjectId, String title, String content) {
        super(BlockType.TEXT, title != null ? title : "메모", Constants.DEFAULT_TEXT_BLOCK_BG_HEX);
        this.subjectId = subjectId;
        this.title = title;
        this.content = content;
        this.backgroundColor = Constants.DEFAULT_TEXT_BLOCK_BG_HEX;
    }

    public String getSubjectId()                          { return subjectId; }
    public void setSubjectId(String subjectId)            { this.subjectId = subjectId; touch(); }
    public String getTitle()                              { return title; }
    public void setTitle(String title)                    { this.title = title; touch(); }
    public String getContent()                            { return content; }
    public void setContent(String content)                { this.content = content; touch(); }
    public String getBackgroundColor()                    { return backgroundColor; }
    public void setBackgroundColor(String color)          { this.backgroundColor = color; touch(); }
}
