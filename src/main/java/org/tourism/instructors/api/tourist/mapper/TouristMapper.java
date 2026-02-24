package org.tourism.instructors.api.tourist.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.tourism.instructors.api.protocol.mapper.GradeAssignmentMapper;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;
import org.tourism.instructors.domain.tourist.model.Tourist;

import java.time.LocalDate;
import java.util.List;

@Mapper(componentModel = "spring", uses = {GradeAssignmentMapper.class})
public interface TouristMapper {
    TouristLightDTO toLightDTO (Tourist model);
    Tourist toEntity (TouristDTO dto);
    TouristDTO toDTO (Tourist model);
    TouristDTO toDTO (Tourist model, List<ProtocolRepository.GradeAssignmentProjection> assignments);

    @AfterMapping
    default void mapValidAssignments(@MappingTarget TouristDTO dto) {
        if (dto.getAssignments() != null) {
            List<TouristDTO.AssignmentDTO> validAssignments = dto.getAssignments().stream().filter(a -> a.getValidThrough().isAfter(LocalDate.now())).toList();
            dto.setValidAssignments(validAssignments);
        }
    }
}
