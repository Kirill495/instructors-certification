package org.tourism.instructors.api.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.tourism.instructors.application.pending.PendingTouristService;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TouristRegistrationBot extends TelegramLongPollingBot {

    private enum Step { LAST_NAME, FIRST_NAME, MIDDLE_NAME, DATE_OF_BIRTH, PHONE, EMAIL }

    private static class ConversationState {
        Step step = Step.LAST_NAME;
        String lastName, firstName, middleName, dateOfBirth, phone, email;
        String tgUsername;
    }

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final Map<Long, ConversationState> conversations = new ConcurrentHashMap<>();
    private final PendingTouristService pendingTouristService;

    @Value("${telegram.bot.username}")
    private String botUsername;

    public TouristRegistrationBot(@Value("${telegram.bot.token}") String token,
                                  PendingTouristService pendingTouristService) {
        super(token);
        this.pendingTouristService = pendingTouristService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();
        String tgUsername = update.getMessage().getFrom().getUserName();

        if (text.equals("/start") || text.equals("/register")) {
            startRegistration(chatId, tgUsername);
            return;
        }

        if (conversations.containsKey(chatId)) {
            handleStep(chatId, text);
        } else {
            send(chatId, "Используйте /register для регистрации.");
        }
    }

    // ── Registration flow ────────────────────────────────────────────────────

    private void startRegistration(long chatId, String tgUsername) {
        if (pendingTouristService.existsByChatId(chatId)) {
            send(chatId, "Вы уже оставляли заявку ранее. Ожидайте решения администратора.");
            return;
        }
        ConversationState state = new ConversationState();
        state.tgUsername = tgUsername;
        conversations.put(chatId, state);
        send(chatId, "Начнём регистрацию.\n\nВведите вашу фамилию:");
    }

    private void handleStep(long chatId, String text) {
        ConversationState state = conversations.get(chatId);

        switch (state.step) {
            case LAST_NAME -> {
                state.lastName = text;
                state.step = Step.FIRST_NAME;
                send(chatId, "Введите ваше имя:");
            }
            case FIRST_NAME -> {
                state.firstName = text;
                state.step = Step.MIDDLE_NAME;
                send(chatId, "Введите ваше отчество (или «-» если нет):");
            }
            case MIDDLE_NAME -> {
                state.middleName = text.equals("-") ? null : text;
                state.step = Step.DATE_OF_BIRTH;
                sendYearPicker(chatId);
            }
            case DATE_OF_BIRTH ->
                send(chatId, "Пожалуйста, выберите дату на календаре выше.");
            case PHONE -> {
                state.phone = text;
                state.step = Step.EMAIL;
                send(chatId, "Введите email (или «-» если нет):");
            }
            case EMAIL -> {
                state.email = text.equals("-") ? null : text;
                conversations.remove(chatId);
                pendingTouristService.register(chatId, state.tgUsername,
                        state.lastName, state.firstName, state.middleName,
                        state.dateOfBirth, state.email, state.phone);
                send(chatId, "Ваша заявка принята ✅\n\n" +
                        state.lastName + " " + state.firstName +
                        (state.middleName != null ? " " + state.middleName : "") +
                        "\n\nАдминистратор рассмотрит её в ближайшее время.");
            }
        }
    }

    // ── Callback handling ────────────────────────────────────────────────────

    private void handleCallback(CallbackQuery callback) {
        String data = callback.getData();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();

        try {
            execute(AnswerCallbackQuery.builder().callbackQueryId(callback.getId()).build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        if (data.equals("cal:IGNORE")) return;

        if (data.startsWith("cal:YEAR_PAGE:")) {
            int startYear = Integer.parseInt(data.substring(14));
            editKeyboard(chatId, messageId, YearKeyboard.build(startYear));

        } else if (data.startsWith("cal:SELECT_YEAR:")) {
            int year = Integer.parseInt(data.substring(16));
            editKeyboard(chatId, messageId, MonthKeyboard.build(year));

        } else if (data.startsWith("cal:SELECT_MONTH:")) {
            YearMonth yearMonth = YearMonth.parse(data.substring(17));
            editKeyboard(chatId, messageId, CalendarKeyboard.build(yearMonth));

        } else if (data.startsWith("cal:SELECT:")) {
            LocalDate date = LocalDate.parse(data.substring(11));
            ConversationState state = conversations.get(chatId);
            if (state == null || state.step != Step.DATE_OF_BIRTH) return;

            state.dateOfBirth = date.format(DISPLAY_FORMAT);
            state.step = Step.PHONE;

            try {
                execute(EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .text("Дата рождения: " + state.dateOfBirth + " ✅")
                        .build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

            send(chatId, "Введите номер телефона:");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void sendYearPicker(long chatId) {
        int defaultStartYear = YearKeyboard.pageStartFor(Year.now().getValue() - 30);
        try {
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("Выберите год рождения:")
                    .replyMarkup(YearKeyboard.build(defaultStartYear))
                    .build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void editKeyboard(long chatId, int messageId,
                              org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup markup) {
        try {
            execute(EditMessageReplyMarkup.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .replyMarkup(markup)
                    .build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(long chatId, String text) {
        try {
            execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (TelegramApiException e) {
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }
}