package org.tourism.instructors.api.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage;
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
import org.tourism.instructors.domain.pending.repository.RegistrationStep;

import java.time.YearMonth;
import java.util.List;

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
    class HandleRegistrationStepTest {
        @BeforeEach
        void setUp() {
            when(conversationRegistry.get(chatId)).thenReturn(state);
        }

        @Test
        void handleNameStep() {

            state.setRegistrationStep(RegistrationStep.NAME);
            when(botExecutor.dispatch(any(SendMessage.class))).thenReturn(message);
            when(message.getMessageId()).thenReturn(messageId);

            chatRegistrationHandler.handleStep(botExecutor, chatId, "Andrew Fedorov", 1);

            assertEquals(RegistrationStep.GENDER, state.getRegistrationStep());
        }

        @Test
        void handleNameStepEditing() {
            KindOfTourismDTO kindOfTourism = new KindOfTourismDTO();
            state.setRegistrationStep(RegistrationStep.NAME);
            GradeDTO grade = new GradeDTO(1, "grade", false, 10);
            state.setGrade(grade);
            state.setEditing(true);
            state.setKindOfTourism(kindOfTourism);
            when(botExecutor.dispatch(any())).thenReturn(any(SendMessage.class));

            chatRegistrationHandler.handleStep(botExecutor, chatId, messageTextWithName, messageId);

            assertFalse(state.isEditing());
            assertEquals(RegistrationStep.CHECK_INPUT, state.getRegistrationStep());
            ArgumentCaptor<EditMessageText> argumentCaptor = ArgumentCaptor.forClass(EditMessageText.class);
            verify(botExecutor).dispatch(argumentCaptor.capture());
            assertEquals(chatId, Long.parseLong(argumentCaptor.getValue().getChatId()));
        }

        @Test
        void handleDateOfBirth() {
            state.setRegistrationStep(RegistrationStep.DATE_OF_BIRTH);

            chatRegistrationHandler.handleStep(botExecutor, chatId, messageTextWithName, messageId);

            assertEquals(RegistrationStep.DATE_OF_BIRTH, state.getRegistrationStep());

            verify(botExecutor).send(eq(chatId), any(String.class));
        }

        @Test
        void handlePhone() {
            state.setRegistrationStep(RegistrationStep.PHONE);

            chatRegistrationHandler.handleStep(botExecutor, chatId, messageTextWithName, messageId);

            assertEquals(RegistrationStep.EMAIL, state.getRegistrationStep());

            verify(botExecutor).send(eq(chatId), any(String.class));
        }

        @Test
        void handleEmail() {
            state.setRegistrationStep(RegistrationStep.EMAIL);

            chatRegistrationHandler.handleStep(botExecutor, chatId, messageTextWithName, messageId);

            assertEquals(RegistrationStep.KIND_OF_TOURISM, state.getRegistrationStep());

            verify(botExecutor).send(eq(chatId), any(String.class));
        }

        @Test
        void handleKindOfTourism() {
            state.setRegistrationStep(RegistrationStep.KIND_OF_TOURISM);
            when(botExecutor.dispatch(any(SendMessage.class))).thenReturn(message);
            when(message.getMessageId()).thenReturn(messageId);

            chatRegistrationHandler.handleStep(botExecutor, chatId, messageTextWithName, messageId);

            assertEquals(RegistrationStep.GRADE, state.getRegistrationStep());
        }

        @Test
        void handleGrade() {
            state.setRegistrationStep(RegistrationStep.GRADE);
            when(botExecutor.dispatch(any(SendMessage.class))).thenReturn(message);
            when(message.getMessageId()).thenReturn(messageId);

            chatRegistrationHandler.handleStep(botExecutor, chatId, messageTextWithName, messageId);

            assertEquals(RegistrationStep.CHECK_INPUT, state.getRegistrationStep());
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
            state.setRegistrationStep(RegistrationStep.DATE_OF_BIRTH);
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

    @Nested
    class handleConfirmCaseCommand {

        @BeforeEach
        void setUp() {
            state.setRegistrationStep(RegistrationStep.DATE_OF_BIRTH);
            when(conversationRegistry.get(chatId)).thenReturn(state);
            when(conversationRegistry.hasActive(chatId)).thenReturn(true);
        }

        @Test
        void testHandleCommandOK () {
            chatRegistrationHandler.handleCommand(botExecutor, chatId, messageId, "CONFIRM:OK");
            verify(botExecutor, times(1)).send(chatId, "Ваша заявка принята ✅ и будет рассмотрена.");
        }

        @Test
        void testHandleCommandCancel () {
            chatRegistrationHandler.handleCommand(botExecutor, chatId, messageId, "CONFIRM:CANCEL");
            verify(botExecutor, times(1)).send(chatId, "Введите ФИО:");
        }
    }

    @Nested
    class handleGenderCaseCommand {

        @BeforeEach
        void setUp() {
            state.setRegistrationStep(RegistrationStep.GENDER);
            when(conversationRegistry.get(chatId)).thenReturn(state);
            when(conversationRegistry.hasActive(chatId)).thenReturn(true);
        }

        @Test
        void testHandleGenderCase() {

            when(botExecutor.dispatch(any(SendMessage.class))).thenReturn(message);
            when(message.getMessageId()).thenReturn(messageId);

            chatRegistrationHandler.handleCommand(botExecutor, chatId, messageId, "GENDER:MALE");

            assertEquals(RegistrationStep.DATE_OF_BIRTH, state.getRegistrationStep());
            ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);

            verify(botExecutor).dispatch(argumentCaptor.capture());
            assertEquals("Выберите год рождения:", argumentCaptor.getValue().getText());
        }

        @Test
        void testHandleKindOfTourismCase() {

            when(botExecutor.dispatch(any(SendMessage.class))).thenReturn(message);
            when(message.getMessageId()).thenReturn(messageId);
            state.setRegistrationStep(RegistrationStep.KIND_OF_TOURISM);

            chatRegistrationHandler.handleCommand(botExecutor, chatId, messageId, "KIND_OF_TOURISM:1");

            assertEquals(RegistrationStep.GRADE, state.getRegistrationStep());
            ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);

            verify(botExecutor).dispatch(argumentCaptor.capture());
            assertEquals("Укажите вид туризма:", argumentCaptor.getValue().getText());
        }

        @Test
        void testHandleGradeCase() {

            when(botExecutor.dispatch(any(SendMessage.class))).thenReturn(message);
            when(message.getMessageId()).thenReturn(messageId);
            state.setRegistrationStep(RegistrationStep.GRADE);
            GradeDTO grade = new GradeDTO(1, "grade", false, 10);
            state.setGrade(grade);
            when(catalogService.findActiveGrades()).thenReturn(List.of(grade));
            when(catalogService.findGradeById(1)).thenReturn(grade);
            chatRegistrationHandler.handleCommand(botExecutor, chatId, messageId, "GRADE:1");

            assertEquals(RegistrationStep.CHECK_INPUT, state.getRegistrationStep());
            ArgumentCaptor<BotApiMethodMessage> argumentCaptor = ArgumentCaptor.forClass(BotApiMethodMessage.class);

            verify(botExecutor, times(2)).dispatch(argumentCaptor.capture());
        }
    }
}
