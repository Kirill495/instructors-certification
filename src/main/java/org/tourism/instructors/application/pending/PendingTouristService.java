package org.tourism.instructors.application.pending;

import org.tourism.instructors.domain.pending.ConversationState;
import org.tourism.instructors.domain.pending.PendingTourist;

import java.util.List;

public interface PendingTouristService {
    void register(ConversationState conversation);
    PendingTourist getById(int id);
    List<PendingTourist> findAllPending();
    PendingTourist linkTourist(int pendingId, int touristId);
    PendingTourist approve(int id);
    PendingTourist reject(int id);
    boolean existsByChatId(long chatId);
    int countPending();
}