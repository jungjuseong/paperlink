package com.paperlink.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.FileOutputStream;
import java.io.IOException;

public class TestSheet {

    public static final float SCALE_FACTOR = 6f; // 72/1200;

    public void drawBoxes(PdfContentByte canvas, BaseColor color) throws DocumentException, IOException {
        canvas.saveState();
        //canvas.setColorFill(color);
        canvas.setColorStroke(color);
        canvas.setLineWidth(0.001f);

        canvas.setFontAndSize(BaseFont.createFont("c:/windows/fonts/times.ttf", BaseFont.WINANSI, BaseFont.EMBEDDED), 6); // /F1 12 Tf

        float xoffset = 7.2f*1;
        float yoffset = 7.2f*1;

        float box_size = 7.2f*1;

        for (float x = 0f; x < PageSize.A4.getWidth(); x += xoffset) {

            for (float y = 0; y < PageSize.A4.getHeight(); y += yoffset) {
               canvas.rectangle(x, y, box_size, box_size);
            }
        }
        canvas.stroke();
        canvas.restoreState();
    }

    public void showPosition(PdfContentByte canvas, float x, float y)  {
                canvas.beginText();
                canvas.moveText(x, y);

                canvas.showText("" + (int)x + "," + (int)y);
                canvas.endText();
    }

    public void colorRectangle(PdfContentByte canvas, BaseColor color, float x, float y, float width, float height) {
        canvas.saveState();
        canvas.setColorFill(color);
        canvas.setLineWidth(0.01f);

        canvas.rectangle(x, y, width, height);
        canvas.fillStroke();
        canvas.restoreState();
    }

    static private float MARGIN_LEFT = 36f;
    static private float MARGIN_RIGHT = 72f;
    static private float MARGIN_TOP = 72f;
    static private float MARGIN_BOTTOM = 72f;

    public void drawTestSheet(String filename) throws DocumentException, IOException {

        // step 1
        Document document = new Document(PageSize.A4, MARGIN_LEFT,MARGIN_RIGHT,MARGIN_TOP,MARGIN_BOTTOM); // 72f´Â 1ÀÎÄ¡
        //document.setMarginMirroring(true); // spine of the book is to the left
        // step 2
        PdfWriter pdfWriter = PdfWriter.getInstance(document, new FileOutputStream(filename)); // PdfWriter is the class responsible for writing the PDF
        pdfWriter.setPdfVersion(PdfWriter.VERSION_1_7);
        // step 3
        document.open();
        // step 4
        PdfContentByte canvas = pdfWriter.getDirectContent(); // getDirectContentUnder();
        pdfWriter.setCompressionLevel(0);

        // draw coordinate-image
        canvas.saveState();
        canvas.setLineWidth(0.03f);

        Image coordImage = Image.getInstance(COORD_IMAGE_FILE);
        printImageInfo(coordImage);

        coordImage.scalePercent(SCALE_FACTOR, SCALE_FACTOR);
        coordImage.setAbsolutePosition((PageSize.A4.getWidth() - coordImage.getScaledWidth()) / 2,
                (PageSize.A4.getHeight() - coordImage.getScaledHeight()) / 2);

        canvas.addImage(coordImage);
        canvas.stroke();
        canvas.restoreState();

        // draw margin rect
        //Rectangle pageRect = pdfWriter.getPageSize();
        //Rectangle r = addMargin(pdfWriter.getPageSize(), MARGIN_LEFT, MARGIN_RIGHT, MARGIN_TOP, MARGIN_BOTTOM);
        //colorRectangle(canvas, new CMYKColor(0f,1f,0f,0.5f), r.getLeft(), r.getRight(), r.getWidth(), r.getHeight());

        drawBoxes(canvas, new CMYKColor(0f,.6f,.5f,0.5f));
        // writes something to the direct content using a convenience method
        Phrase hello = new Phrase("Test Sheet 01");
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, hello, MARGIN_LEFT + 10, 788, 0);

        // step 5
        document.close();
    }

    private Rectangle addMargin(Rectangle rect, float ml, float mr, float mt, float mb) {
        return new Rectangle(rect.getLeft(ml), rect.getBottom(mb), rect.getRight(mr), rect.getTop(mt));
    }

    public void printRect(Rectangle rect) {
        System.out.printf("Rect@%s [%.0f,%.0f,%.0f,%.0f]\n", rect.hashCode(),
                rect.getLeft(),
                rect.getBottom(),
                rect.getRight(),
                rect.getTop());
    }

    public void printImageInfo(Image im) {
        System.out.printf("%.0f X %.0f pixels, %d dpi, %d ColorSpace, %d bpc ",
                im.getWidth(), im.getHeight(), im.getDpiX(), im.getColorspace(), im.getBpc());
    }


    public static final String PDF = "results/test_sheet1.pdf";
    public static final String COORD_IMAGE_FILE = "src/main/resources/page_images/page001.tif";

    public static void main(String[] args) throws DocumentException, IOException {
        TestSheet example = new TestSheet();
        example.drawTestSheet(PDF);
    }
}

