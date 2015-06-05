package com.paperlink.service;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.parser.ContentByteUtils;
import com.itextpdf.text.pdf.parser.PdfContentStreamProcessor;
import com.paperlink.CustomProperties;
import com.paperlink.SpreadedTextRenderListener;
import com.paperlink.dao.WordDaoImpl;
import com.paperlink.domain.BookLink;
import com.paperlink.util.StringWithRect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@Service
public class LinkBuilder {

    @Autowired
    private WordAnalyser wordAnalyser;
    @Autowired
    private WordDaoImpl wordDictionaryService;
    @Autowired
    private CustomProperties bookProperties;
    public void setBookProperties(CustomProperties bookProperties) {
        this.bookProperties = bookProperties;
    }

    /**
     * Extracts texts from a PDF document and adds the link information to its text .
     *
     * @throws IOException, DocumentException
     */

    private PdfReader pdfReader;
    private PdfStamper pdfStamper;
    private PrintWriter printWriter;

    public void scanDocument(String jobName)  throws IOException, DocumentException {

        pdfReader = new PdfReader(bookProperties.getPdfFilePath() + jobName + ".pdf");
        pdfStamper = new PdfStamper(pdfReader, new FileOutputStream("results/" + jobName + "_result.pdf"));
        printWriter = new PrintWriter(new FileOutputStream("results/" + jobName + "_result.txt"));

        // for each page, I'll reset listener so ...
        for (int currentPage = 1; currentPage <= pdfReader.getNumberOfPages(); currentPage++) {
            List<StringWithRect> chars = new ArrayList<StringWithRect>();

            new PdfContentStreamProcessor(new SpreadedTextRenderListener(chars)).
                        processContent(ContentByteUtils.getContentBytesForPage(pdfReader, currentPage),
                                pdfReader.getPageN(currentPage).getAsDict(PdfName.RESOURCES));

            addLinksToPage(currentPage, getStringList(chars));
        }
        printWriter.close();
        pdfStamper.close();
        pdfReader.close();
    }

    private List<StringWithRect> getStringList(List<StringWithRect> chars) {
        List<StringWithRect> result = new ArrayList<StringWithRect>();

        for (int i = 0, spanStart = 0; i < chars.size(); i++) {
            if (chars.get(i).getText() != null &&
                    chars.get(i).getText().matches(bookProperties.getDelimeterPattern().toString())) {
                result.add(StringWithRect.mergeFrom(chars, spanStart, i - 1));

                spanStart = i + 1;
            }
        }
        return result;
    }

    private List<BookLink> makeAllTextToLinks(List<StringWithRect> strings) {
        List<BookLink> bLinks = new ArrayList<BookLink>();

        for (StringWithRect word : strings) {

            // 책사전에 예약된 단어에 대한 링크가 있는지
            if (bookProperties.getPreDefinedLinks() != null) {
                BookLink aBookLink = bookProperties.getPreDefinedLinks().get(word.getText());
                if (aBookLink != null) {
                    aBookLink.setText(word.getText());
                    aBookLink.setPath(bookProperties.getMediaURL() + aBookLink.getPath());
                    bLinks.add(aBookLink);
                    continue;
                }
            }

            if (word.getText().length() < 3) {
                bLinks.add(null);
                continue;
            }
            // 단어의 원형을 검사
            String morph = wordAnalyser.morphologicalAnalysis(word.getText());
            //if (morph != null && wordDictionaryService.getWord(trimmedText) != null) {
            //    bLinks.add(new BookLink(morph+":","html/text","url",searchSite));
            //}
            //else {
            //    bLinks.add(new BookLink(trimmedText,"html/text","url",searchSite));
            //
            //}
            if (morph.length() > 3 && !morph.equals(word.getText()))
                bLinks.add(new BookLink(word.getText()+"," + morph, "html/text", "LINK", bookProperties.getSearchSite()));
            else
                bLinks.add(new BookLink(word.getText(),"html/text","LINK", bookProperties.getSearchSite()));

        }
        if (bLinks.size() != strings.size())
            System.out.printf("error during making link urls\n");

        return bLinks;
    }

    private List<BookLink> makeLinks(List<StringWithRect> strings) {
        List<BookLink> bLinks = new ArrayList<BookLink>();

        for (StringWithRect word : strings) {

            // 책사전에 예약된 단어에 대한 링크가 있는지
            if (bookProperties.getPreDefinedLinks().get(word.getText()) != null) {
                bLinks.add(bookProperties.getPreDefinedLinks().get(word.getText()));
                continue;
            }

            // 영문인가?
            if (!isAlphabet(word.getText())) {
                bLinks.add(null);
                continue;
            }
            // 2자 미만은 링크 안함
            if (word.getText().length() < 3) {
                bLinks.add(null);
                continue;
            }

            // 단어의 원형을 검사
            String morph = wordAnalyser.morphologicalAnalysis(word.getText());
            //if (morph != null && wordDictionaryService.getWord(trimmedText) != null) {
            //    bLinks.add(new BookLink(morph+":","html/text","url",searchSite));
            //}
            //else {
            //    bLinks.add(new BookLink(trimmedText,"html/text","url",searchSite));
            //
            //}
            if (morph.length() > 3)
                bLinks.add(new BookLink(word.getText()+"," + morph,"html/text","LINK", bookProperties.getSearchSite()));
            else
                bLinks.add(new BookLink(word.getText(),"html/text","LINK", bookProperties.getSearchSite()));

        }
        if (bLinks.size() != strings.size())
            System.out.printf("error during making link urls\n");

        return bLinks;
    }

    /**
     * adds the link to the page from texts and rects lists.
     * @param page  current page number
     * @param strings  List<StringsWithRect>
     * @throws
     */
    private void addLinksToPage(int page, List<StringWithRect> strings) {

        List<BookLink> bookLinks = makeAllTextToLinks(strings);

        PdfContentByte pdf_out = pdfStamper.getOverContent(page);
        pdf_out.saveState();
        pdf_out.setLineWidth(0.01f);

        printWriter.printf("PageStart %03d\n",page);

        BookLink bl;
        StringWithRect sr;
        for (int i=0; i < strings.size(); i++) {
            bl = bookLinks.get(i);
            sr = strings.get(i);
            if (bl != null) {
                int x = sr.getRect().x;
                int y = sr.getRect().y;
                int w = x + sr.getRect().width;
                int h = y + sr.getRect().height;

                printWriter.printf("{\"%s\",\"%d %d %d %d\",\"%s\",\"%s\",\"%s\"}\n",
                        bl.getText(), x, y, w, h, bl.getFormat(), bl.getAction(),bl.getPath());

                pdf_out.setAction(new PdfAction(bl.getPath()), x, y, w, h);
            }
        }
        printWriter.printf("PageEnd\n");
        System.out.printf(".");

        pdf_out.stroke();
        pdf_out.closePath();
        pdf_out.restoreState();
    }


    private boolean isAlphabet(String word) {
        for (char c : word.toCharArray()) {
            if(!(Character.isAlphabetic(c)))
                return false;
        }
        return true;
    }
}