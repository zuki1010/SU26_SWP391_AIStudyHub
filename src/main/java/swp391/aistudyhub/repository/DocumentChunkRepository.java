package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.entity.DocumentChunk;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    /**
     * Native Query xử lý ép kiểu mảng chuỗi sang vector(1536) mượt mà không lỗi Driver JDBC.
     * Đã cập nhật lại lệnh cast sang kiểu vector để khớp với cấu trúc lưu trữ của Supabase AI.
     */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO document_chunks (chunk_id, document_id, chunk_content, vector_embedding, page_number) " +
            "VALUES (gen_random_uuid(), :documentId, :content, cast(:embedding as vector), :pageNumber)",
            nativeQuery = true)
    void insertChunkWithVector(
            @Param("documentId") UUID documentId,
            @Param("content") String content,
            @Param("embedding") String embeddingString, // Chuỗi số thực nhận từ OpenAI: "[0.012, -0.045, ...]"
            @Param("pageNumber") Integer pageNumber
    );

    // Truy vấn tìm kiếm các chunk thuộc về một tài liệu cụ thể phục vụ tính năng RAG/Chat sau này
    List<DocumentChunk> findByDocumentId(UUID documentId);
}