package com.paperlink;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.parser.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class ExtractLink {
    public static final String PDF = "results/Hackers_Toeic_2013_Listening_result.pdf";
    public static final String RESULT = "results/Hackers_Toeic_2013_Listening_result_rect.pdf";
    public static final String TEXT = "results/Hackers_Toeic_2013_Listening_result.txt";
    public static final String COORD_IMAGE = "resources/page_images/page001.tif";

    public static final float IMAGE_SCALE_FACTOR = 0.06f; // 72/1200;
    static Image coordImage = null;

    public void extractLink(String src, String destTxt, String destPdf) throws DocumentException, IOException {
        PrintWriter out = new PrintWriter(new FileOutputStream(destTxt));
        PdfReader reader = new PdfReader(src);
        // show anchors
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(RESULT));

        for (int currentPage = 1; currentPage <= reader.getNumberOfPages(); currentPage++) {

            PdfDictionary action;
            PdfDictionary pageDict = reader.getPageN(currentPage);      // Gets the dictionary that represents a page
            PdfArray annotationDict = pageDict.getAsArray(PdfName.ANNOTS);
            Rectangle mediaBox = reader.getPageSize(currentPage);

            if (currentPage == 1) {
                printMediaBox(mediaBox);
            }

            PdfContentByte contentByte = stamper.getUnderContent(currentPage);
            contentByte.saveState();
            contentByte.setLineWidth(0.01f);

            if (currentPage == 1) {
                coordImage = Image.getInstance(COORD_IMAGE);
                printImageInfo(coordImage);

                coordImage.scalePercent(IMAGE_SCALE_FACTOR * 100.0f, IMAGE_SCALE_FACTOR * 100.0f);
                float yOffset = coordImage.getWidth()*(IMAGE_SCALE_FACTOR) - mediaBox.getHeight();

                if (yOffset < 0.0f) {
                    yOffset = 0.0f;
                }
                coordImage.setAbsolutePosition(0.0f, -yOffset);
            }
            if (annotationDict == null)
                continue; // goto next page

            for (int i = 0; i < annotationDict.size(); i++) {
                PdfDictionary annotation = annotationDict.getAsDict(i);
                if (PdfName.LINK.equals(annotation.getAsName(PdfName.SUBTYPE))) {
                    action = annotation.getAsDict(PdfName.A);
                    if (action == null)
                        continue;
                    if (action.getAsName(PdfName.S).equals(PdfName.URI)) {
                        PdfString uri = action.getAsString(PdfName.URI);
                        PdfArray rect = annotation.getAsArray(PdfName.RECT);

                        float x = rect.getAsNumber(0).floatValue();
                        float y = rect.getAsNumber(1).floatValue();
                        float w = rect.getAsNumber(2).floatValue() - x;
                        float h = rect.getAsNumber(3).floatValue() - y;

                        float yMargin = h * .2f;
                        y -= yMargin; //  lower than 20% of the height
                        h = yMargin * 2; // h += yMargin;

                        contentByte.rectangle(x, y, w, h);

                        out.printf("[%03d-%04d],[%X,%X,%X,%X],[%s]",
                             currentPage, i,
                             (int) (x * 32), (int) ((mediaBox.getHeight()-y) * 32), (int) w*32, (int) h*32,
                             uri.toString());

                        String linkedText = getTextFromRectangle(rect, reader, currentPage);

                        if (linkedText != null) {
                            out.printf("[%s]\n", linkedText);
                        }
                    }
                    /*
                    PdfArray d = annotation.getAsArray(PdfName.DEST);
                    if (d != null && d.size() == 5 && PdfName.XYZ.equals(d.getAsName(1)))
                    */
                }

            }
            if (currentPage == 1) {
                contentByte.clip();
                contentByte.newPath();
                contentByte.addImage(coordImage);
            }

            contentByte.restoreState();
        } /* page */

        stamper.close();
        out.flush();
        out.close();
        reader.close();
    }


    public void printMediaBox(Rectangle mediaBox) {
        System.out.printf("Media Box [%.0f,%.0f,%.0f,%.0f]\n",
                mediaBox.getLeft(),
                mediaBox.getBottom(),
                mediaBox.getRight(),
                mediaBox.getTop());
    }

    public void printImageInfo(Image image) {
        System.out.printf("Image Size: [%.0f, %.0f] Unit\n", image.getWidth()*(IMAGE_SCALE_FACTOR),image.getHeight()*(IMAGE_SCALE_FACTOR));
        System.out.printf("DPI: %d, %d\n", image.getDpiX(),image.getDpiX());
        System.out.printf("Alignment: %d\n", image.getAlignment());
        System.out.printf("Color Space: %d\n", image.getColorspace());
        System.out.printf("Bits Per Component: %d\n", image.getBpc());
    }

    public String getTextFromRectangle(PdfArray rectangle, PdfReader reader, int pageNumber) throws IOException {
        if (rectangle == null) {
            return null;
        }
        // Get the retangle coodinates
        float llx = rectangle.getAsNumber(0).floatValue();
        float lly = rectangle.getAsNumber(1).floatValue();
        float urx = rectangle.getAsNumber(2).floatValue();
        float ury = rectangle.getAsNumber(3).floatValue();

        Rectangle rect = new Rectangle(llx, lly, urx, ury);
        RenderFilter filter = new RegionTextRenderFilter(rect);
        TextExtractionStrategy strategy = new FilteredTextRenderListener(new LocationTextExtractionStrategy(), filter);
        return PdfTextExtractor.getTextFromPage(reader, pageNumber, strategy);
    }


    public static void main(String[] args) throws DocumentException, IOException {
        ExtractLink example = new ExtractLink();
        example.extractLink(PDF, TEXT, RESULT);
    }
}
