package com.paperlink.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
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
import com.paperlink.util.TokenOffset;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private PrintWriter txtWriter;

    private final String FORMAT_OUT_TEXT = "{\"%s\",\"%d\",\"%d %d %d %d\",\"%s\",\"%s\"}\n";

    private void printPageBoxInfo(PdfReader reader, int page, String boxType) {
        com.itextpdf.text.Rectangle r = reader.getBoxSize(page, boxType);

        System.out.printf("%s\t[%.0f %.0f %.0f %.0f] unit(72dpi), ", boxType, r.getLeft(), r.getBottom(), r.getWidth(), r.getHeight());
        System.out.printf("[%.1f %.1f %.1f %.1f] inch\n", r.getLeft() / 72.0f, r.getBottom() / 72.0f, r.getWidth() / 72.0f, r.getHeight() / 72.0f);
    }

    // 분석 시작 지점이다
    public void scanDocument(String jobName) throws IOException, DocumentException {

        PdfReader reader = new PdfReader(bookProperties.getPdfFilePath() + jobName + ".pdf"); // 원본 pdf
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream("results/" + jobName + "_result.pdf")); // 새로 만들 pdf
        txtWriter = new PrintWriter(new FileOutputStream("results/" + jobName + "_result.txt"));

        int numPage = reader.getNumberOfPages();

        com.itextpdf.text.Rectangle cropBox;
        com.itextpdf.text.Rectangle mediaBox;

        printPageNum(numPage);

        if (bookProperties.getBookTitle().equals("cn_master1")) {
            // for each page, I'll reset listener so ...
            for (int page = 1; page <= numPage; page++) {

                cropBox = reader.getBoxSize(page, "crop");
                mediaBox = reader.getBoxSize(page, "media");

                PaperGlyphs glyphs = usingStreamProcessor(reader, page, "[\\p{script=Han}]*", false);
                List<BookLink> bookLinks = makeMeaningfulWordToLinkforChinese(glyphs);

                printLinkInfo(page, cropBox, bookLinks, FORMAT_OUT_TEXT);

            }

        } else if (bookProperties.getBookTitle().equals("cretec-opti-555")) {

            for (int page = 1; page <= numPage; page++) {

                cropBox = reader.getBoxSize(page, "crop");
                mediaBox = reader.getBoxSize(page, "media");

                printPageBoxInfo(reader, page, "crop");
                //printPageBoxInfo(reader, page, "trim");
                //printPageBoxInfo(reader, page, "art");
                printPageBoxInfo(reader, page, "media");

                PaperGlyphs glyphs = usingStreamProcessor(reader, page, "[0-9\\-]+", true);
                List<BookLink> bookLinks = makeMeaningfulWordToLinkforCretec(glyphs);

                String forehand = "src/main/resources/page_images/cretec_publish_images/책임기업-opti-555_페이지_";

                showLinkedArea(stamper.getOverContent(page), bookLinks, mediaBox, cropBox);

                String resourceName = forehand + String.format("%02d_pbh.tif", page);

                clippingLinkedArea(stamper.getOverContent(page), bookLinks, resourceName, cropBox);

                printLinkInfo(page, cropBox, bookLinks, FORMAT_OUT_TEXT);
            }
        } else if (bookProperties.getBookTitle().equals("hackers_toeic_listening")) {
            for (int page = 1; page <= numPage; page++) {

                PaperGlyphs glyphs = usingStreamProcessor(reader, page, "[A-Za-z0-9_\\.\\-\\s]+", false);
                List<BookLink> bookLinks = makeMeaningfulWordToLinkforHackersToiecListening(glyphs);

                cropBox = reader.getBoxSize(page, "crop");
                mediaBox = reader.getBoxSize(page, "media");

                printLinkInfo(page, cropBox, bookLinks, FORMAT_OUT_TEXT);
            }

        }
        txtWriter.close();
        stamper.close();
        reader.close();
    }

    private PaperGlyphs usingStreamProcessor(PdfReader reader, int page, String filter, boolean shouldWordBreak) throws IOException {
        PaperGlyphs glyphs = new PaperGlyphs();

        new PdfContentStreamProcessor(new SpreadedTextRenderListener(glyphs, filter, shouldWordBreak)).
                processContent(ContentByteUtils.getContentBytesForPage(reader, page),
                        reader.getPageN(page).getAsDict(PdfName.RESOURCES));

        return glyphs;

    }

    // 미리 정의한 단어에게 링크를 부착한다. -config파일에 등록된 단어를 의미한다.
    // 형태소 분석을 통해 분해하는 것도 여기서 작업을 한다.
    private List<BookLink> makeMeaningfulWordToLinkforChinese(PaperGlyphs glyphs) {
        List<BookLink> bookLinks = new ArrayList<BookLink>();

        if (glyphs.getText().length() > 0 || StringCheck.containsChinese(glyphs.getText())) { // 중국어책

            // 형태소 분석을 통해 중국어 단어들을 분리하는 작업
            if (glyphs.getText().length() > 1 && StringCheck.containsChinese(glyphs.getText())) {
                List<Integer> runOffsets = CnAnalyser.analysis(glyphs.getText());
                if (runOffsets.size() > 0) {
                    int index = 0;
                    for (int i = 0; i < runOffsets.size(); i++) {
                        int run = runOffsets.get(i);

                        bookLinks.add(new BookLink(glyphs.getText().substring(index, index + run),
                                "LINK",
                                bookProperties.getSearchSite(),
                                glyphs.getBoundingRectBetween(index, index + run)));

                        index += run;
                    }
                } else if (glyphs.getText().length() == 1) {
                    bookLinks.add(new BookLink(glyphs.getText(),
                            "LINK",
                            bookProperties.getSearchSite(),
                            glyphs.getBoundingRect()));
                }
                //TextRenderInfo tri = glyphs.getTextRenderInfo();
                // do graphics here!!!
            }
        }

        return bookLinks;
    }

    // 미리 정의한 단어에게 링크를 부착한다. -config파일에 등록된 단어를 의미한다.
    // 형태소 분석을 통해 분해하는 것도 여기서 작업을 한다.
    private List<BookLink> makeMeaningfulWordToLinkforCretec(PaperGlyphs glyphs) {
        List<BookLink> bookLinks = new ArrayList<BookLink>();

        Pattern pattern = Pattern.compile("([0-9]{0,3}[-][0-9]{4})"); /// 000-0000 or -0000

        Matcher matcher = pattern.matcher(glyphs.getText());

        while (matcher.find()) {

            bookLinks.add(new BookLink(matcher.group(),
                    "LINK",
                    bookProperties.getSearchSite(),
                    glyphs.getBoundingRectBetween(matcher.start(), matcher.end())));
        }
        System.out.println();

        return bookLinks;
    }

    private List<BookLink> makeMeaningfulWordToLinkforHackersToiecListening(PaperGlyphs glyphs) {
        List<BookLink> bookLinks = new ArrayList<BookLink>();


        if (glyphs.getText().length() > 1) {

            // 형태소 분석을 통해 단어들을 분리하는 작업
            if (glyphs.getText().length() > 1) {
                List<TokenOffset> runOffsets = EnAnalyser.analysis(glyphs.getText());
                if (runOffsets.size() > 0) {
                    for (int i = 0; i < runOffsets.size(); i++) {
                        TokenOffset run = runOffsets.get(i);

                        bookLinks.add(new BookLink(glyphs.getText().substring(run.getStart(), run.getEnd()),
                                "LINK",
                                bookProperties.getSearchSite(),
                                glyphs.getBoundingRectBetween(run.getStart(), run.getEnd())));
                    }
                } else if (glyphs.getText().length() == 1) {
                    bookLinks.add(new BookLink(glyphs.getText(),
                            "LINK",
                            bookProperties.getSearchSite(),
                            glyphs.getBoundingRect()));
                }
                //TextRenderInfo tri = glyphs.getTextRenderInfo();
                // do graphics here!!!
            }
        }

        return bookLinks;
    }

    /**
     * adds the link to the page from texts and rects lists.
     */
    private void printLinkInfo(int page, com.itextpdf.text.Rectangle cropBox, List<BookLink> bookLinks, String PRINT_FORMAT) {

        txtWriter.printf("%d page's CropBox: [%.0f %.0f %.0f %.0f]\n", page, cropBox.getLeft(), cropBox.getBottom(), cropBox.getWidth(), cropBox.getHeight());

        for (BookLink bookLink : bookLinks) {
            if (bookLink != null && bookLinks.size() > 0) {
                int llx = bookLink.getRect().x;
                int lly = bookLink.getRect().y;
                int urx = llx + bookLink.getRect().width;
                int ury = lly + bookLink.getRect().height;

                try {
                    String param = bookLink.getText();

                    if (StringCheck.containsChinese(bookLink.getText()) || StringCheck.containsKorean(bookLink.getText()))
                        param = HexStringConverter.getHexStringConverterInstance().stringToHex(bookLink.getText());

                    txtWriter.printf(PRINT_FORMAT, bookLink.getText(), page, llx, lly, urx, ury, bookLink.getAction(), bookLink.getPath() + param);

                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
        System.out.printf("%3d ", page);
        if (page % 20 == 0)
            System.out.println();
    }


    private void showLinkedArea(PdfContentByte canvas, List<BookLink> bookLinks, com.itextpdf.text.Rectangle mediaBox, com.itextpdf.text.Rectangle cropBox)
            throws DocumentException {

        canvas.saveState();

        canvas.setLineWidth(1.5f);
        canvas.setColorStroke(BaseColor.BLUE);
        canvas.rectangle(mediaBox);
        canvas.stroke();

        canvas.setColorStroke(BaseColor.RED);
        canvas.rectangle(cropBox);
        canvas.closePathStroke();

        canvas.setLineWidth(0.0009f);

        for (BookLink bookLink : bookLinks) {

            if (bookLink != null && bookLinks.size() > 0) {

                int llx = bookLink.getRect().x;
                int lly = bookLink.getRect().y;
                int urx = llx + bookLink.getRect().width;
                int ury = lly + bookLink.getRect().height;

                try {
                    String param = bookLink.getText();

                    if (StringCheck.containsChinese(bookLink.getText()) || StringCheck.containsKorean(bookLink.getText()))
                        param = HexStringConverter.getHexStringConverterInstance().stringToHex(bookLink.getText());

                    canvas.setAction(new PdfAction(bookLink.getPath() + param), llx, lly, urx, ury); // add the link

                    showTextOverRect(canvas, bookLink.getRect(), bookLink.getText());
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }

        canvas.restoreState();

    }

    public static final float IMAGE_SCALE_FACTOR = 0.06f; // 72/1200;

    private void clippingLinkedArea(PdfContentByte canvas, List<BookLink> bookLinks, final String RESOURCE, com.itextpdf.text.Rectangle cropBox)
            throws DocumentException, IOException {

        if (bookLinks.size() > 0) {
            canvas.saveState();
            canvas.setLineWidth(0.09f);

            for (BookLink bookLink : bookLinks) {
                java.awt.Rectangle rect = bookLink.getRect();
                canvas.rectangle(rect.x - 1, rect.y + 2.1f, rect.getWidth() + 5, rect.getHeight() - 1.5f);
            }

            canvas.clip();
            canvas.newPath();

            Image coordImage = Image.getInstance(RESOURCE);
            if (coordImage.isMaskCandidate())
                coordImage.makeMask();

            coordImage.scalePercent(6 ,6); // IMAGE_SCALE_FACTOR * 100.0f, IMAGE_SCALE_FACTOR * 100.0f);

            float yOffset = coordImage.getScaledHeight() - cropBox.getHeight();
            if (yOffset < 0.0f) {
                yOffset = 0.0f;
            }

            coordImage.setAbsolutePosition(cropBox.getLeft(), cropBox.getBottom()-yOffset);

            canvas.addImage(coordImage);

            canvas.restoreState();
        }
    }

    public void printPageNum(int numPage) throws IOException {
        txtWriter.printf("Number of pages: %s\n", numPage);
    }

    public void showTextOverRect(PdfContentByte canvas, java.awt.Rectangle rect, String text) throws DocumentException {

        canvas.saveState();

            //배경은 WHITE
            canvas.setColorFill(BaseColor.WHITE);
            canvas.rectangle(rect.x - 1, rect.y + 2.5f, rect.getWidth() + 5, rect.getHeight() - 2.2f);
            canvas.fill();

            canvas.beginText();

                // 글자 색은 non-K
                canvas.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_FILL);
                canvas.setCMYKColorFill(0xA0, 0xB0, 0xC0, 0x00);
                try {
                    canvas.setFontAndSize(BaseFont.createFont(), 8);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                canvas.setTextMatrix(rect.x, rect.y + 3.5f);
                canvas.showText(text);

            canvas.endText();

        canvas.restoreState();
    }
}