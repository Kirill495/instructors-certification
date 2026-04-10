package org.tourism.instructors.api.tourist.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tourism.instructors.api.tourist.dto.ContactInfoItemDTO;
import org.tourism.instructors.domain.tourist.model.Tourist;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoItem;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoType;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import(ContactInfoMapperImpl.class)
class ContactInfoMapperTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    ContactInfoMapper mapper;

    @Test
    void toModel() {
        ContactInfoItemDTO dto = new ContactInfoItemDTO(ContactInfoType.PHONE_NUMBER, "111");
        ContactInfoItem item = mapper.toModel(dto);
        assertEquals(dto.type(), item.getType());
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
        assertEquals(1, dto.touristId());
    }

    @Test
    void toDTOWhenTouristIsNull() {
        ContactInfoItem item = new ContactInfoItem();
        ContactInfoItemDTO dto = mapper.toDTO(item);
        assertNull(dto.touristId());
    }

    @Test
    void toDTOWhenTouristIdIsNull() {
        ContactInfoItem item = new ContactInfoItem();
        Tourist tourist = new Tourist();

        item.setTourist(tourist);
        ContactInfoItemDTO dto = mapper.toDTO(item);
        assertNull(dto.touristId());
    }

    @Test
    void toDTOWhenNull() {
        ContactInfoItemDTO dto = mapper.toDTO(null);
        assertNull(dto);
    }
}