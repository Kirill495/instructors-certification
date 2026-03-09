package org.tourism.instructors.application.protocol;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolForListDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolLightDTO;
import org.tourism.instructors.domain.pending.PendingTourist;

import java.util.List;

public interface ProtocolService {
    Page<ProtocolForListDTO> getProtocolsForList(String searchString, Pageable pageable);
    List<ProtocolLightDTO> searchByNumber(String query);
    ProtocolDTO getProtocolById(int id);
    void deleteProtocol(int protocolId);
    void saveProtocol(ProtocolDTO protocolDTO);
    void addTouristToProtocol(int protocolId, PendingTourist pending, int touristId);
    int countProtocols();
    int getProtocolIndex(int highLightedId);
}
