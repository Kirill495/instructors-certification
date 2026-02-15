package org.tourism.instructors.api.protocol.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tourism.instructors.api.protocol.dto.ProtocolContentDTO;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;
import org.tourism.instructors.domain.protocol.ProtocolContent;
import org.tourism.instructors.domain.tourist.model.Tourist;

@Mapper(componentModel = "spring")
public interface ProtocolContentMapper {

    @Mapping(target = "protocolId", source = "id.protocolId")
    @Mapping(target = "rowNum", source = "id.rowNum")
    @Mapping(target = "touristId", source = "tourist.id")
    @Mapping(target = "kindOfTourismId", source = "kindOfTourism.id")
    @Mapping(target = "gradeId", source = "grade.id")
    @Mapping(target = "touristFullName", expression = "java(model.getTourist() != null ? model.getTourist().getFullName() : null)")
    @Mapping(target = "kindOfTourismTitle", source = "kindOfTourism.title")
    @Mapping(target = "gradeTitle", source = "grade.title")
    @Mapping(target = "certificationId", source = "certificationID")
    ProtocolContentDTO toDTO(ProtocolContent model);

    @Mapping(target = "id.protocolId", source = "protocolId")
    @Mapping(target = "id.rowNum", source = "rowNum")
    @Mapping(target = "tourist.id", source = "touristId")
    @Mapping(target = "kindOfTourism.id", source = "kindOfTourismId")
    @Mapping(target = "grade.id", source = "gradeId")
    @Mapping(target = "certificationID", source = "certificationId")
    @Mapping(target = "protocol", ignore = true)
    ProtocolContent toEntity (ProtocolContentDTO dto);


    @Mapping(target = "id", source = "model.tourist.id")
    @Mapping(target = "lastName", source = "model.tourist.lastName")
    @Mapping(target = "firstName", source = "model.tourist.firstName")
    @Mapping(target = "middleName", source = "model.tourist.middleName")
    TouristLightDTO toTouristDTO(ProtocolContent model);
}
