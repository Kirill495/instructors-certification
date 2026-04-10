package org.tourism.instructors.api.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.tourism.instructors.api.bot.exception.TelegramDispatchException;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.application.tourist.TouristService;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class TouristRegistrationBot extends TelegramLongPollingBot implements BotExecutor {

    private final TouristService touristService;
    private final PendingTouristService pendingTouristService;
    private final ChatRegistrationHandler chatRegistrationHandler;
    private final MiniAppHandler miniAppHandler;


    @Override
    public <T extends Serializable> T dispatch(BotApiMethod<T> method) {
        try {
            return execute(method);
        } catch (TelegramApiException exception) {
            throw new TelegramDispatchException(exception);
        }
    }

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.mini-app-url}")
    private String miniAppUrl;

    public TouristRegistrationBot(@Value("${telegram.bot.token}") String token,
                                  PendingTouristService pendingTouristService,
                                  TouristService touristService,
                                  ChatRegistrationHandler chatRegistrationHandler,
                                  MiniAppHandler miniAppHandler) {
        super(token);
        this.pendingTouristService = pendingTouristService;
        this.touristService = touristService;
        this.chatRegistrationHandler = chatRegistrationHandler;
        this.miniAppHandler = miniAppHandler;
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

        long chatId = update.getMessage().getChatId();
        String tgUsername = update.getMessage().getFrom().getUserName();

        if (!update.hasMessage()) {
            return;
        }
        Message message = update.getMessage();
        if (Objects.nonNull(message.getWebAppData())) {
            miniAppHandler.submit(this, chatId, tgUsername, message.getWebAppData().getData());
            return;
        }
        if (!message.hasText()) {
            return;
        }
        String text = update.getMessage().getText().trim();
        if (chatRegistrationHandler.hasActiveConversation(chatId)) {
            chatRegistrationHandler.handleStep(this, chatId, text, update.getMessage().getMessageId());
            return;
        }
        Optional<TouristDTO> touristOpt = touristService.findTouristByTelegramId(chatId);
        if (touristOpt.isPresent()) {
            TouristDTO tourist = touristOpt.get();
            send(chatId, tourist.getFullName() + ", с возвращением!");
            return;
        }
        if (text.equals("/start")) {
            startRegistration(chatId);
            return;
        }
        send(chatId, "Используйте /start для регистрации.");
    }

    @Override
    public int send(long chatId, String text) {
        return dispatch(SendMessage.builder().chatId(chatId).text(text).build()).getMessageId();
    }

    // ── Registration flow ────────────────────────────────────────────────────

    private void startRegistration(long chatId) {
        if (pendingTouristService.existsByChatId(chatId)) {
            send(chatId, "Вы уже оставляли заявку ранее. Ожидайте решения.");
            return;
        }
        var button = KeyboardButton.builder()
                .text("📋 Заполнить анкету")
                .webApp(new WebAppInfo(miniAppUrl))
                .build();
        var keyboard = ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow(List.of(button)))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
        dispatch(SendMessage.builder().chatId(chatId).text("Для регистрации заполните анкету:").replyMarkup(keyboard).build());
    }

    // ── Callback handling ────────────────────────────────────────────────────

    private void handleCallback(CallbackQuery callback) {
        String data = callback.getData();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();

        dispatch(AnswerCallbackQuery.builder().callbackQueryId(callback.getId()).build());
        chatRegistrationHandler.handleCommand(this, chatId, messageId, data);
    }

}
