package org.tourism.instructors.api.tourist.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tourism.instructors.api.tourist.dto.ContactInfoItemDTO;
import org.tourism.instructors.domain.tourist.model.Tourist;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoItem;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoType;
import org.tourism.instructors.domain.tourist.model.contactinfo.TelegramDetails;

@ExtendWith(SpringExtension.class)
@Import(ContactInfoMapperImpl.class)
class ContactInfoMapperTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    ContactInfoMapper mapper;

    @Test
    void toModel() {
        ContactInfoItemDTO dto =
                new ContactInfoItemDTO(
                        0,
                        0,
                        ContactInfoType.PHONE_NUMBER,
                        "111",
                        new TelegramDetails(1, "@tgNickname"));
        ContactInfoItem item = mapper.toModel(dto);
        assertEquals(dto.getType(), item.getType());
    }

    @Test
    void toModelWhenNull() {
        ContactInfoItem item = mapper.toModel(null);
        assertNull(item);
    }

    @Test
    void toDTO() {
        ContactInfoItem item = new ContactInfoItem();
        Tourist tourist = new Tourist();
        tourist.setId(1);
        item.setTourist(tourist);
        ContactInfoItemDTO dto = mapper.toDTO(item);
        assertEquals(1, dto.getTouristId());
    }

    @Test
    void toDTOWhenTouristIsNull() {
        ContactInfoItem item = new ContactInfoItem();
        ContactInfoItemDTO dto = mapper.toDTO(item);
        assertNull(dto.getTouristId());
    }

    @Test
    void toDTOWhenTouristIdIsNull() {
        ContactInfoItem item = new ContactInfoItem();
        Tourist tourist = new Tourist();

        item.setTourist(tourist);
        ContactInfoItemDTO dto = mapper.toDTO(item);
        assertNull(dto.getTouristId());
    }

    @Test
    void toDTOWhenNull() {
        ContactInfoItemDTO dto = mapper.toDTO(null);
        assertNull(dto);
    }
}
