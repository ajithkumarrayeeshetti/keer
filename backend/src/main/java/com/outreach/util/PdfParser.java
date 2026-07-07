package com.outreach.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@Slf4j
public class PdfParser {

    /**
     * Extract all text from a PDF file.
     * Uses PDFBox 3.x API (Loader.loadPDF instead of the deprecated PDDocument.load).
     */
    public String extractText(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            throw new IOException("PDF file not found or not readable: " + filePath);
        }

        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.isEncrypted()) {
                throw new IOException("Cannot extract text from an encrypted PDF. Please provide an unencrypted resume.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new IOException("No text could be extracted — the PDF may be a scanned image without OCR.");
            }
            return text;
        }
    }
}
