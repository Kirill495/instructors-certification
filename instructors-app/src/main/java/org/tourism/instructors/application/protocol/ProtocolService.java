package org.tourism.instructors.application.protocol;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolForListDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolFormDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolLiteDTO;
import org.tourism.instructors.domain.pending.PendingTourist;

public interface ProtocolService {
    Page<ProtocolForListDTO> getProtocolsForList(String searchString, Pageable pageable);

    List<ProtocolLiteDTO> searchDraftsByNumber(String query);

    List<ProtocolLiteDTO> getLastDrafts();

    ProtocolDTO getProtocolById(int id);

    ProtocolFormDTO getProtocolFormById(int id);

    void deleteProtocol(int protocolId);

    void saveProtocol(ProtocolFormDTO protocolFormDTO);

    void addTouristToProtocol(int protocolId, PendingTourist pending, int touristId);

    int countProtocols();

    int getProtocolIndex(int highLightedId);
}
