package com.paperlink.dao;

import com.paperlink.domain.BookLink;

import java.util.Map;


public class BookLinkDaoImpl implements BookLinkDao {

    private Map<String, BookLink> bookLinkDictionary;

    public BookLink getBookLink(String id) {
        return bookLinkDictionary.get(id);
    }

    public boolean isExist(String id) {
        return getBookLink(id) != null;
    }

    public int getCount() { return bookLinkDictionary.size(); }
}
