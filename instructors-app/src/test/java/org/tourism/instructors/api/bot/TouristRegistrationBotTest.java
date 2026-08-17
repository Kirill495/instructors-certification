package org.tourism.instructors.api.bot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppData;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.application.tourist.TouristService;

@ExtendWith(MockitoExtension.class)
class TouristRegistrationBotTest {

    static final long CHAT_ID = 12345L;
    static final String TG_USERNAME = "test_user";
    static final int MESSAGE_ID = 42;
    static final String CALLBACK_ID = "cb_id";

    @Mock PendingTouristService pendingTouristService;
    @Mock TouristService touristService;
    @Mock ChatRegistrationHandler chatRegistrationHandler;
    @Mock MiniAppHandler miniAppHandler;
    @Mock Message messageMock;
    @Mock User fromMock;

    TouristRegistrationBot bot;

    @BeforeEach
    void setUp() {
        bot =
                spy(
                        new TouristRegistrationBot(
                                "fake-token",
                                pendingTouristService,
                                touristService,
                                chatRegistrationHandler,
                                miniAppHandler));
        ReflectionTestUtils.setField(bot, "miniAppUrl", "https://mini-app.example.com");
    }

    private Update buildUpdateWithMessage() {
        Update update = mock(Update.class);
        when(update.hasCallbackQuery()).thenReturn(false);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(messageMock);
        when(messageMock.getChatId()).thenReturn(CHAT_ID);
        when(messageMock.getFrom()).thenReturn(fromMock);
        when(fromMock.getUserName()).thenReturn(TG_USERNAME);
        return update;
    }

    private void stubDispatchSendMessage() {
        doReturn(mock(Message.class)).when(bot).dispatch(any(SendMessage.class));
    }

    @Nested
    class OnUpdateReceived {

        @Test
        void whenCallbackQuery_thenAnswerCallbackAndDelegateToHandler() {
            Update update = mock(Update.class);
            when(update.hasCallbackQuery()).thenReturn(true);

            CallbackQuery callback = mock(CallbackQuery.class);
            Message callbackMessage = mock(Message.class);
            when(update.getCallbackQuery()).thenReturn(callback);
            when(callback.getMessage()).thenReturn(callbackMessage);
            when(callbackMessage.getChatId()).thenReturn(CHAT_ID);
            when(callbackMessage.getMessageId()).thenReturn(MESSAGE_ID);
            when(callback.getId()).thenReturn(CALLBACK_ID);
            when(callback.getData()).thenReturn("EDIT:NAME");
            doReturn(true).when(bot).dispatch(any(AnswerCallbackQuery.class));

            bot.onUpdateReceived(update);

            ArgumentCaptor<AnswerCallbackQuery> answerCaptor =
                    ArgumentCaptor.forClass(AnswerCallbackQuery.class);
            verify(bot).dispatch(answerCaptor.capture());
            assertEquals(CALLBACK_ID, answerCaptor.getValue().getCallbackQueryId());
            verify(chatRegistrationHandler).handleCommand(bot, CHAT_ID, MESSAGE_ID, "EDIT:NAME");
            verify(update, never()).getMessage();
        }

        @Test
        void whenWebAppData_thenMiniAppHandlerSubmitCalled() {
            Update update = buildUpdateWithMessage();
            WebAppData webAppData = mock(WebAppData.class);
            when(messageMock.getWebAppData()).thenReturn(webAppData);
            when(webAppData.getData()).thenReturn("{\"key\":\"value\"}");

            bot.onUpdateReceived(update);

            verify(miniAppHandler).submit(bot, CHAT_ID, TG_USERNAME, "{\"key\":\"value\"}");
            verify(chatRegistrationHandler, never()).handleStep(any(), anyLong(), any(), any());
        }

        @Test
        void whenMessageHasNoText_thenDoNothing() {
            Update update = buildUpdateWithMessage();
            when(messageMock.getWebAppData()).thenReturn(null);
            when(messageMock.hasText()).thenReturn(false);

            bot.onUpdateReceived(update);

            verify(touristService, never()).findTouristByTelegramId(anyLong());
            verify(chatRegistrationHandler, never()).handleStep(any(), anyLong(), any(), any());
        }

