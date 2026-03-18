package org.tourism.instructors.domain.pending.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.pending.PendingTourist;

import java.util.List;

@Repository
public interface PendingTouristRepository extends JpaRepository<PendingTourist, Integer> {
    List<PendingTourist> findByStatusOrderByCreatedAtDesc(String status);

    boolean existsByChatId(Long chatId);

    int countByStatus(String status);
}