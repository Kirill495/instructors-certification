package org.tourism.instructors.api.bot;

import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class BotInitializer {

    private final TouristRegistrationBot bot;
    @Value("$telegram.bot.token:}")
    private String token;

    public BotInitializer(TouristRegistrationBot bot) {
        this.bot = bot;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() throws TelegramApiException {
        if (StringUtil.isBlank(token)) {
            return;
        }
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);
    }
}