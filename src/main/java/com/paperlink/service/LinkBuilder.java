package com.paperlink.service;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.parser.ContentByteUtils;
import com.itextpdf.text.pdf.parser.PdfContentStreamProcessor;
import com.paperlink.CustomProperties;
import com.paperlink.SpreadedTextRenderListener;
import com.paperlink.dao.WordDaoImpl;
import com.paperlink.domain.BookLink;
import com.paperlink.util.HexStringConverter;
import com.paperlink.util.PaperGlyphs;
import com.paperlink.util.StringCheck;
import org.apache.log4j.chainsaw.Main;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class LinkBuilder {

    private WordAnalyser wordAnalyser = null; // english analyser
    @Autowired
    private WordDaoImpl wordDictionaryService;
    @Autowired
    private CustomProperties bookProperties;
    public void setBookProperties(CustomProperties bookProperties) {
        this.bookProperties = bookProperties;
    }

    /**
     * Extracts texts from a PDF document and adds the link information to its text
     */

    private PrintWriter writer;

    private final String FORMAT_OUT_TEXT = "{\"%s\",\"%d\",\"%d %d %d %d\",\"%s\",\"%s\"}\n";

    // 시작 지점이다
    public void scanDocument(String jobName)  throws IOException, DocumentException {

        PdfReader reader = new PdfReader(bookProperties.getPdfFilePath() + jobName + ".pdf"); // 원본 pdf
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream("results/" + jobName + "_result.pdf")); // 새로 만들 pdf
        writer = new PrintWriter(new FileOutputStream("results/" + jobName + "_result.txt")); // 좌표 정보가 들어가는 text 파일

        int numPage = reader.getNumberOfPages();
        printPageInfo(numPage, reader.getPageSize(1)); // print PDF size information

        // for each page, I'll reset listener so ...
        for (int page = 1; page <= numPage; page++) {
            //List<StringWithRect> glyphs = new ArrayList<StringWithRect>();
            PaperGlyphs glyphs = new PaperGlyphs();

            //new PdfContentStreamProcessor(new SpreadedTextRenderListener(glyphs)).
            new PdfContentStreamProcessor(new SpreadedTextRenderListener(glyphs)).
            processContent(ContentByteUtils.getContentBytesForPage(reader, page),
                    reader.getPageN(page).getAsDict(PdfName.RESOURCES));

            // glyphs에는 현재 페이지의 모든 문자(glyph)들이 들어 있다.
            List<PaperGlyphs> glyphsList = getStringListFromGlyph(glyphs);
            List<BookLink> bookLinks = makeMeaningfulWordToLink(glyphsList);
            printLinkInfo(stamper, page, bookLinks, FORMAT_OUT_TEXT);
        }
        writer.close();
        stamper.close();
        reader.close();
    }

    // 각 개별 글자들로부터 단어로 묶어낸다. (사각형 좌표도 머지 연산이 됨)
    // 단지 분리자에 의해 분리된 스트링에 불과한다.
    // delimeter는 -config.xml 파일에 있다.
    //private List<StringWithRect> getStringListFromGlyph(List<StringWithRect> glyphs) {
    private List<PaperGlyphs> getStringListFromGlyph(PaperGlyphs glyphs) {
        List<PaperGlyphs> glyphsList = new ArrayList<PaperGlyphs>();

        for (int i = 0, spanStart = 0; i < glyphs.getText().length(); i++) {

            if (glyphs.getText().substring(i, i + 1).matches(bookProperties.getDelimeterPattern().toString())) {
                glyphsList.add(glyphs.subGlyph(glyphs, spanStart, i));
                spanStart = i + 1;
            }
        }
        return glyphsList;
    }

    // 미리 정의한 단어에게 링크를 부착한다. -config파일에 등록된 단어를 의미한다.
    // 형태소 분석을 통해 분해하는 것도 여기서 작업을 한다.
    private List<BookLink> makeMeaningfulWordToLink(List<PaperGlyphs> glyphsList) {
        List<BookLink> bookLinks = new ArrayList<BookLink>();

        for (PaperGlyphs glyphs : glyphsList) {

            // 미리 정의한 링크들은 -config.xml에 있다. 주로 멀티미디어 리소스로 연결하는데 사용한다
            if (bookProperties.getPreDefinedLinks() != null) {
                BookLink aBookLink = bookProperties.getPreDefinedLinks().get(glyphs.getText());
                if (aBookLink != null) {
                    aBookLink.setText(glyphs.getText());
                    aBookLink.setPath(bookProperties.getMediaURL() + aBookLink.getPath());
                    aBookLink.setBoundingRect(glyphs.getBoundingRect());
                    bookLinks.add(aBookLink);
                    continue;
                }
            }

            //if (word.getText().length() < 3 || !StringCheck.isOnlyAlphabet(word.getText())) { // 영어책
            if (glyphs.getText().length() < 1 || !StringCheck.containsChinese(glyphs.getText())) { // 중국어책
                bookLinks.add(null);
                    continue;
            }

            if (glyphs.getText().length() > 1 && StringCheck.containsChinese(glyphs.getText())) {
                List<Integer> runOffsets = CnAnalyser.analysis(glyphs.getText()); // 중국어 파서
                if (runOffsets.size() > 0) {
                    int index = 0;
                    for (int i = 0; i < runOffsets.size(); i++) {
                        int run = runOffsets.get(i);

                        bookLinks.add(new BookLink(glyphs.getText().substring(index, index+run),
                                "LINK",
                                bookProperties.getSearchSite(),
                                glyphs.getBoundingRectBetween(index, index+run)));

                        index += run;
                    }
                }
            }
            //bookLinks.add(new BookLink(glyphs.getText(),"LINK", bookProperties.getSearchSite(), glyphs.getBoundingRect()));
        }

        return bookLinks;
    }

    /**
     * adds the link to the page from texts and rects lists.
     */
    private void printLinkInfo(PdfStamper stamper, int page, List<BookLink> bookLinks, String PRINT_FORMAT) {

        //List<BookLink> bookLinks = makeAllTextToLinks(strings);
        PdfContentByte overCanvas = stamper.getOverContent(page); // copy page to new canvas

        overCanvas.saveState();
        overCanvas.setLineWidth(0.01f);

        for (BookLink bookLink : bookLinks) {

            if (bookLink != null) {
                int llx = bookLink.getBoundingRect().x;
                int lly = bookLink.getBoundingRect().y;
                int urx = llx + bookLink.getBoundingRect().width;
                int ury = lly + bookLink.getBoundingRect().height;
                try {
                    String theHexaText = HexStringConverter.getHexStringConverterInstance().stringToHex(bookLink.getText());

                    writer.printf(PRINT_FORMAT, bookLink.getText(), page, llx, lly, urx, ury, bookLink.getAction(), bookLink.getPath()+theHexaText);
                    overCanvas.setAction(new PdfAction(bookLink.getPath()+theHexaText), llx, lly, urx, ury); // add the link
                }
                catch (UnsupportedEncodingException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        System.out.printf("%3d ", page);
        if (page%20 == 0)
            System.out.println();

        overCanvas.stroke();
        overCanvas.closePath();
        overCanvas.restoreState();
    }

    public void printPageInfo(int numPage, Rectangle mediaBox) throws IOException {
        writer.printf("Number of pages: %s\n", numPage);
        writer.printf("Size of page: [%.2f %.2f %.2f %.2f]\n", mediaBox.getLeft(), mediaBox.getBottom(), mediaBox.getRight(), mediaBox.getTop());
        writer.flush();
    }

}