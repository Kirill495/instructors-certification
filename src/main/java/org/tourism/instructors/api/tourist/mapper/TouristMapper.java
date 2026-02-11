package org.tourism.instructors.api.tourist.mapper;

import org.mapstruct.Mapper;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.domain.tourist.model.Tourist;

@Mapper(componentModel = "spring")
public interface TouristMapper {
    TouristLightDTO toLightDTO (Tourist model);
    TouristDTO toDTO (Tourist model);
    Tourist toEntity (TouristDTO dto);
}
