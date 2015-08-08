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
import com.paperlink.util.StringCheck;
import com.paperlink.util.StringWithRect;
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

    // 중국어 분석기
    //private CnAnalyser cnAnalyser = new CnAnalyser();

    private WordAnalyser wordAnalyser = null; // english analyser
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
            List<StringWithRect> glyphs = new ArrayList<StringWithRect>();

            new PdfContentStreamProcessor(new SpreadedTextRenderListener(glyphs)).
                        processContent(ContentByteUtils.getContentBytesForPage(reader, page),
                                reader.getPageN(page).getAsDict(PdfName.RESOURCES));

            List<BookLink> bookLinks = makeMeaningfulWordToLink(getStringListFromGlyph(glyphs));
            printLinkInfo(stamper, page, bookLinks, FORMAT_OUT_TEXT);
        }
        writer.close();
        stamper.close();
        reader.close();
    }

    // 각 개별 글자들로부터 단어로 묶어낸다. (사각형 좌표도 머지 연산이 됨)
    // 단지 분리자에 의해 분리된 스트링에 불과한다.
    // 분리자 delimeter는 -config.xml 파일에 있다.
    private List<StringWithRect> getStringListFromGlyph(List<StringWithRect> glyphs) {
        List<StringWithRect> strList = new ArrayList<StringWithRect>();

        for (int i = 0, spanStart = 0; i < glyphs.size(); i++) {
            if (glyphs.get(i).getText() != null &&
                    glyphs.get(i).getText().matches(bookProperties.getDelimeterPattern().toString())) {
                strList.add(StringWithRect.mergeFrom(glyphs, spanStart, i - 1));

                spanStart = i + 1;
            }
        }
        return strList;
    }

    // 의미있는 단어에게 링크를 부착한다. 의미있는 단어를 만들려면 미리 정의한 사전이 필요하다.
    // 형태소 분석을 통해 분해하는 것도 여기서 작업을 한다.
    private List<BookLink> makeMeaningfulWordToLink(List<StringWithRect> strings) {
        List<BookLink> bookLinks = new ArrayList<BookLink>();

        for (StringWithRect word : strings) {

            // 미리 정의한 링크들은 -config.xml에 있다. 주로 멀티미디어 리소스로 연결하는데 사용한다
            if (bookProperties.getPreDefinedLinks() != null) {
                BookLink aBookLink = bookProperties.getPreDefinedLinks().get(word.getText());
                if (aBookLink != null) {
                    aBookLink.setText(word.getText());
                    aBookLink.setPath(bookProperties.getMediaURL() + aBookLink.getPath());
                    aBookLink.setBoundingRect(word.getRect());
                    bookLinks.add(aBookLink);
                    continue;
                }
            }

            //if (word.getText().length() < 3 || !StringCheck.isOnlyAlphabet(word.getText())) { // 영어책
            if (word.getText().length() < 1 || !StringCheck.containsChinese(word.getText())) { // 중국어책
                bookLinks.add(null);
                    continue;
            }

            if (word.getText().length() > 1 && StringCheck.containsChinese(word.getText())) {
                CnAnalyser.analysis(word.getText()); // 중국어 파서
            }

            bookLinks.add(new BookLink(word.getText(),"LINK", bookProperties.getSearchSite(), word.getRect()));
        }
        if (bookLinks.size() != strings.size())
            System.out.printf("error during making link urls\n");

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

        BookLink bookLink;
        for (int i=0; i < bookLinks.size(); i++) {
            bookLink = bookLinks.get(i);
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
        /*
        printWriter.print("Rotation of page 1: ");
        printWriter.println(pdfReader.getPageRotation(1));
        printWriter.print("Page size with rotation of page 1: ");
        printWriter.println(pdfReader.getPageSizeWithRotation(1));
        printWriter.print("Is rebuilt? ");
        printWriter.println(pdfReader.isRebuilt());
        printWriter.print("Is encrypted? ");
        printWriter.println(pdfReader.isEncrypted());
        printWriter.println();
        */
        writer.flush();
    }

}