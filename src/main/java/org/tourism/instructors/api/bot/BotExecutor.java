package org.tourism.instructors.api.bot;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;

@FunctionalInterface
public interface BotExecutor {
    <T extends Serializable> T dispatch(BotApiMethod<T> method) throws TelegramApiException;

    default int send(long chatId, String text) {
        try {
            return dispatch(SendMessage.builder().chatId(chatId).text(text).build()).getMessageId();
        } catch (TelegramApiException e) {
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }

    default void deleteMessage(long chatId, Integer messageId) {
        if (messageId == null) return;
        try {
            dispatch(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
        } catch (TelegramApiException e) {
            // ignore — message may already be deleted or too old
        }
    }
}
