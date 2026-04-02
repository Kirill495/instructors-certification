package org.tourism.instructors.api.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismDTO;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.domain.pending.ConversationState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    SendMessage sendMessage;

    @InjectMocks
    ChatRegistrationHandler chatRegistrationHandler;

    int messageId = 1;
    long chatId = 1L;
    ConversationState state = new ConversationState();
    String messageTextWithName = "Andrew Fedorov";

    @BeforeEach
    void setUp() {
        when(conversationRegistry.get(chatId)).thenReturn(state);

    }

    @Test
    void handleNameStep() {

        state.setStep(TouristRegistrationBot.Step.NAME);
        when(botExecutor.dispatch(any())).thenReturn(any(SendMessage.class));
        when(message.getMessageId()).thenReturn(messageId);
        chatRegistrationHandler.handleStep(botExecutor, chatId, "Andrew Fedorov", 1);

        assertEquals(TouristRegistrationBot.Step.GENDER, state.getStep());
    }

    @Test
    void handleNameStepEditing() {
        KindOfTourismDTO kindOfTourism = new KindOfTourismDTO();
        state.setStep(TouristRegistrationBot.Step.NAME);
        GradeDTO grade = new GradeDTO();
        state.setGrade(grade);
        state.setEditing(true);
        state.setKindOfTourism(kindOfTourism);
        when(botExecutor.dispatch(any())).thenReturn(any(SendMessage.class));
        chatRegistrationHandler.handleStep(botExecutor, chatId, messageTextWithName, messageId);

        assertFalse(state.isEditing());
        assertEquals(TouristRegistrationBot.Step.CHECK_INPUT, state.getStep());
        ArgumentCaptor<EditMessageText> argumentCaptor = ArgumentCaptor.forClass(EditMessageText.class);
        verify(botExecutor).dispatch(argumentCaptor.capture());
        assertEquals(chatId, Long.parseLong(argumentCaptor.getValue().getChatId()));
    }

    @Test
    void handleDateOfBirth() {
        state.setStep(TouristRegistrationBot.Step.DATE_OF_BIRTH);

        chatRegistrationHandler.handleStep(botExecutor, chatId, messageTextWithName, messageId);

        assertEquals(TouristRegistrationBot.Step.DATE_OF_BIRTH, state.getStep());

        verify(botExecutor).send(eq(chatId), any(String.class));
    }
}