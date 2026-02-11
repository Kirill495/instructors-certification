package org.tourism.instructors.application.protocol;

import org.tourism.instructors.api.protocol.dto.ProtocolDTO;

import java.util.List;

public interface ProtocolService {
    List<ProtocolDTO> getProtocols();

    ProtocolDTO getProtocolById (int id);

    void saveProtocol (ProtocolDTO protocolDTO);

    int countProtocols();
}
