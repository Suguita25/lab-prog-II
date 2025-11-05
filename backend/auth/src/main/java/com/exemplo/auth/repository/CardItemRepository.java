// repository/CardItemRepository.java
package com.exemplo.auth.repository;
import com.exemplo.auth.model.CardItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CardItemRepository extends JpaRepository<CardItem, Long> {
    List<CardItem> findByFolderIdAndUserId(Long folderId, Long userId);
    long countByFolderIdAndUserId(Long folderId, Long userId);
    void deleteByFolderIdAndUserId(Long folderId, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
    boolean existsByIdAndUserId(Long id, Long userId);
}
