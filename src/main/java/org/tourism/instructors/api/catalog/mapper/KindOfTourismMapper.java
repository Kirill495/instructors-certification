package org.tourism.instructors.api.catalog.mapper;

import org.mapstruct.Mapper;
import org.tourism.instructors.api.catalog.dto.KindOfTourismDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismListDTO;
import org.tourism.instructors.domain.catalog.model.KindOfTourism;

@Mapper(componentModel = "spring")
public interface KindOfTourismMapper {
    KindOfTourismDTO toDTO(KindOfTourism entity);
    KindOfTourismListDTO toListDTO(KindOfTourism entity);
    KindOfTourism toEntity(KindOfTourismDTO dto);

}
