// repository/CardItemRepository.java
package com.exemplo.auth.repository;
import com.exemplo.auth.model.CardItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CardItemRepository extends JpaRepository<CardItem, Long> {
    List<CardItem> findByFolderIdAndUserId(Long folderId, Long userId);
}
