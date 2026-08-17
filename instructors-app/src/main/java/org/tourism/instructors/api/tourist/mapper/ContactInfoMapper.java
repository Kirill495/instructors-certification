package org.tourism.instructors.api.tourist.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tourism.instructors.api.tourist.dto.ContactInfoItemDTO;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoItem;

@Mapper(componentModel = "spring")
public interface ContactInfoMapper {

    @Mapping(target = "details", ignore = true)
    @Mapping(target = "touristId", source = "tourist.id")
    ContactInfoItemDTO toDTO(ContactInfoItem item);

    @Mapping(target = "tourist.id", source = "touristId")
    ContactInfoItem toModel(ContactInfoItemDTO dto);
}
