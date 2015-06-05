package com.paperlink.domain;

public class BookLink {
    private String text;
    private String format;
    private String action;
    private String path;

    public BookLink() {}
    public BookLink(String text, String format, String action, String path) {
        this.text = text;
        this.format = format;
        this.action = action;
        this.path = path;
    }

    public void setText(String text) { this.text = text; }
    public void setFormat(String format) { this.format = format; }
    public void setAction(String action) { this.action = action; }
    public void setPath(String path) { this.path = path; }

    public String getText() { return text; }
    public String getFormat() { return format; }
    public String getAction() { return action; }
    public String getPath() { return path; }
}