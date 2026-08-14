package org.tourism.instructors.api.enums;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class EnumsPresentationTest {

    @Mock MessageSource messageSource;

    @InjectMocks EnumsPresentation controller;

    final Locale locale = Locale.ENGLISH;

    @Nested
    class GetEnumLabels {

        @Test
        void unknownEnumClass_throwsNotFoundWithNameInReason() {
            ResponseStatusException ex =
                    assertThrows(
                            ResponseStatusException.class,
                            () -> controller.getEnumLabels("NonExistent", locale));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertTrue(ex.getReason().contains("NonExistent"));
        }

        @Test
        void gender_returnsTranslatedLabelsFromMessageSource() {
            when(messageSource.getMessage(eq("enum.Gender.MALE"), isNull(), eq("MALE"), eq(locale)))
                    .thenReturn("Мужской");
            when(messageSource.getMessage(
                            eq("enum.Gender.FEMALE"), isNull(), eq("FEMALE"), eq(locale)))
                    .thenReturn("Женский");

            Map<String, String> result = controller.getEnumLabels("Gender", locale);

            assertEquals(2, result.size());
            assertEquals("Мужской", result.get("MALE"));
            assertEquals("Женский", result.get("FEMALE"));
        }

        @Test
        void gender_whenMessageSourceReturnsNull_fallsBackToEnumName() {
            when(messageSource.getMessage(anyString(), isNull(), anyString(), any(Locale.class)))
                    .thenReturn(null);

            Map<String, String> result = controller.getEnumLabels("Gender", locale);

            assertEquals("MALE", result.get("MALE"));
            assertEquals("FEMALE", result.get("FEMALE"));
        }

        @Test
        void protocolStatus_isRegisteredAndReturnsAllConstants() {
            when(messageSource.getMessage(anyString(), isNull(), anyString(), any(Locale.class)))
                    .thenAnswer(inv -> inv.getArgument(2));

            Map<String, String> result = controller.getEnumLabels("ProtocolStatus", locale);

            assertEquals(2, result.size());
            assertTrue(result.containsKey("DRAFT"));
            assertTrue(result.containsKey("FINALIZED"));
        }

        @Test
        void contactInfoType_isRegisteredAndReturnsAllConstants() {
            when(messageSource.getMessage(anyString(), isNull(), anyString(), any(Locale.class)))
                    .thenAnswer(inv -> inv.getArgument(2));

            Map<String, String> result = controller.getEnumLabels("ContactInfoType", locale);

            assertEquals(3, result.size());
            assertTrue(result.containsKey("PHONE_NUMBER"));
            assertTrue(result.containsKey("EMAIL"));
            assertTrue(result.containsKey("TELEGRAM"));
        }
    }
}
