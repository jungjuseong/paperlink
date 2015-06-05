package com.paperlink.util;


import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class SaxParser extends DefaultHandler {
    public static final String XMLFILE = "src/main/resources/scripts/hackers-toeic-listening-answers.xml";
    protected StringBuffer buf = new StringBuffer();
    protected PrintWriter printWriter;

    /**
     * @see ContentHandler#startElement(String, String, String, Attributes)
     */
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //System.out.printf("[start] %s\n", qName);
        if ("answer".equals(qName)) {
        }

    }

    /**
     * @see org.xml.sax.ContentHandler#endElement(String, String, String)
     */
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("id".equals(qName)) {
            System.out.printf("%s\n", formatID(buf.toString(), "answer-id"));

            // file open
        }
        else if ("answer_list".equals(qName)) {
            System.out.printf("%s\n", formatList(buf.toString(), "answer_list"));
        }
        else if ("description".equals(qName)) {
            System.out.printf("%s\n", formatList(buf.toString(), "description"));
        }
        else if ("examples".equals(qName)) {
            System.out.printf("%s\n", formatList(buf.toString(), "examples"));
        }
        else if ("title".equals(qName)) {
            System.out.printf("%s\n", formatList(buf.toString(), "title"));
        }
        else if ("answer".equals(qName)) {
            System.out.println("</div>\n");
        }

        buf = new StringBuffer();
    }

    private String formatID(String id, String class_name) {
        return "<div id=\"" + id.trim() + "\" class=\"" + class_name + "\">\n";
    }

    private String formatList(String answer_list, String class_name) {

        return "\t<div class=\"" + class_name + "\">" + "\n" + formatLines(answer_list) + "\t</div>";
    }

    private String formatLines(String sb) {

        String subs[] = sb.split("\n");

        StringBuffer sss = new StringBuffer();

        for (String s : subs) {
            if (s.trim().length() > 0)
                sss.append("\t  ").append(s.trim()).append("<br>\n");
        }

        return sss.toString();
    }
    /**
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        buf.append(ch, start, length);
    }

    /**
     * Replaces all the Newline characters by a space.
     *
     * @param buf the origianl StringBuffer
     * @return a String without newlines
     *
     */
    protected String strip_newline(StringBuffer buf) {
        int pos;

        while ((pos = buf.indexOf("\n")) != -1)
            buf.replace(pos, pos+1, " ");

        while (buf.charAt(0) == ' ')
            buf.deleteCharAt(0);

        return buf.toString();
    }

    /**
     * Create the handler to read the Toeic Answer
     */
    public SaxParser(InputSource is) throws SAXException, FactoryConfigurationError,
            ParserConfigurationException, IOException {

        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

        parser.parse(is, this);
    }


    public static void main(String[] args) {
        try {
            new SaxParser(new InputSource(new FileInputStream(XMLFILE)));
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
