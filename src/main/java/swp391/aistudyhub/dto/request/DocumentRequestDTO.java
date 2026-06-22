package swp391.aistudyhub.dto.request;

import lombok.Data;

@Data
public class DocumentRequestDTO {
    private String documentName;
    private String fileType;
    private String previewUrl;
    private String downloadUrl;
    private Long fileSize;
    private String description;

    // ĐÃ BỔ SUNG: Chuỗi văn bản thô trích xuất từ file do Frontend gửi lên để xử lý RAG Chunking
    private String textContent;
}