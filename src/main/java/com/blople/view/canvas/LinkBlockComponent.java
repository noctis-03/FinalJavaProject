package com.blople.view.canvas;

import com.blople.model.block.LinkBlock;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * 캔버스 위 링크 블록 (FR-07).
 *
 * <p>제목(표시 이름) 아래에 URL 을 회색으로 보여주고, 좌측에 링크 아이콘을 둔다.
 * 더블클릭 편집은 {@link InfiniteCanvasPanel} 의 onEditRequest 로 위임된다.
 * (브라우저 열기는 컨트롤러에서 처리)
 */
public class LinkBlockComponent extends CanvasBlockComponent {

    private static final Color LINK_BG = new Color(0xEAF2FF);

    private final LinkBlock model;

    public LinkBlockComponent(LinkBlock model, double x, double y, double w, double h) {
        super(model.getId(), x, y, w, h);
        this.model = model;
    }

    public LinkBlock getModel() { return model; }

    @Override
    protected Color bodyColor() {
        return LINK_BG;
    }

    @Override
    protected String titleText() {
        String name = model.getDisplayName();
        if (name != null && !name.isBlank()) return "🔗 " + name;
        return "🔗 " + (model.getUrl() == null ? "링크" : model.getUrl());
    }

    @Override
    protected void paintBody(Graphics2D g2, int sx, int sy, int sw, int sh) {
        String url = model.getUrl();
        if (url == null || url.isBlank()) return;

        g2.setColor(new Color(0x3A6FF0));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        String shown = clip(g2, url, sw - 12);
        g2.drawString(shown, sx + 6, sy + 34);

        // 본문 영역이 충분하면 안내 문구
        if (sh >= 56) {
            g2.setColor(new Color(0x999999));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString("더블클릭하여 편집", sx + 6, sy + 50);
        }
    }
}
