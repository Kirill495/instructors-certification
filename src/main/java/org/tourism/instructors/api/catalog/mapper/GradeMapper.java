package org.tourism.instructors.api.catalog.mapper;

import org.mapstruct.Mapper;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.domain.catalog.model.Grade;

@Mapper(componentModel = "spring")
public interface GradeMapper {
    GradeDTO toDTO(Grade entity);
    Grade toEntity (GradeDTO dto);
}
