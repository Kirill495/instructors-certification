package org.tourism.instructors.api.bot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.domain.pending.ConversationState;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRegistrationHandlerTest {

    @Mock
    CatalogService catalogService;
    @Mock
    PendingTouristService pendingTouristService;
    @Mock
    ConversationStateRegistry conversationRegistry;
    @Mock
    BotExecutor botExecutor;
    @Mock
    Message message;

    @InjectMocks
    ChatRegistrationHandler chatRegistrationHandler;

    @Test
    void handleStep() {
        long chatId = 1L;
        ConversationState state = new ConversationState();
        state.setStep(TouristRegistrationBot.Step.NAME);
        when(conversationRegistry.get(chatId)).thenReturn(state);
        when(botExecutor.dispatch(any())).thenReturn(message);
        when(message.getMessageId()).thenReturn(1);
        chatRegistrationHandler.handleStep(botExecutor, chatId, "Andrew Fedorov", 1);

        assertEquals(TouristRegistrationBot.Step.GENDER, state.getStep());
    }
}