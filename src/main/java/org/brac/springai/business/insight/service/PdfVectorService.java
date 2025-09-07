package org.brac.springai.business.insight.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class PdfVectorService {

    private final VectorStore vectorStore;
    private static final int CHUNK_SIZE = 500; // words per chunk

    public PdfVectorService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<String> upsertPdf(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF file cannot be empty");
        }

        // Save temp file
        File tempFile = File.createTempFile("upload-", ".pdf");
        file.transferTo(tempFile);

        // Extract text
        String text = extractTextFromPdf(tempFile);
        tempFile.delete();

        // Chunk the text
        List<String> chunks = chunkText(text, CHUNK_SIZE);

        // Create documents and upsert
        List<String> docIds = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Document doc = Document.builder()
                    .text(chunks.get(i))
                    .metadata(Map.of(
                            "source", file.getOriginalFilename(),
                            "type", "pdf",
                            "chunk_index", i
                    ))
                    .build();
            vectorStore.add(List.of(doc));
            docIds.add(doc.getId());
        }

        return docIds;
    }

    private String extractTextFromPdf(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private List<String> chunkText(String text, int wordsPerChunk) {
        List<String> chunks = new ArrayList<>();
        if (!StringUtils.hasText(text)) return chunks;

        String[] words = text.split("\\s+");
        int totalWords = words.length;

        for (int i = 0; i < totalWords; i += wordsPerChunk) {
            int end = Math.min(i + wordsPerChunk, totalWords);
            String chunk = String.join(" ", Arrays.copyOfRange(words, i, end));
            chunks.add(chunk);
        }

        return chunks;
    }
}
