package org.tourism.instructors.api.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismDTO;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.domain.pending.ConversationState;
import org.tourism.instructors.domain.tourist.model.Gender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MiniAppHandlerTest {

    static final long CHAT_ID = 12345L;
    static final String TG_USERNAME = "test_user";

    @Mock CatalogService catalogService;
    @Mock PendingTouristService pendingTouristService;
    @Mock BotExecutor bot;

    @InjectMocks
    MiniAppHandler miniAppHandler;

    final KindOfTourismDTO kindOfTourism = new KindOfTourismDTO(1, "Hiking", false);
    final GradeDTO grade = new GradeDTO(2, "Instructor", false, 5);

    private String buildJson(String lastName, String firstName, String middleName,
                              String gender, String dateOfBirth,
                              String phone, String email,
                              int kindOfTourismId, int gradeId) {
        return String.format(
                "{\"lastName\":\"%s\",\"firstName\":\"%s\",\"middleName\":\"%s\"," +
                "\"gender\":\"%s\",\"dateOfBirth\":\"%s\"," +
                "\"phone\":\"%s\",\"email\":\"%s\"," +
                "\"kindOfTourismId\":%d,\"gradeId\":%d}",
                lastName, firstName, middleName, gender, dateOfBirth,
                phone, email, kindOfTourismId, gradeId);
    }

    @Nested
    class SubmitWithValidData {

        @BeforeEach
        void setUpCatalogAndBot() {
            doReturn(mock(Message.class)).when(bot).dispatch(any(SendMessage.class));
            when(catalogService.getKindOfTourismById(1)).thenReturn(kindOfTourism);
            when(catalogService.findGradeById(2)).thenReturn(grade);
        }

        @Test
        void parsesAllFieldsAndRegisters() {
            String json = buildJson("Иванов", "Иван", "Иванович",
                    "MALE", "1990-05-15", "+79001234567", "ivan@example.com", 1, 2);

            miniAppHandler.submit(bot, CHAT_ID, TG_USERNAME, json);

            ArgumentCaptor<ConversationState> stateCaptor = ArgumentCaptor.forClass(ConversationState.class);
            verify(pendingTouristService).register(stateCaptor.capture());

            ConversationState state = stateCaptor.getValue();
            assertEquals(CHAT_ID, state.getChatId());
            assertEquals(TG_USERNAME, state.getTgUsername());
            assertEquals("Иванов", state.getLastName());
            assertEquals("Иван", state.getFirstName());
            assertEquals("Иванович", state.getMiddleName());
            assertEquals("Иванов Иван Иванович", state.getFullName());
            assertEquals(Gender.MALE, state.getGender());
            assertEquals("15.05.1990", state.getDateOfBirth());
            assertEquals("+79001234567", state.getPhoneNumber());
            assertEquals("ivan@example.com", state.getEmail());
            assertEquals(kindOfTourism, state.getKindOfTourism());
            assertEquals(grade, state.getGrade());
        }

        @Test
        void sendsSuccessMessageWithUsername() {
            String json = buildJson("Иванов", "Иван", "Иванович",
                    "MALE", "1990-05-15", "+79001234567", "ivan@example.com", 1, 2);

            miniAppHandler.submit(bot, CHAT_ID, TG_USERNAME, json);

            ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
            verify(bot).dispatch(messageCaptor.capture());
            String text = messageCaptor.getValue().getText();
            assertTrue(text.contains(TG_USERNAME));
            assertTrue(text.contains("ваша заявка принята"));
        }

        @Test
        void whenMiddleNameIsBlank_thenMiddleNameNullAndFullNameHasTwoParts() {
            String json = buildJson("Иванов", "Иван", "",
                    "MALE", "1990-05-15", "+79001234567", "ivan@example.com", 1, 2);

            miniAppHandler.submit(bot, CHAT_ID, TG_USERNAME, json);

            ArgumentCaptor<ConversationState> stateCaptor = ArgumentCaptor.forClass(ConversationState.class);
            verify(pendingTouristService).register(stateCaptor.capture());
            ConversationState state = stateCaptor.getValue();
            assertNull(state.getMiddleName());
            assertEquals("Иванов Иван", state.getFullName());
        }

        @Test
        void whenPhoneIsBlank_thenPhoneIsNull() {
            String json = buildJson("Иванов", "Иван", "Иванович",
                    "MALE", "1990-05-15", "", "ivan@example.com", 1, 2);

            miniAppHandler.submit(bot, CHAT_ID, TG_USERNAME, json);

            ArgumentCaptor<ConversationState> stateCaptor = ArgumentCaptor.forClass(ConversationState.class);
            verify(pendingTouristService).register(stateCaptor.capture());
            assertNull(stateCaptor.getValue().getPhoneNumber());
        }

        @Test
        void whenEmailIsBlank_thenEmailIsNull() {
            String json = buildJson("Иванов", "Иван", "Иванович",
                    "MALE", "1990-05-15", "+79001234567", "", 1, 2);

            miniAppHandler.submit(bot, CHAT_ID, TG_USERNAME, json);

            ArgumentCaptor<ConversationState> stateCaptor = ArgumentCaptor.forClass(ConversationState.class);
            verify(pendingTouristService).register(stateCaptor.capture());
            assertNull(stateCaptor.getValue().getEmail());
        }
    }

    @Nested
    class SubmitWithInvalidData {

        @Test
        void whenJsonIsMalformed_thenSendsErrorMessageAndSkipsRegistration() {
            miniAppHandler.submit(bot, CHAT_ID, TG_USERNAME, "not-valid-json{{{");

            verify(bot).send(eq(CHAT_ID), contains("Ошибка"));
            verify(pendingTouristService, never()).register(any());
        }

        @Test
        void whenDateFormatIsInvalid_thenSendsErrorMessageAndSkipsRegistration() {
            String json = buildJson("Иванов", "Иван", "Иванович",
                    "MALE", "15-05-1990", "+79001234567", "ivan@example.com", 1, 2);

            miniAppHandler.submit(bot, CHAT_ID, TG_USERNAME, json);

            verify(bot).send(eq(CHAT_ID), contains("Ошибка"));
            verify(pendingTouristService, never()).register(any());
        }
    }
}
