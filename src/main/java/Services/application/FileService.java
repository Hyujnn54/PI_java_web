package Services.application;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class FileService {
    private static final String UPLOAD_DIR = "uploads/applications/";

    public FileService() {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            System.err.println("Error creating upload directory: " + e.getMessage());
        }
    }

    /**
     * Upload PDF file and return the file path
     */
    public String uploadPDF(File sourceFile) throws IOException {
        if (!sourceFile.exists()) {
            throw new FileNotFoundException("Source file does not exist: " + sourceFile.getAbsolutePath());
        }

        if (!sourceFile.getName().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("File must be a PDF file (*.pdf)");
        }

        // Generate unique filename
        String fileName = UUID.randomUUID() + "_" + sourceFile.getName();
        Path targetPath = Paths.get(UPLOAD_DIR, fileName);

        // Copy file to upload directory
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("PDF uploaded successfully: " + targetPath.toString());
        return targetPath.toString();
    }

    /**
     * Get PDF file for download
     */
    public File downloadPDF(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("PDF file not found: " + filePath);
        }
        return file;
    }

    /**
     * Delete PDF file
     */
    public void deletePDF(String filePath) throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
        System.out.println("PDF deleted: " + filePath);
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Extract text from PDF file
     */
    public String extractTextFromPDF(String filePath) throws IOException {
        File pdfFile = new File(filePath);
        if (!pdfFile.exists()) {
            throw new FileNotFoundException("PDF file not found: " + filePath);
        }

        if (!filePath.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("File must be a PDF file (*.pdf)");
        }

        try (InputStream input = new FileInputStream(pdfFile);
             PDDocument document = PDDocument.load(input)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            System.out.println("Text extracted from PDF successfully");
            return text;
        } catch (IOException e) {
            System.err.println("Error extracting text from PDF: " + e.getMessage());
            throw e;
        }
    }
}

