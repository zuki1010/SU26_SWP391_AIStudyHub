package swp391.aistudyhub.service;

import swp391.aistudyhub.entity.Document;
import java.util.UUID;

public interface DocumentChunkService {

    void chunkAndEmbedDocument(Document document, String fullTextContent);

    String getVectorStringForQuery(String text);
}