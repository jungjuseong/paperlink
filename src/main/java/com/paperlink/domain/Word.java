package com.paperlink.domain;

public class Word {
    private String id;
    private String level;
    private String simpleMeaning;
    private String detail;

    public Word(String ide, String level, String simpleMeaning, String detail) {
        this.id = ide;
        this.level = level;
        this.simpleMeaning = simpleMeaning;
        this.detail = detail;
    }

    public String getId() { return id; }
    public String getLevel() { return level; }
    public String getSimpleMeaning() { return simpleMeaning; }
    public String getDetail() { return detail; }

    public void setId(String id) { this.id = id; }
    public void setLevel(String level) { this.level = level; }
    public void setDetail(String detail) { this.detail = detail; }
    public void setSimpleMeaning(String simpleMeaning) { this.simpleMeaning = simpleMeaning; }
}
