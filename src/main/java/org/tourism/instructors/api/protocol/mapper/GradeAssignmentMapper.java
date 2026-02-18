package org.tourism.instructors.api.protocol.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tourism.instructors.api.catalog.mapper.GradeMapper;
import org.tourism.instructors.api.catalog.mapper.KindOfTourismMapper;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;

@Mapper(componentModel = "spring", uses = {GradeMapper.class, KindOfTourismMapper.class})
public interface GradeAssignmentMapper {

    @Mapping(target = "date", expression = "java(model.getProtocolDate())")
    @Mapping(target = "validThrough", expression = "java(model.getProtocolDate().plusYears(model.getGrade().getExpiresInYears()))")
    TouristDTO.AssignmentDTO toDTO(ProtocolRepository.GradeAssignmentProjection model);
}
