package org.tourism.instructors.api.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@ExtendWith(MockitoExtension.class)
class BotInitializerTest {

    @Mock TouristRegistrationBot bot;

    BotInitializer botInitializer;

    @BeforeEach
    void setUp() {
        botInitializer = new BotInitializer(bot);
    }

    @Test
    void init_withNullToken_doesNotConstructApi() throws TelegramApiException {
        ReflectionTestUtils.setField(botInitializer, "token", null);

        try (MockedConstruction<TelegramBotsApi> mockedApi =
                mockConstruction(TelegramBotsApi.class)) {
            botInitializer.init();

            assertTrue(mockedApi.constructed().isEmpty());
        }
    }

    @Test
    void init_withEmptyToken_doesNotConstructApi() throws TelegramApiException {
        ReflectionTestUtils.setField(botInitializer, "token", "");

        try (MockedConstruction<TelegramBotsApi> mockedApi =
                mockConstruction(TelegramBotsApi.class)) {
            botInitializer.init();

            assertTrue(mockedApi.constructed().isEmpty());
        }
    }

    @Test
    void init_withValidToken_constructsApiAndRegistersBot() throws TelegramApiException {
        ReflectionTestUtils.setField(botInitializer, "token", "valid-token");

        try (MockedConstruction<TelegramBotsApi> mockedApi =
                mockConstruction(TelegramBotsApi.class)) {
            botInitializer.init();

            assertEquals(1, mockedApi.constructed().size());
            verify(mockedApi.constructed().get(0)).registerBot(bot);
        }
    }
}
