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
import org.tourism.instructors.api.bot.keyboards.CalendarKeyboard;
import org.tourism.instructors.api.bot.keyboards.MonthKeyboard;
import org.tourism.instructors.api.bot.keyboards.OneLineKeyboard;
import org.tourism.instructors.api.bot.keyboards.YearKeyboard;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismListDTO;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.application.tourist.TouristService;
import org.tourism.instructors.domain.pending.ConversationState;
import org.tourism.instructors.domain.tourist.model.Gender;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Component
public class TouristRegistrationBot extends TelegramLongPollingBot {

    public enum Step {NAME, GENDER, DATE_OF_BIRTH, PHONE, EMAIL, CERTIFICATION_ID, KIND_OF_TOURISM, GRADE, CHECK_INPUT}

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final TouristService touristService;
    private final CatalogService catalogService;
    private final PendingTouristService pendingTouristService;
    private final Map<Long, ConversationState> conversations = new ConcurrentHashMap<>();

    @Value("${telegram.bot.username}")
    private String botUsername;

    public TouristRegistrationBot(@Value("${telegram.bot.token}") String token,
                                  PendingTouristService pendingTouristService, TouristService touristService, CatalogService catalogService) {
        super(token);
        this.pendingTouristService = pendingTouristService;
        this.touristService = touristService;
        this.catalogService = catalogService;
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

        if (conversations.containsKey(chatId)) {
            handleStep(chatId, text);
            return;
        }
        Optional<TouristDTO> touristOpt = touristService.findTouristByTelegramId(chatId);
        if (touristOpt.isPresent()) {
            TouristDTO tourist = touristOpt.get();
            send(chatId, tourist.getFullName() + ", с возвращением!");
            return;
        }
        if (text.equals("/start") || text.equals("/register")) {
            startRegistration(chatId, tgUsername);
            return;
        }
        send(chatId, "Используйте /register для регистрации.");
    }

    // ── Registration flow ────────────────────────────────────────────────────

    private void startRegistration(long chatId, String tgUsername) {
        if (pendingTouristService.existsByChatId(chatId)) {
            send(chatId, "Вы уже оставляли заявку ранее. Ожидайте решения.");
            return;
        }
        conversations.put(chatId, new ConversationState(chatId, tgUsername));
        send(chatId, "Начнём регистрацию.\n\nВведите ФИО:");
    }

    private void handleStep(long chatId, String text) {
        ConversationState state = conversations.get(chatId);

        switch (state.getStep()) {
            case NAME -> {
                state.setFullName(text);
                parseFullName(state, text);
                state.setStep(Step.GENDER);
                sendGenderPicker(chatId);
            }
            case GENDER -> {
                state.setStep(Step.DATE_OF_BIRTH);
                sendYearPicker(chatId);
            }
            case DATE_OF_BIRTH -> send(chatId, "Пожалуйста, выберите дату в календаре выше.");
            case PHONE -> {
                state.setPhone(text);
                send(chatId, "Введите email:");
                state.setStep(Step.EMAIL);
            }
            case EMAIL -> {
                state.setEmail(text.equals("-") ? null : text);
                send(chatId, "Введите номер удостоверения:");
                state.setStep(Step.KIND_OF_TOURISM);
            }
            case KIND_OF_TOURISM -> {
                sendKindOfTourismPicker(chatId);
                state.setStep(Step.GRADE);
            }
            case GRADE -> {
                sendGradePicker(chatId);
                sendCheckInputQuestion(chatId, state);
//                state.setStep(Step.CERTIFICATION_ID);
            }
            case CERTIFICATION_ID -> {
                state.setCertificationId(text);
                state.setStep(Step.CHECK_INPUT);
                sendCheckInputQuestion(chatId, state);
            }
        }

    }

