package com.paperlink;

import com.paperlink.dao.WordDaoImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations= "/scripts/applicationContext.xml")
public class WordDaoTest {
    @Autowired
    private ApplicationContext context;
    @Autowired
    private WordDaoImpl wordDaoImpl;

    @Before
    public void setUp() {
    }

    @Test
    public void addAndGet() {
        //Word aWord = wordDaoImpl.get("branch");
        //assert(aWord.getId().equals("branch"));
        //System.out.printf("get word: %s\n", aWord.getId());

        int count = wordDaoImpl.getCount();
        System.out.printf("total %d words\n",count);
    }

    @Test(expected= EmptyResultDataAccessException.class)
    public void getWordFailure() {

       // Word w = wordDaoImpl.get("Muu__uuu");
    }


    @Test
    public void testSetDataSource() throws Exception {

    }

    @Test
    public void testAdd() throws Exception {

    }

    @Test
    public void testFindWords() throws Exception {

    }

    @Test
    public void testFindExactWords() throws Exception {

    }

    @Test
    public void testFindAllWords() throws Exception {

    }

    @Test
    public void testDeleteAll() throws Exception {

    }

    @Test
    public void testGetCount() throws Exception {

    }
}
