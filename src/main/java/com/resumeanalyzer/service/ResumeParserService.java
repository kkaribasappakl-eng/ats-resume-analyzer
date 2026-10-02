package com.resumeanalyzer.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ResumeParserService {

    public String parseFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("Invalid file name");
        }

        byte[] bytes = file.getBytes();
        String lowerCaseName = fileName.toLowerCase();

        if (lowerCaseName.endsWith(".pdf")) {
            // Priority 1: PDFBox Text and Hyperlinks
            try {
                String text = parsePdfWithPDFBox(bytes);
                if (text != null && !text.strip().isEmpty()) {
                    return text;
                }
            } catch (Exception e) {
                // fall back
            }

            // Priority 2: Apache Tika AutoDetect
            try {
                String text = parseWithTika(bytes);
                if (text != null && !text.strip().isEmpty()) {
                    return text;
                }
            } catch (Exception e) {
                // fall back
            }

            // Priority 3: Metadata Fallback representation
            return parseMetadataFallback(bytes);
        } else if (lowerCaseName.endsWith(".docx")) {
            // DOCX Parsing
            try {
                return parseDocxWithPOI(bytes);
            } catch (Exception e) {
                try {
                    return parseWithTika(bytes);
                } catch (Exception ex) {
                    throw new IOException("Failed to parse Word document with both POI and Tika", ex);
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported file type. Only PDF and DOCX files are allowed.");
        }
    }

    private String parsePdfWithPDFBox(byte[] bytes) throws IOException {
        try (PDDocument document = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Extract page annotation hyperlinks
            StringBuilder links = new StringBuilder();
            for (PDPage page : document.getPages()) {
                try {
                    for (PDAnnotation annot : page.getAnnotations()) {
                        if (annot instanceof PDAnnotationLink) {
                            PDAnnotationLink link = (PDAnnotationLink) annot;
                            if (link.getAction() instanceof PDActionURI) {
                                PDActionURI uri = (PDActionURI) link.getAction();
                                links.append(" Hyperlink: ").append(uri.getURI()).append(" ");
                            }
                        }
                    }
                } catch (Exception ex) {
                    // ignore link issues
                }
            }
            return text + "\n" + links.toString();
        }
    }

    private String parseDocxWithPOI(byte[] bytes) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes);
             XWPFDocument document = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            
            String text = extractor.getText();
            
            // Extract document hyperlinks
            StringBuilder links = new StringBuilder();
            try {
                for (XWPFParagraph p : document.getParagraphs()) {
                    for (XWPFRun run : p.getRuns()) {
                        if (run instanceof XWPFHyperlinkRun) {
                            XWPFHyperlinkRun linkRun = (XWPFHyperlinkRun) run;
                            links.append(" Hyperlink: ").append(linkRun.getHyperlink(document).getURL()).append(" ");
                        }
                    }
                }
            } catch (Exception e) {
                // ignore
            }
            return text + "\n" + links.toString();
        }
    }

    private String parseWithTika(byte[] bytes) throws Exception {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            parser.parse(is, handler, metadata, context);
            return handler.toString();
        }
    }

    private String parseMetadataFallback(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            parser.parse(is, handler, metadata, context);
            for (String name : metadata.names()) {
                sb.append(name).append(": ").append(metadata.get(name)).append("\n");
            }
        } catch (Exception e) {
            // ignore
        }
        return sb.toString();
    }
}
