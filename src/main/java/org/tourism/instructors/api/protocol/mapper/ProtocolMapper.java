package org.tourism.instructors.api.protocol.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolForListDTO;
import org.tourism.instructors.domain.protocol.Protocol;
import org.tourism.instructors.domain.protocol.ProtocolContent;

@Mapper(componentModel = "spring", uses = {ProtocolContentMapper.class})
public interface ProtocolMapper {

    @Mapping(target = "contentRows", source = "protocolContents")
    ProtocolDTO toDTO(Protocol protocol);

    @Mapping(target = "protocolContents", source = "contentRows")
    Protocol toEntity (ProtocolDTO protocolDTO);

    @AfterMapping
    default void setProtocolRef(@MappingTarget Protocol protocol) {
        if (protocol.getProtocolContents() != null) {
            for (ProtocolContent protocolContent : protocol.getProtocolContents()) {
                protocolContent.setProtocol(protocol);
            }
        }
    }

    @Mapping(target = "tourists", source="protocolContents")
    ProtocolForListDTO toProtocolForListDTO(Protocol protocol);
}