    private void sendCheckInputQuestion(long chatId, ConversationState state) {
        StringBuilder builder = new StringBuilder();
        builder.append("Проверьте введенные данные:").append("\n")
                .append("Фамилия:  ").append(state.getLastName()).append("\n")
                .append("Имя:      ").append(state.getFirstName()).append("\n")
                .append("Отчество: ").append(state.getMiddleName()).append("\n")
                .append("Пол: ").append(state.getGender()).append("\n")
                .append("Дата рождения: ").append(state.getDateOfBirth()).append("\n")
                .append("Email:    ").append(state.getEmail()).append("\n")
                .append("Телефон:  ").append(state.getPhone()).append("\n")
                .append("Вид туризма: ").append(state.getKindOfTourism().getTitle()).append("\n")
                .append("Звание:      ").append(state.getGrade().getTitle());
        try {
            Map<String, String> buttons = new LinkedHashMap<>();
            buttons.put("Все верно", "CONFIRM:OK");
            buttons.put("Исправить", "CONFIRM:CANCEL");
            execute(SendMessage.builder().chatId(chatId).text(builder.toString()).replyMarkup(OneLineKeyboard.build(() -> buttons)).build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendKindOfTourismPicker(long chatId) {
        Map<String, String> buttons = catalogService.findActiveKindsOfTourism().stream()
                .collect(Collectors.toMap(
                        KindOfTourismListDTO::title,
                        k -> "KIND_OF_TOURISM:" + k.id(),
                        (s1, s2) -> s1,
                        LinkedHashMap::new));
        try {
            execute(SendMessage.builder().chatId(chatId).text("Укажите вид туризма:").replyMarkup(OneLineKeyboard.build(() -> buttons)).build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendGradePicker(long chatId) {
        Map<String, String> buttons = catalogService.findActiveGrades().stream()
                .collect(Collectors.toMap(
                        GradeDTO::getTitle,
                        gradeDTO -> "GRADE:" + gradeDTO.getId(),
                        (s1, s2) -> s1,
                        LinkedHashMap::new));
        try {
            execute(SendMessage.builder().chatId(chatId).text("Укажите звание:").replyMarkup(OneLineKeyboard.build(() -> buttons)).build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendGenderPicker(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("Мужской", "GENDER:MALE");
        buttons.put("Женский", "GENDER:FEMALE");
        try {
            execute(SendMessage.builder().chatId(chatId).text("Укажите пол:").replyMarkup(OneLineKeyboard.build(() -> buttons)).build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    private static void parseFullName (ConversationState state, String text) {
        List<String> nameParts = Arrays.stream(text.trim().split(" ")).toList();
        if (nameParts.size() == 3) {
            state.setMiddleName(nameParts.get(2));
        }
        if (nameParts.size() >= 2) {
            state.setFirstName(nameParts.get(1));
        }
        if (!nameParts.isEmpty()) {
            state.setLastName(nameParts.getFirst());
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

        List<String> dataParts = new ArrayList<>(asList(data.split(":")));
        if (dataParts.size() < 2) {
            return;
        }
        ConversationState state = conversations.get(chatId);
        if (Objects.isNull(state)) {
            return;
        }
        switch (dataParts.removeFirst()) {
            case "cal" -> processCalendarActions(dataParts, chatId, messageId);
            case "CONFIRM" -> processConfirmationActions(dataParts.removeFirst(), state, chatId);
            case "GENDER" -> {
                state.setGender(Gender.valueOf(dataParts.getFirst()));
                handleStep(chatId, null);
            }
            case "KIND_OF_TOURISM" -> {
                state.setKindOfTourism(catalogService.getKindOfTourismById(Integer.parseInt(dataParts.getFirst())));
                handleStep(chatId, null);
            }
            case "GRADE" -> {
                state.setGrade(catalogService.findGradeById(Integer.parseInt(dataParts.getFirst())));
                handleStep(chatId, null);
            }
        }
    }

    private void processConfirmationActions(String action, ConversationState state, long chatId) {
        switch (action) {
            case "OK" -> {
                pendingTouristService.register(state);
                send(chatId, "Ваша заявка принята ✅ и будет рассмотрена.");
            }
            case "CANCEL" -> {
                conversations.put(chatId, new ConversationState(chatId, state.getTgUsername()));
                send(chatId, "Введите ФИО:");
            }
        }
    }

    private void processCalendarActions(List<String> dataParts, long chatId, int messageId) {
        switch (dataParts.removeFirst()) {
            case "YEAR_PAGE" -> editKeyboard(chatId, messageId, YearKeyboard.build(Integer.parseInt(dataParts.getFirst())));
            case "SELECT_YEAR" -> editKeyboard(chatId, messageId, MonthKeyboard.build(Integer.parseInt(dataParts.getFirst())));
            case "SELECT_MONTH" -> editKeyboard(chatId, messageId, CalendarKeyboard.build(YearMonth.parse(dataParts.getFirst())));
            case "SELECT" -> {
                LocalDate date = LocalDate.parse(dataParts.getFirst());
                ConversationState state = conversations.get(chatId);
                if (state == null || state.getStep() != Step.DATE_OF_BIRTH) return;

                try {
                    execute(EditMessageText.builder()
                            .chatId(chatId)
                            .messageId(messageId)
                            .text("Дата рождения: " + date + " ✅")
                            .build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }

                state.setDateOfBirth(date.format(DISPLAY_FORMAT));
                state.setStep(Step.PHONE);
                send(chatId, "Введите номер телефона:");
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void sendYearPicker(long chatId) {
        int defaultStartYear = YearKeyboard.pageStartFor(2000);
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