        @Test
        void whenActiveConversation_thenDelegateToHandleStep() {
            Update update = buildUpdateWithMessage();
            when(messageMock.getWebAppData()).thenReturn(null);
            when(messageMock.hasText()).thenReturn(true);
            when(messageMock.getText()).thenReturn("Иванов Иван Иванович");
            when(messageMock.getMessageId()).thenReturn(MESSAGE_ID);
            when(chatRegistrationHandler.hasActiveConversation(CHAT_ID)).thenReturn(true);

            bot.onUpdateReceived(update);

            verify(chatRegistrationHandler)
                    .handleStep(bot, CHAT_ID, "Иванов Иван Иванович", MESSAGE_ID);
            verify(touristService, never()).findTouristByTelegramId(anyLong());
        }

        @Test
        void whenTouristExists_thenSendWelcomeBackMessage() {
            Update update = buildUpdateWithMessage();
            when(messageMock.getWebAppData()).thenReturn(null);
            when(messageMock.hasText()).thenReturn(true);
            when(messageMock.getText()).thenReturn("/start");
            when(chatRegistrationHandler.hasActiveConversation(CHAT_ID)).thenReturn(false);

            TouristDTO tourist = new TouristDTO();
            tourist.setLastName("Иванов");
            tourist.setFirstName("Иван");
            tourist.setMiddleName("Иванович");
            when(touristService.findTouristByTelegramId(CHAT_ID)).thenReturn(Optional.of(tourist));
            stubDispatchSendMessage();

            bot.onUpdateReceived(update);

            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(bot).dispatch(captor.capture());
            String sentText = captor.getValue().getText();
            assertTrue(sentText.contains(tourist.getFullName()));
            assertTrue(sentText.contains("с возвращением!"));
            verify(pendingTouristService, never()).existsByChatId(anyLong());
        }

        @Test
        void whenStartCommandAndPendingExists_thenSendAlreadyAppliedMessage() {
            Update update = buildUpdateWithMessage();
            when(messageMock.getWebAppData()).thenReturn(null);
            when(messageMock.hasText()).thenReturn(true);
            when(messageMock.getText()).thenReturn("/start");
            when(chatRegistrationHandler.hasActiveConversation(CHAT_ID)).thenReturn(false);
            when(touristService.findTouristByTelegramId(CHAT_ID)).thenReturn(Optional.empty());
            when(pendingTouristService.existsByChatId(CHAT_ID)).thenReturn(true);
            stubDispatchSendMessage();

            bot.onUpdateReceived(update);

            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(bot).dispatch(captor.capture());
            assertTrue(captor.getValue().getText().contains("Вы уже оставляли заявку"));
            assertNull(captor.getValue().getReplyMarkup());
        }

        @Test
        void whenStartCommandAndNoPending_thenSendRegistrationFormWithKeyboard() {
            Update update = buildUpdateWithMessage();
            when(messageMock.getWebAppData()).thenReturn(null);
            when(messageMock.hasText()).thenReturn(true);
            when(messageMock.getText()).thenReturn("/start");
            when(chatRegistrationHandler.hasActiveConversation(CHAT_ID)).thenReturn(false);
            when(touristService.findTouristByTelegramId(CHAT_ID)).thenReturn(Optional.empty());
            when(pendingTouristService.existsByChatId(CHAT_ID)).thenReturn(false);
            stubDispatchSendMessage();

            bot.onUpdateReceived(update);

            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(bot).dispatch(captor.capture());
            assertEquals("Для регистрации заполните анкету:", captor.getValue().getText());
            assertNotNull(captor.getValue().getReplyMarkup());
        }

        @Test
        void whenUnknownText_thenSendUsageHint() {
            Update update = buildUpdateWithMessage();
            when(messageMock.getWebAppData()).thenReturn(null);
            when(messageMock.hasText()).thenReturn(true);
            when(messageMock.getText()).thenReturn("hello");
            when(chatRegistrationHandler.hasActiveConversation(CHAT_ID)).thenReturn(false);
            when(touristService.findTouristByTelegramId(CHAT_ID)).thenReturn(Optional.empty());
            stubDispatchSendMessage();

            bot.onUpdateReceived(update);

            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(bot).dispatch(captor.capture());
            assertTrue(captor.getValue().getText().contains("/start"));
        }
    }
}
