package com.paperlink.dao;

import com.paperlink.domain.Word;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

@Service("wordDictionaryService")
public class WordDaoImpl implements WordDao {

    private HashMap<String, Word> wordDictionary = null;

    @Autowired
    private DataSource dataSource;

    private void makeWordDictionary() {
        wordDictionary = new HashMap<String, Word>();
        List<Word> allWords = findAllWords();
        for (int i=0; i < allWords.size(); i++) {
            wordDictionary.put(allWords.get(i).getId(), allWords.get(i));
        }
    }

    public Word getWord(String id) {
        if (wordDictionary == null)
            makeWordDictionary();

        return wordDictionary.get(id);
    }

    private List<Word> findAllWords() {
        return new JdbcTemplate(dataSource).query("select * from endict", new WordMapper());
    }

    private static final class WordMapper implements RowMapper<Word> {
        public Word mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Word(rs.getString("id"), rs.getString("level"), rs.getString("simplemeaning"), rs.getString("detail"));
        }
    }
    // get total records
    public int getCount() {
        return wordDictionary.size();
    }

}

