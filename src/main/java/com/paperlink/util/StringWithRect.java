package com.paperlink.util;

import java.awt.*;
import java.util.List;

public class StringWithRect {
    private Rectangle rect;
    private String text;

    public StringWithRect(String text, Rectangle rect) {
        this.rect = rect;
        this.text = text;
    }

    public Rectangle getRect() { return rect;}
    public String getText() { return text;}

    static public StringWithRect mergeFrom(List<StringWithRect> stringsWithRect, int from, int to) {
        return new StringWithRect(mergeStrings(stringsWithRect, from, to),
                mergeRectangles(stringsWithRect, from, to));
    }

    static private String mergeStrings(List<StringWithRect> stringsWithRect, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i <= to; i++) {
            sb.append(stringsWithRect.get(i).getText());
        }
        return sb.toString();
    }

    static private Rectangle mergeRectangles(List<StringWithRect> stringsWithRect, int from, int to) {

        Rectangle resultRect = new Rectangle(stringsWithRect.get(from).getRect());
        for (int i = from + 1; i <= to; i++) {
            resultRect = resultRect.union(stringsWithRect.get(i).getRect());
        }
        return resultRect;
    }
}
