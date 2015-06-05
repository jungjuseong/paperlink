package com.paperlink.dao;

import com.paperlink.domain.BookLink;

public interface BookLinkDao {
    BookLink getBookLink(String id);
    boolean isExist(String id);
    int getCount();
}
