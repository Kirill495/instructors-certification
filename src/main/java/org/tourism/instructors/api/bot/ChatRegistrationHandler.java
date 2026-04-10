package org.tourism.instructors.api.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.tourism.instructors.api.bot.exception.UnknownButtonConversationException;
import org.tourism.instructors.api.bot.exception.UnknownQuestionnaireFieldException;
import org.tourism.instructors.api.bot.exception.UnknownTelegramOptionException;
import org.tourism.instructors.api.bot.keyboards.CalendarKeyboard;
import org.tourism.instructors.api.bot.keyboards.MonthKeyboard;
import org.tourism.instructors.api.bot.keyboards.OneLineKeyboard;
import org.tourism.instructors.api.bot.keyboards.YearKeyboard;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismListDTO;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.domain.pending.ConversationState;
import org.tourism.instructors.domain.pending.repository.RegistrationStep;
import org.tourism.instructors.domain.tourist.model.Gender;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Component
public class ChatRegistrationHandler {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final CatalogService catalogService;
    private final PendingTouristService pendingTouristService;
    private final ConversationStateRegistry conversationsRegistry;

    public ChatRegistrationHandler(CatalogService catalogService, PendingTouristService pendingTouristService, ConversationStateRegistry registry) {
        this.catalogService = catalogService;
        this.pendingTouristService = pendingTouristService;
        this.conversationsRegistry = registry;
    }

    public boolean hasActiveConversation(Long chatId) {
        return conversationsRegistry.hasActive(chatId);
    }

    public void handleStep(BotExecutor bot, long chatId, String text, Integer userMessageId) {
        ConversationState state = conversationsRegistry.get(chatId);

        switch (state.getRegistrationStep()) {
            case NAME -> {
                state.setFullName(text);
                parseFullName(state, text);
                if (state.isEditing()) {
                    refreshSummary(bot, chatId, state, userMessageId);
                    return;
                }
                state.setRegistrationStep(RegistrationStep.GENDER);
                sendGenderPicker(bot, chatId);
            }
            case GENDER -> {
                sendYearPicker(bot, chatId);
                state.setRegistrationStep(RegistrationStep.DATE_OF_BIRTH);
            }
            case DATE_OF_BIRTH -> bot.send(chatId, "Пожалуйста, выберите дату в календаре выше.");
            case PHONE -> {
                state.setPhoneNumber(text);
                if (state.isEditing()) {
                    refreshSummary(bot, chatId, state, userMessageId);
                    return;
                }
                bot.send(chatId, "Введите email:");
                state.setRegistrationStep(RegistrationStep.EMAIL);
            }
            case EMAIL -> {
                state.setEmail(text.equals("-") ? null : text);
                if (state.isEditing()) {
                    refreshSummary(bot, chatId, state, userMessageId);
                    return;
                }
                bot.send(chatId, "Введите номер удостоверения:");
                state.setRegistrationStep(RegistrationStep.KIND_OF_TOURISM);
            }
            case KIND_OF_TOURISM -> {
                sendKindOfTourismPicker(bot, chatId);
                state.setRegistrationStep(RegistrationStep.GRADE);
            }
            case GRADE -> {
                sendGradePicker(bot, chatId);
                sendCheckInputQuestion(bot, chatId, state);
            }
            default -> throw new UnknownQuestionnaireFieldException(state.getRegistrationStep().name());
        }
    }

