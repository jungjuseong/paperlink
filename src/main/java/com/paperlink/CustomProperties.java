package com.paperlink;

import com.paperlink.domain.BookLink;

import java.net.URL;
import java.util.Map;
import java.util.regex.Pattern;

public class CustomProperties {

    private String bookTitle;
    private String publisher;
    private String publishingDate;
    private Pattern delimeterPattern;
    private String pdfFilePath;
    private String searchSite;
    private URL mediaURL;
    private Map<String, BookLink> preDefinedLinks;

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public void setDelimeterPattern(Pattern delimeterPattern) {
        this.delimeterPattern = delimeterPattern;
    }

    public void setMediaURL(URL mediaURL) {
        this.mediaURL = mediaURL;
    }

    public void setSearchSite(String searchSite) {
        this.searchSite = searchSite;
    }

    public void setPublishingDate(String publishingDate) {
        this.publishingDate = publishingDate;
    }

    public void setPreDefinedLinks(Map<String, BookLink> preDefinedLinks) {
        this.preDefinedLinks = preDefinedLinks;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public void setPdfFilePath(String pdfFilePath) {
        this.pdfFilePath = pdfFilePath;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getPublishingDate() {
        return publishingDate;
    }

    public Pattern getDelimeterPattern() {
        return delimeterPattern;
    }

    public String getPdfFilePath() {
        return pdfFilePath;
    }

    public String getSearchSite() {
        return searchSite;
    }

    public URL getMediaURL() {
        return mediaURL;
    }

    public Map<String, BookLink> getPreDefinedLinks() {
        return preDefinedLinks;
    }
}
