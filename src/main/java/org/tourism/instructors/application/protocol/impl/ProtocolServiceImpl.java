package org.tourism.instructors.application.protocol.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tourism.instructors.api.protocol.dto.ProtocolContentDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolForListDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolLightDTO;
import org.tourism.instructors.api.protocol.mapper.ProtocolMapper;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;
import org.tourism.instructors.application.protocol.ProtocolService;
import org.tourism.instructors.application.protocol.exception.ProtocolNotFoundException;
import org.tourism.instructors.domain.pending.PendingTourist;
import org.tourism.instructors.domain.protocol.Protocol;
import org.tourism.instructors.domain.protocol.ProtocolStatus;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;

import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@Transactional(readOnly = true)
public class ProtocolServiceImpl implements ProtocolService {

    private final ProtocolRepository protocolRepository;
    private final ProtocolMapper protocolMapper;

    public ProtocolServiceImpl (ProtocolRepository protocolRepository, ProtocolMapper protocolMapper) {
        this.protocolRepository = protocolRepository;
        this.protocolMapper = protocolMapper;
    }

    @Override
    public Page<ProtocolForListDTO> getProtocolsForList (String searchString, Pageable pageable) {
        Page<ProtocolRepository.ProtocolProjection> protocolProjectionPage;
        if (hasSearchParameter(searchString)) {
            protocolProjectionPage = protocolRepository.searchByTouristLastNameStartingWithIgnoreCase(searchString, pageable);
        } else {
            protocolProjectionPage = protocolRepository.findAllProtocols(pageable);
        }
        if (protocolProjectionPage.isEmpty()) {
            return Page.empty();
        }
        List<Integer> ids = protocolProjectionPage.getContent().stream().map(ProtocolRepository.ProtocolProjection::getId).toList();
        List<ProtocolForListDTO> protocols = protocolRepository.getProtocolWithContentByIDs(ids, pageable.getSort()).stream()
                                                     .map(protocolMapper::toProtocolForListDTO)
                                                     .toList();
        if (hasSearchParameter(searchString)) {
            sortTouristsInProtocol(searchString, protocols);
        }
        return new PageImpl<>(protocols, pageable, protocolProjectionPage.getTotalElements());
    }

    private static void sortTouristsInProtocol (String searchString, List<ProtocolForListDTO> protocols) {
        protocols.forEach(dto -> {
            Comparator<TouristLightDTO> comparator = Comparator.<TouristLightDTO, Boolean>comparing(
                    t -> Strings.CI.contains(t.getFullName(), searchString)).reversed();
            dto.getTourists().sort(comparator);
        });
    }

    private boolean hasSearchParameter (String searchString) {
        return StringUtils.isNoneBlank(searchString) && searchString.trim().length() > 2;
    }

    @Override
    public List<ProtocolLightDTO> searchDraftsByNumber(String query) {
        return protocolRepository.searchByNumberAndStatus(query, ProtocolStatus.DRAFT).stream()
                .map(protocolMapper::toLightDTO)
                .collect(toList());
    }

    @Override
    public List<ProtocolLightDTO> getLastDrafts() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("date").descending());
        return protocolRepository.getLast(ProtocolStatus.DRAFT, pageable).stream()
                .map(protocolMapper::toLightDTO)
                .collect(toList());
    }

    @Transactional
    @Override
    public void addTouristToProtocol(int protocolId, PendingTourist pending, int touristId) {
        ProtocolDTO protocol = getProtocolById(protocolId);
        int nextRow = protocol.getContentRows().stream()
                .mapToInt(row -> row.getRowNum() != null ? row.getRowNum() : 0)
                .max().orElse(0) + 1;
        ProtocolContentDTO content = new ProtocolContentDTO();
        content.setProtocolId(protocolId);
        content.setRowNum(nextRow);
        content.setTouristId(touristId);
        content.setKindOfTourismId(pending.getKindOfTourism().getId());
        content.setGradeId(pending.getGrade().getId());
        content.setCertificationId(pending.getCertificationId());
        protocol.getContentRows().add(content);
        saveProtocol(protocol);
    }

    @Override
    public int countProtocols () {
        return (int) protocolRepository.count();
    }

    @Override
    public int getProtocolIndex (int highLightedId) {
        return protocolRepository.countOfRowsBefore(highLightedId);
    }

    @Override
    public ProtocolDTO getProtocolById (int id) {
        return protocolRepository.findById(id).map(protocolMapper::toDTO).orElseThrow(() -> new RuntimeException("Протокол с ID:" + id + " не найден"));
    }

    @Transactional
    @Override
    public void saveProtocol (ProtocolDTO protocolDTO) {
        protocolRepository.save(protocolMapper.toEntity(protocolDTO));
    }

    @Transactional
    @Override
    public void deleteProtocol (int protocolId) {
        Protocol protocol = protocolRepository.findById(protocolId).orElseThrow(() -> new ProtocolNotFoundException(protocolId));
        protocolRepository.delete(protocol);
    }
}