    public void handleCommand(BotExecutor bot, long chatId, int messageId, String commandData) {

        List<String> dataParts = new ArrayList<>(asList(commandData.split(":")));
        if (dataParts.size() < 2) {
            return;
        }
        if (!conversationsRegistry.hasActive(chatId)) {
            return;
        }
        ConversationState state = conversationsRegistry.get(chatId);
        String step = dataParts.removeFirst();
        switch (step) {
            case "cal" -> processCalendarActions(bot, dataParts, chatId, messageId);
            case "CONFIRM" -> processConfirmationActions(bot, dataParts.removeFirst(), state, chatId);
            case "GENDER" -> {
                state.setGender(Gender.valueOf(dataParts.getFirst()));
                if (state.isEditing()) {
                    bot.deleteMessage(chatId, messageId);
                    refreshSummary(bot, chatId, state, null);
                    return;
                }
                handleStep(bot, chatId, null, null);
            }
            case "KIND_OF_TOURISM" -> {
                state.setKindOfTourism(catalogService.getKindOfTourismById(Integer.parseInt(dataParts.getFirst())));
                if (state.isEditing()) {
                    bot.deleteMessage(chatId, messageId);
                    refreshSummary(bot, chatId, state, null);
                    return;
                }
                handleStep(bot, chatId, null, null);
            }
            case "GRADE" -> {
                state.setGrade(catalogService.findGradeById(Integer.parseInt(dataParts.getFirst())));
                if (state.isEditing()) {
                    bot.deleteMessage(chatId, messageId);
                    refreshSummary(bot, chatId, state, null);
                    return;
                }
                handleStep(bot, chatId, null, null);
            }
            case "EDIT" -> {
                state.setEditing(true);
                state.setSummaryMessageId(messageId);
                switch (dataParts.getFirst()) {
                    case "NAME"           -> {
                        state.setRegistrationStep(RegistrationStep.NAME);
                        state.setEditingPromptMessageId(bot.send(chatId, "Введите ФИО:"));
                    }
                    case "GENDER"         -> {
                        state.setRegistrationStep(RegistrationStep.GENDER);
                        state.setEditingPromptMessageId(sendGenderPicker(bot, chatId));
                    }
                    case "DATE_OF_BIRTH"  -> {
                        state.setRegistrationStep(RegistrationStep.DATE_OF_BIRTH);
                        state.setEditingPromptMessageId(sendYearPicker(bot, chatId));
                    }
                    case "PHONE"          -> {
                        state.setRegistrationStep(RegistrationStep.PHONE);
                        state.setEditingPromptMessageId(bot.send(chatId, "Введите номер телефона:"));
                    }
                    case "EMAIL"          -> {
                        state.setRegistrationStep(RegistrationStep.EMAIL);
                        state.setEditingPromptMessageId(bot.send(chatId, "Введите email (или «-» если нет):"));
                    }
                    case "KIND_OF_TOURISM" -> {
                        state.setRegistrationStep(RegistrationStep.KIND_OF_TOURISM);
                        state.setEditingPromptMessageId(sendKindOfTourismPicker(bot, chatId));
                    }
                    case "GRADE"          -> {
                        state.setRegistrationStep(RegistrationStep.GRADE);
                        state.setEditingPromptMessageId(sendGradePicker(bot, chatId));
                    }
                    default -> throw new UnknownQuestionnaireFieldException(dataParts.getFirst());
                }
            }
            default -> throw new UnknownButtonConversationException(step);
        }
    }

    private void processConfirmationActions(BotExecutor bot, String action, ConversationState state, long chatId) {
        if ("OK".equals(action)) {
            pendingTouristService.register(state);
            bot.send(chatId, "Ваша заявка принята ✅ и будет рассмотрена.");
        } else if ("CANCEL".equals(action)) {
            conversationsRegistry.put(chatId, new ConversationState(chatId, state.getTgUsername()));

            bot.send(chatId, "Введите ФИО:");
        }
    }

    private void processCalendarActions(BotExecutor bot, List<String> dataParts, long chatId, int messageId) {

        String option = dataParts.removeFirst();
        switch (option) {
            case "YEAR_PAGE" -> editKeyboard(bot, chatId, messageId, YearKeyboard.build(Integer.parseInt(dataParts.getFirst())));
            case "SELECT_YEAR" -> editKeyboard(bot, chatId, messageId, MonthKeyboard.build(Integer.parseInt(dataParts.getFirst())));
            case "SELECT_MONTH" -> editKeyboard(bot, chatId, messageId, CalendarKeyboard.build(YearMonth.parse(dataParts.getFirst())));
            case "SELECT" -> {
                LocalDate date = LocalDate.parse(dataParts.getFirst());
                ConversationState state = conversationsRegistry.get(chatId);
                if (state == null || state.getRegistrationStep() != RegistrationStep.DATE_OF_BIRTH) return;

                bot.dispatch(EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .text("Дата рождения: " + date + " ✅")
                        .build());

                state.setDateOfBirth(date.format(DISPLAY_FORMAT));
                if (state.isEditing()) {
                    bot.deleteMessage(chatId, messageId); refreshSummary(bot, chatId, state, null); return;
                }
                state.setRegistrationStep(RegistrationStep.PHONE);
                bot.send(chatId, "Введите номер телефона:");
            }
            default -> throw new UnknownTelegramOptionException(option);
        }
    }

