package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swp391.aistudyhub.entity.CloudStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CloudStorageRepository extends JpaRepository<CloudStorage, UUID> {
    Optional<CloudStorage> findByUser_Id(UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE CloudStorage c SET c.usedQuota = GREATEST(0, c.usedQuota - :fileSize) WHERE c.user.id = :userId")
    void minusUsedQuota(@Param("userId") java.util.UUID userId, @Param("fileSize") long fileSize);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE CloudStorage c SET c.usedQuota = c.usedQuota + :fileSize WHERE c.user.id = :userId")
    void plusUsedQuota(@org.springframework.data.repository.query.Param("userId") java.util.UUID userId, @org.springframework.data.repository.query.Param("fileSize") long fileSize);
}