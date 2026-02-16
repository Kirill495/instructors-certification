package org.tourism.instructors.application.protocol;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolForListDTO;

public interface ProtocolService {
    Page<ProtocolForListDTO> getProtocolsForList (String searchString, Pageable pageable);

    ProtocolDTO getProtocolById (int id);

    void saveProtocol (ProtocolDTO protocolDTO);

    int countProtocols();

}