    private static void parseFullName(ConversationState state, String text) {
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

    private void refreshSummary(BotExecutor bot, long chatId, ConversationState state, Integer userMessageId) {
        bot.deleteMessage(chatId, state.getEditingPromptMessageId());
        if (Objects.nonNull(userMessageId)) {
            bot.deleteMessage(chatId, userMessageId);
        }
        state.setEditing(false);
        state.setRegistrationStep(RegistrationStep.CHECK_INPUT);
        EditMessageText message = EditMessageText.builder()
                .chatId(chatId)
                .messageId(state.getSummaryMessageId())
                .text("Проверьте данные:")
                .replyMarkup(buildSummaryKeyboard(state))
                .build();
        bot.dispatch(message);
    }

    private void sendCheckInputQuestion(BotExecutor bot, long chatId, ConversationState state) {
        var message = bot.dispatch(SendMessage.builder()
                .chatId(chatId)
                .text("Проверьте данные:")
                .replyMarkup(buildSummaryKeyboard(state))
                .build());
        state.setSummaryMessageId(message.getMessageId());
        state.setRegistrationStep(RegistrationStep.CHECK_INPUT);
    }

    private InlineKeyboardMarkup buildSummaryKeyboard(ConversationState state) {
        String genderLabel = state.getGender() == Gender.MALE ? "Мужской ♂" : "Женский ♀";
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(summaryRow(state.getLastName() + " " + state.getFirstName() + (state.getMiddleName() != null ? " " + state.getMiddleName() : ""), "EDIT:NAME"));
        rows.add(summaryRow(genderLabel, "EDIT:GENDER"));
        rows.add(summaryRow(state.getDateOfBirth(), "EDIT:DATE_OF_BIRTH"));
        rows.add(summaryRow(state.getPhoneNumber() != null ? state.getPhoneNumber() : "-", "EDIT:PHONE"));
        rows.add(summaryRow(state.getEmail() != null ? state.getEmail() : "-", "EDIT:EMAIL"));
        rows.add(summaryRow(state.getKindOfTourism().getTitle(), "EDIT:KIND_OF_TOURISM"));
        rows.add(summaryRow(state.getGrade().title(), "EDIT:GRADE"));
        rows.add(List.of(InlineKeyboardButton.builder()
                .text("✅ Всё верно").callbackData("CONFIRM:OK").build()));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private List<InlineKeyboardButton> summaryRow(String value, String callback) {
        return List.of(InlineKeyboardButton.builder()
                .text(value + " ✏️").callbackData(callback).build());
    }

    private int sendKindOfTourismPicker(BotExecutor bot, long chatId) {
        Map<String, String> buttons = catalogService.findActiveKindsOfTourism().stream()
                .collect(Collectors.toMap(
                        KindOfTourismListDTO::title,
                        k -> "KIND_OF_TOURISM:" + k.id(),
                        (s1, s2) -> s1,
                        LinkedHashMap::new));
        var message = SendMessage.builder().chatId(chatId).text("Укажите вид туризма:").replyMarkup(OneLineKeyboard.build(() -> buttons)).build();
        return bot.dispatch(message).getMessageId();
    }

    private int sendGradePicker(BotExecutor bot, long chatId) {
        Map<String, String> buttons = catalogService.findActiveGrades().stream()
                .collect(Collectors.toMap(
                        GradeDTO::title,
                        gradeDTO -> "GRADE:" + gradeDTO.id(),
                        (s1, s2) -> s1,
                        LinkedHashMap::new));
        return bot
                .dispatch(SendMessage.builder()
                        .chatId(chatId)
                        .text("Укажите звание:")
                        .replyMarkup(OneLineKeyboard.build(() -> buttons))
                        .build())
                .getMessageId();
    }

    private int sendGenderPicker(BotExecutor bot,
                                 long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("Мужской", "GENDER:MALE");
        buttons.put("Женский", "GENDER:FEMALE");
        SendMessage message = SendMessage.builder().chatId(chatId).text("Укажите пол:").replyMarkup(OneLineKeyboard.build(() -> buttons)).build();
        return bot.dispatch(message).getMessageId();

    }

    private int sendYearPicker(BotExecutor bot, long chatId) {
        int defaultStartYear = YearKeyboard.pageStartFor(2000);
        return bot.dispatch(SendMessage.builder()
                .chatId(chatId)
                .text("Выберите год рождения:")
                .replyMarkup(YearKeyboard.build(defaultStartYear))
                .build()).getMessageId();
    }

    private void editKeyboard(BotExecutor bot, long chatId, int messageId,
                              InlineKeyboardMarkup markup) {
        bot.dispatch(EditMessageReplyMarkup.builder()
                .chatId(chatId)
                .messageId(messageId)
                .replyMarkup(markup)
                .build());
    }

}
