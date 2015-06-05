package com.paperlink.dao;

import com.paperlink.domain.Word;

public interface WordDao {
    Word getWord(String id);
    int getCount();
}
