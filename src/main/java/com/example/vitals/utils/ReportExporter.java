package com.example.vitals.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class ReportExporter {

    /**
     * Exports the given system stats to a CSV file.
     * @param stats A map with keys as parameter names and values as their corresponding string representations.
     * @param file  The destination CSV file.
     */
    public static void exportToCSV(Map<String, String> stats, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            // Write header row
            writer.append("Parameter,Value\n");
            // Write each key/value pair as a new row
            for (Map.Entry<String, String> entry : stats.entrySet()) {
                writer.append(entry.getKey())
                        .append(",")
                        .append(entry.getValue())
                        .append("\n");
            }
            writer.flush();
        }
    }

    /**
     * Exports the given system stats to a PDF file.
     * @param stats A map with keys as parameter names and values as their corresponding string representations.
     * @param file  The destination PDF file.
     */
    public static void exportToPDF(Map<String, String> stats, File file) throws IOException {
        // Create a new PDF document
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            // Start writing content
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Write the report title
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("System Vitals Report");
                contentStream.endText();

                // Write each statistic under the title
                contentStream.setFont(PDType1Font.HELVETICA, 14);
                float yPosition = 700;
                for (Map.Entry<String, String> entry : stats.entrySet()) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, yPosition);
                    contentStream.showText(entry.getKey() + ": " + entry.getValue());
                    contentStream.endText();
                    yPosition -= 20;
                }
            }
            document.save(file);
        }
    }
}
