package com.blople.view.canvas;

import com.blople.model.block.LinkBlock;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * 캔버스 위 링크 블록 (FR-07).
 *
 * <p>제목(표시 이름) 아래에 URL 을 보여준다. 노드 그래프에서 실행 블록의
 * 연결 대상이 되며, 좌측 입력 포트(●)를 가진다.
 * 더블클릭 편집은 {@link InfiniteCanvasPanel} 의 onEditRequest 로 위임된다.
 */
public class LinkBlockComponent extends CanvasBlockComponent {

    private static final Color LINK_BG = new Color(0xEAF2FF);

    private final LinkBlock model;

    public LinkBlockComponent(LinkBlock model, double x, double y, double w, double h) {
        super(model.getId(), x, y, w, h);
        this.model = model;
    }

    public LinkBlock getModel() { return model; }

    /** 실행 블록의 연결 대상 → 입력 포트 보유. */
    @Override
    public boolean hasInputPort() { return true; }

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

        int textX = sx + 12; // 좌측 입력 포트 자리 확보
        g2.setColor(new Color(0x3A6FF0));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        String shown = clip(g2, url, sw - 18);
        g2.drawString(shown, textX, sy + 34);

        // 본문 영역이 충분하면 안내 문구
        if (sh >= 56) {
            g2.setColor(new Color(0x999999));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString("더블클릭하여 편집", textX, sy + 50);
        }
    }
}
