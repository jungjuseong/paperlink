package com.paperlink;

import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import com.paperlink.util.StringWithRect;

import java.awt.*;
import java.util.List;

public class SpreadedTextRenderListener implements RenderListener {

    private List<StringWithRect> stringsWithRect;

    /**
     * Creates a RenderListener that will look for text.
     */
    public SpreadedTextRenderListener(List<StringWithRect> stringsWithRect) {
        this.stringsWithRect = stringsWithRect;
    }

    /**
     * @see RenderListener#beginTextBlock()
     */
    public void beginTextBlock() {    }

    /**
     * @see RenderListener#endTextBlock()
     */
    public void endTextBlock() {    }

    /**
     * @see RenderListener#renderImage(ImageRenderInfo)
     */
    public void renderImage(ImageRenderInfo renderInfo) {
        //text_out.println("*");
    }

    /**
     * @see RenderListener#renderText(TextRenderInfo)
     */
    public void renderText(TextRenderInfo textRenderInfo) {

        for (TextRenderInfo te : textRenderInfo.getCharacterRenderInfos())
            stringsWithRect.add(new StringWithRect(te.getText(), getTextRectangle(te)));
    }

    private Rectangle getTextRectangle(TextRenderInfo renderInfo) {
        int x = (int) renderInfo.getDescentLine().getStartPoint().get(0);
        int y = (int) renderInfo.getDescentLine().getStartPoint().get(1);
        int w = (int) (renderInfo.getAscentLine().getEndPoint().get(0) - renderInfo.getAscentLine().getStartPoint().get(0));
        int h = (int) (renderInfo.getAscentLine().getEndPoint().get(1) - renderInfo.getDescentLine().getStartPoint().get(1));

        return new Rectangle(x, y, w, h);
    }
}
