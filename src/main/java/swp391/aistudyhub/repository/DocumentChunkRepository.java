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

    @Modifying
    @Transactional
    // Đã tối ưu câu lệnh Native SQL chuẩn hóa cho extension pgvector trên Supabase
    @Query(value = "INSERT INTO document_chunks (chunk_id, document_id, chunk_content, vector_embedding, page_number) " +
            "VALUES (gen_random_uuid(), :documentId, :content, cast(:embedding as vector), :pageNumber)",
            nativeQuery = true)
    void insertChunkWithVector(
            @Param("documentId") UUID documentId,
            @Param("content") String content,
            @Param("embedding") String embeddingString, // Ví dụ: "[0.012, -0.045, ...]"
            @Param("pageNumber") Integer pageNumber
    );

    // ĐÃ SỬA: Thêm dấu _ để Spring Data JPA tự hiểu cấu trúc liên kết sang bảng Document
    List<DocumentChunk> findByDocument_Id(UUID documentId);

    // Thay thế hàm cũ bằng câu lệnh @Query tường minh để ép Postgres chạy lệnh DELETE thuần túy
    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentChunk dc WHERE dc.document.id = :documentId")
    void deleteByDocument_Id(@Param("documentId") UUID documentId);

    @Query(value = "SELECT * FROM document_chunks dc " +
            "WHERE dc.document_id IN (:documentIds) " +
            "ORDER BY dc.vector_embedding <=> cast(:queryVector as vector) " +
            "LIMIT :limitCount",
            nativeQuery = true)
    List<String> findRelevantChunks(
            @Param("documentIds") List<UUID> documentIds,
            @Param("queryVector") String queryVector,
            @Param("limitCount") int limitCount
    );
}