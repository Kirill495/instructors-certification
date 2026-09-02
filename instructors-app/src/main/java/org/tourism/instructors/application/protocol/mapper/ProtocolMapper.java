package org.tourism.instructors.application.protocol.mapper;

import org.mapstruct.Mapper;
import org.tourism.instructors.domain.protocol.Protocol;
import org.tourism.publication.contract.ProtocolSnapshot;

@Mapper
public interface ProtocolMapper {

    ProtocolSnapshot toSnapshot(Protocol protocol);
}
