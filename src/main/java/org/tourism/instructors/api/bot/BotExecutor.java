package org.tourism.instructors.api.bot;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;

import java.io.Serializable;

@FunctionalInterface
public interface BotExecutor {
    <T extends Serializable> T dispatch(BotApiMethod<T> method);

    default int send(long chatId, String text) {
        return dispatch(SendMessage.builder().chatId(chatId).text(text).build()).getMessageId();
    }

    default void deleteMessage(long chatId, Integer messageId) {
        if (messageId == null) {
            return;
        }

        dispatch(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

    }
}
