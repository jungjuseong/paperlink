package com.paperlink;

import com.itextpdf.text.DocumentException;
import com.paperlink.service.LinkBuilder;
import net.sf.extjwnl.JWNLException;
import org.springframework.context.support.GenericXmlApplicationContext;

import java.io.IOException;

public class build {

    /**
     * Main method.
     * @param    args    no arguments needed
     * @throws DocumentException
     * @throws IOException
     */
    public static void main(String[] args)
            throws DocumentException, IOException, JWNLException {

        String jobName = args[0];

        if (args.length == 0) {
            System.out.printf("Usage: build jobname (ex: build hackers_toeic_listening)\n");
            return;
        }

        GenericXmlApplicationContext context = new GenericXmlApplicationContext();
        context.getEnvironment().setActiveProfiles(jobName);
        context.load("classpath:*-config.xml");
        context.refresh();

        LinkBuilder bd = context.getBean("linkBuilder",com.paperlink.service.LinkBuilder.class);

        if (context != null)
            bd.scanDocument(args[0]);
    }
}
