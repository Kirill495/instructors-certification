package org.tourism.instructors.application.protocol;

import org.jspecify.annotations.Nullable;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolForListDTO;

import java.util.List;

public interface ProtocolService {
    List<ProtocolForListDTO> getProtocolsForList ();

    ProtocolDTO getProtocolById (int id);

    void saveProtocol (ProtocolDTO protocolDTO);

    int countProtocols();

    List<ProtocolForListDTO> searchProtocols (String search);
}
