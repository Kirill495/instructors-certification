package org.tourism.instructors.api.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.domain.pending.ConversationState;
import org.tourism.instructors.domain.tourist.model.Gender;

@Component
public class MiniAppHandler {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final CatalogService catalogService;
    private final PendingTouristService pendingTouristService;

    public MiniAppHandler(
            CatalogService catalogService, PendingTouristService pendingTouristService) {
        this.catalogService = catalogService;
        this.pendingTouristService = pendingTouristService;
    }

    public void submit(BotExecutor bot, long chatId, String tgUsername, String json) {
        try {
            JsonNode node = jsonMapper.readTree(json);

            ConversationState state = new ConversationState(chatId, tgUsername);
            state.setLastName(node.path("lastName").asText());
            state.setFirstName(node.path("firstName").asText());
            String middleName = node.path("middleName").asText(null);
            state.setMiddleName(middleName == null || middleName.isBlank() ? null : middleName);
            state.setFullName(
                    state.getLastName()
                            + " "
                            + state.getFirstName()
                            + (state.getMiddleName() != null ? " " + state.getMiddleName() : ""));
            state.setGender(Gender.valueOf(node.path("gender").asText()));

            LocalDate dob = LocalDate.parse(node.path("dateOfBirth").asText());
            state.setDateOfBirth(dob.format(DISPLAY_FORMAT));

            String phone = node.path("phone").asText(null);
            state.setPhoneNumber(phone == null || phone.isBlank() ? null : phone);
            String email = node.path("email").asText(null);
            state.setEmail(email == null || email.isBlank() ? null : email);

            state.setKindOfTourism(
                    catalogService.getKindOfTourismById(node.path("kindOfTourismId").asInt()));
            state.setGrade(catalogService.findGradeById(node.path("gradeId").asInt()));

            pendingTouristService.register(state);
            bot.dispatch(
                    SendMessage.builder()
                            .chatId(chatId)
                            .text("✅ " + tgUsername + ", ваша заявка принята.")
                            .replyMarkup(new ReplyKeyboardRemove(true))
                            .build());
        } catch (Exception e) {
            bot.send(chatId, "Ошибка при обработке заявки. Попробуйте ещё раз.");
        }
    }
}
