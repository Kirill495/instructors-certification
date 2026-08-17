package org.tourism.instructors.api.bot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.tourism.instructors.domain.pending.ConversationState;

@Component
public class ConversationStateRegistry {

    private final Map<Long, ConversationState> activeConversations = new ConcurrentHashMap<>();

    public ConversationState get(Long chatId) {
        return activeConversations.get(chatId);
    }

    public void put(long chatId, ConversationState state) {
        activeConversations.put(chatId, state);
    }

    public boolean hasActive(Long chatId) {
        return activeConversations.containsKey(chatId);
    }
}
