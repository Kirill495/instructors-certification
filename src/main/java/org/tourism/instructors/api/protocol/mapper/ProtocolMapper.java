package org.tourism.instructors.api.protocol.mapper;

import org.mapstruct.Mapper;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
import org.tourism.instructors.domain.protocol.Protocol;

@Mapper(componentModel = "spring")
public interface ProtocolMapper {
    ProtocolDTO toDTO(Protocol protocol);
    Protocol toEntity (ProtocolDTO protocolDTO);
}
