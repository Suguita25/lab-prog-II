// repository/CollectionFolderRepository.java
package com.exemplo.auth.repository;
import com.exemplo.auth.model.CollectionFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CollectionFolderRepository extends JpaRepository<CollectionFolder, Long> {
    List<CollectionFolder> findByUserId(Long userId);
    Optional<CollectionFolder> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndName(Long userId, String name);
    Optional<CollectionFolder> findByUserIdAndName(Long userId, String name);
}
