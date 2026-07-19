package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.dto.projection.StorageUsageResponse;
import swp391.aistudyhub.entity.CloudStorage;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CloudStorageRepository extends JpaRepository<CloudStorage, UUID> {

    Optional<CloudStorage> findByUser_Id(UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE CloudStorage c SET c.usedQuota = GREATEST(0, c.usedQuota - :fileSize) WHERE c.user.id = :userId")
    void minusUsedQuota(
            @Param("userId") UUID userId,
            @Param("fileSize") long fileSize
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE CloudStorage c SET c.usedQuota = c.usedQuota + :fileSize WHERE c.user.id = :userId")
    void plusUsedQuota(
            @Param("userId") UUID userId,
            @Param("fileSize") long fileSize
    );

    @Query("SELECT c FROM CloudStorage c")
    Page<StorageUsageResponse> findBy(Pageable pageable);
}