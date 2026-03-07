package org.tourism.instructors.api.tourist.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tourism.instructors.api.protocol.mapper.GradeAssignmentMapper;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;
import org.tourism.instructors.domain.pending.PendingTourist;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;
import org.tourism.instructors.domain.tourist.model.Tourist;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoItem;

import java.util.List;

@Mapper(componentModel = "spring", uses = {GradeAssignmentMapper.class})
public interface TouristMapper {
    TouristLightDTO toLightDTO (Tourist model);
    Tourist toEntity (TouristDTO dto);
    TouristDTO toDTO (Tourist model);
    TouristDTO toDTO (Tourist model, List<ProtocolRepository.GradeAssignmentProjection> assignments);
    @Mapping(target = "id", ignore = true)
    TouristDTO toDTO(PendingTourist pending);

    TouristDTO toDTO (Tourist model,
                      List<ProtocolRepository.GradeAssignmentProjection> assignments,
                      List<ContactInfoItem> contactInfo);
}
