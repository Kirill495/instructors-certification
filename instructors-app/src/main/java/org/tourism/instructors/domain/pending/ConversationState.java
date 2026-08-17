package org.tourism.instructors.domain.pending;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismDTO;
import org.tourism.instructors.domain.pending.repository.RegistrationStep;
import org.tourism.instructors.domain.tourist.model.Gender;

@Getter
@Setter
@NoArgsConstructor
public class ConversationState {
    public ConversationState(Long chatId, String tgUsername) {
        this.chatId = chatId;
        this.tgUsername = tgUsername;
    }

    private RegistrationStep registrationStep = RegistrationStep.NAME;
    private Long chatId;
    private String fullName;
    private String firstName;
    private String lastName;
    private String middleName;
    private String dateOfBirth;
    private String phoneNumber;
    private String email;
    private String certificationId;
    private KindOfTourismDTO kindOfTourism;
    private GradeDTO grade;
    private Gender gender;
    private String tgUsername;
    private boolean editing;
    private Integer summaryMessageId;
    private Integer editingPromptMessageId;
}
