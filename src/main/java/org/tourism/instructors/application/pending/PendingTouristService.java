package org.tourism.instructors.application.pending;

import org.tourism.instructors.domain.pending.ConversationState;
import org.tourism.instructors.domain.pending.PendingTourist;

import java.util.List;

public interface PendingTouristService {
    void register(ConversationState conversation);
    List<PendingTourist> findAllPending();
    PendingTourist approve(int id);
    PendingTourist reject(int id);

    boolean existsByChatId (long chatId);
}