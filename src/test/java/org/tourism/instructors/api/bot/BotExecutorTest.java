package org.tourism.instructors.api.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotExecutorTest {

    static final long CHAT_ID = 12345L;
    static final int MESSAGE_ID = 99;
    static final String TEXT = "Hello, world!";

    BotExecutor executor;

    @BeforeEach
    void setUp() {
        executor = spy(new StubBotExecutor());
    }

    static class StubBotExecutor implements BotExecutor {
        @Override
        public <T extends Serializable> T dispatch(BotApiMethod<T> method) {
            return null;
        }
    }

    @Nested
    class SendDefault {

        @Test
        void dispatchesSendMessageWithCorrectChatIdAndText() {
            Message message = mock(Message.class);
            when(message.getMessageId()).thenReturn(MESSAGE_ID);
            doReturn(message).when(executor).dispatch(any(SendMessage.class));

            executor.send(CHAT_ID, TEXT);

            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(executor).dispatch(captor.capture());
            assertEquals(String.valueOf(CHAT_ID), captor.getValue().getChatId());
            assertEquals(TEXT, captor.getValue().getText());
        }

        @Test
        void returnsMessageIdFromDispatch() {
            Message message = mock(Message.class);
            when(message.getMessageId()).thenReturn(MESSAGE_ID);
            doReturn(message).when(executor).dispatch(any(SendMessage.class));

            int result = executor.send(CHAT_ID, TEXT);

            assertEquals(MESSAGE_ID, result);
        }
    }

    @Nested
    class DeleteMessageDefault {

        @Test
        void whenMessageIdIsNull_thenDispatchIsNotCalled() {
            executor.deleteMessage(CHAT_ID, null);

            verify(executor, never()).dispatch(any());
        }

        @Test
        void whenMessageIdIsNotNull_thenDispatchesDeleteMessageWithCorrectArgs() {
            doReturn(true).when(executor).dispatch(any(DeleteMessage.class));

            executor.deleteMessage(CHAT_ID, MESSAGE_ID);

            ArgumentCaptor<DeleteMessage> captor = ArgumentCaptor.forClass(DeleteMessage.class);
            verify(executor).dispatch(captor.capture());
            assertEquals(String.valueOf(CHAT_ID), captor.getValue().getChatId());
            assertEquals(MESSAGE_ID, captor.getValue().getMessageId());
        }
    }
}