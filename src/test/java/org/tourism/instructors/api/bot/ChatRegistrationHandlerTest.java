package org.tourism.instructors.api.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.tourism.instructors.api.bot.exception.UnknownTelegramOptionException;
import org.tourism.instructors.api.bot.keyboards.CalendarKeyboard;
import org.tourism.instructors.api.bot.keyboards.MonthKeyboard;
import org.tourism.instructors.api.bot.keyboards.YearKeyboard;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismDTO;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.domain.pending.ConversationState;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

        state.setKindOfTourism(new KindOfTourismDTO(1, "cycling", false));
        state.setGrade(new GradeDTO(1, "instructor", true, 10));
    }

    @Nested
    class HandleStepTest {
        @BeforeEach
        void setUp() {
            when(conversationRegistry.get(chatId)).thenReturn(state);
        }

        @Test
        void handleNameStep() {

            state.setStep(TouristRegistrationBot.Step.NAME);
            when(botExecutor.dispatch(any(SendMessage.class))).thenReturn(message);
            when(message.getMessageId()).thenReturn(messageId);

            chatRegistrationHandler.handleStep(botExecutor, chatId, "Andrew Fedorov", 1);

            assertEquals(TouristRegistrationBot.Step.GENDER, state.getStep());
        }

        @Test
        void handleNameStepEditing() {
            KindOfTourismDTO kindOfTourism = new KindOfTourismDTO();
            state.setStep(TouristRegistrationBot.Step.NAME);
            GradeDTO grade = new GradeDTO(1, "grade", false, 10);
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

        @Test
        void handlePhone() {
            state.setStep(TouristRegistrationBot.Step.PHONE);

            chatRegistrationHandler.handleStep(botExecutor, chatId, messageTextWithName, messageId);

            assertEquals(TouristRegistrationBot.Step.EMAIL, state.getStep());

            verify(botExecutor).send(eq(chatId), any(String.class));
        }

        @Test
        void handleEmail() {
            state.setStep(TouristRegistrationBot.Step.EMAIL);

            chatRegistrationHandler.handleStep(botExecutor, chatId, messageTextWithName, messageId);

            assertEquals(TouristRegistrationBot.Step.KIND_OF_TOURISM, state.getStep());

            verify(botExecutor).send(eq(chatId), any(String.class));
        }

        @Test
        void handleKindOfTourism() {
            state.setStep(TouristRegistrationBot.Step.KIND_OF_TOURISM);
            when(botExecutor.dispatch(any(SendMessage.class))).thenReturn(message);
            when(message.getMessageId()).thenReturn(messageId);

            chatRegistrationHandler.handleStep(botExecutor, chatId, messageTextWithName, messageId);

            assertEquals(TouristRegistrationBot.Step.GRADE, state.getStep());
        }

        @Test
        void handleGrade() {
            state.setStep(TouristRegistrationBot.Step.GRADE);
            when(botExecutor.dispatch(any(SendMessage.class))).thenReturn(message);
            when(message.getMessageId()).thenReturn(messageId);

            chatRegistrationHandler.handleStep(botExecutor, chatId, messageTextWithName, messageId);

            assertEquals(TouristRegistrationBot.Step.CHECK_INPUT, state.getStep());
        }
    }

    @Nested
    class HandleCommandTest {

        @Test
        void handleCommandWithNoParts() {

            chatRegistrationHandler.handleCommand(botExecutor, chatId, messageId, "");
            verify(conversationRegistry, never()).hasActive(chatId);
        }

        @Test
        void handleCommandInNonActiveChat() {
            when(conversationRegistry.hasActive(chatId)).thenReturn(false);
            chatRegistrationHandler.handleCommand(botExecutor, chatId, messageId, "a:b");
            verify(conversationRegistry, never()).get(chatId);
        }

        @Test
        void handleUnknownCalendarCommand() {
            when(conversationRegistry.hasActive(chatId)).thenReturn(true);

            assertThrows(UnknownTelegramOptionException.class,
                    () -> chatRegistrationHandler.handleCommand(botExecutor, chatId, messageId, "cal:b"));
            verify(conversationRegistry).get(chatId);
        }

        @Test
        void handleYearCalendarCommand() {
            when(conversationRegistry.hasActive(chatId)).thenReturn(true);
            InlineKeyboardMarkup m = YearKeyboard.build(1000);

            chatRegistrationHandler.handleCommand(botExecutor, chatId, messageId, "cal:YEAR_PAGE:1000");

            ArgumentCaptor<EditMessageReplyMarkup> argumentCaptor = ArgumentCaptor.forClass(EditMessageReplyMarkup.class);
            verify(botExecutor).dispatch(argumentCaptor.capture());
            assertEquals(m, argumentCaptor.getValue().getReplyMarkup());
            assertEquals(chatId, Long.parseLong(argumentCaptor.getValue().getChatId()));
            assertEquals(messageId, argumentCaptor.getValue().getMessageId());
            verify(conversationRegistry).get(chatId);
        }

        @Test
        void handleSelectYearCalendarCommand() {
            when(conversationRegistry.hasActive(chatId)).thenReturn(true);
            InlineKeyboardMarkup m = MonthKeyboard.build(12);

            chatRegistrationHandler.handleCommand(botExecutor, chatId, messageId, "cal:SELECT_YEAR:12");

            ArgumentCaptor<EditMessageReplyMarkup> argumentCaptor = ArgumentCaptor.forClass(EditMessageReplyMarkup.class);
            verify(botExecutor).dispatch(argumentCaptor.capture());
            assertEquals(m, argumentCaptor.getValue().getReplyMarkup());
            assertEquals(chatId, Long.parseLong(argumentCaptor.getValue().getChatId()));
            assertEquals(messageId, argumentCaptor.getValue().getMessageId());
            verify(conversationRegistry).get(chatId);
        }

        @Test
        void handleSelectMonthCalendarCommand() {
            when(conversationRegistry.hasActive(chatId)).thenReturn(true);
            InlineKeyboardMarkup m = CalendarKeyboard.build(YearMonth.of(2012, 1));
            chatRegistrationHandler.handleCommand(botExecutor, chatId, messageId, "cal:SELECT_MONTH:2012-01");

            ArgumentCaptor<EditMessageReplyMarkup> argumentCaptor = ArgumentCaptor.forClass(EditMessageReplyMarkup.class);

            verify(botExecutor).dispatch(argumentCaptor.capture());
            assertEquals(m, argumentCaptor.getValue().getReplyMarkup());
            assertEquals(chatId, Long.parseLong(argumentCaptor.getValue().getChatId()));
            assertEquals(messageId, argumentCaptor.getValue().getMessageId());
            verify(conversationRegistry).get(chatId);
        }

        @Test
        void handleSelectDayCalendarCommand() {
            when(conversationRegistry.hasActive(chatId)).thenReturn(true);
            ConversationState state = new ConversationState();
            state.setStep(TouristRegistrationBot.Step.DATE_OF_BIRTH);
            when(conversationRegistry.get(chatId)).thenReturn(state);
            chatRegistrationHandler.handleCommand(botExecutor, chatId, messageId, "cal:SELECT:2012-01-01");

            ArgumentCaptor<EditMessageText> argumentCaptor = ArgumentCaptor.forClass(EditMessageText.class);

            verify(botExecutor).dispatch(argumentCaptor.capture());
            assertEquals(chatId, Long.parseLong(argumentCaptor.getValue().getChatId()));
            assertEquals(messageId, argumentCaptor.getValue().getMessageId());
            verify(conversationRegistry, times(2)).get(chatId);
            assertEquals("01.01.2012", state.getDateOfBirth());
        }
    }
}
