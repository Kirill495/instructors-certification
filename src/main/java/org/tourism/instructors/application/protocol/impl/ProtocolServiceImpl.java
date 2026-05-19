package org.tourism.instructors.application.protocol.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tourism.instructors.api.protocol.dto.*;
import org.tourism.instructors.api.protocol.mapper.ProtocolMapper;
import org.tourism.instructors.application.protocol.ProtocolService;
import org.tourism.instructors.application.protocol.exception.ProtocolNotFoundException;
import org.tourism.instructors.domain.pending.PendingTourist;
import org.tourism.instructors.domain.protocol.Protocol;
import org.tourism.instructors.domain.protocol.ProtocolContent;
import org.tourism.instructors.domain.protocol.ProtocolStatus;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProtocolServiceImpl implements ProtocolService {

    private final ProtocolRepository protocolRepository;
    private final ProtocolMapper protocolMapper;

    public ProtocolServiceImpl(ProtocolRepository protocolRepository, ProtocolMapper protocolMapper) {
        this.protocolRepository = protocolRepository;
        this.protocolMapper = protocolMapper;
    }

    @Override
    public Page<ProtocolForListDTO> getProtocolsForList(String searchString, Pageable pageable) {
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
        List<Protocol> protocols = protocolRepository.getProtocolWithContentByIDs(ids, pageable.getSort());
        if (hasSearchParameter(searchString)) {
            sortTouristsInProtocol(searchString, protocols);
        }
        List<ProtocolForListDTO> protocolDTOs = protocols.stream()
                .map(protocolMapper::toProtocolForListDTO)
                .toList();
        return new PageImpl<>(protocolDTOs, pageable, protocolProjectionPage.getTotalElements());
    }

    private static void sortTouristsInProtocol(String searchString, List<Protocol> protocols) {
        Comparator<ProtocolContent> comparator = Comparator.<ProtocolContent, Boolean>comparing(
                c -> Strings.CI.contains(c.getTourist().getTitle(), searchString)).reversed();
        protocols.forEach(p -> p.getProtocolContents().sort(comparator));
    }

    private boolean hasSearchParameter(String searchString) {
        return StringUtils.isNoneBlank(searchString) && searchString.trim().length() > 2;
    }

    @Override
    public List<ProtocolLiteDTO> searchDraftsByNumber(String query) {
        return protocolRepository.searchByNumberAndStatus(query, ProtocolStatus.DRAFT).stream().map(protocolMapper::toLightDTO).toList();
    }

    @Override
    public List<ProtocolLiteDTO> getLastDrafts() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("date").descending());
        return protocolRepository.getLast(ProtocolStatus.DRAFT, pageable).stream()
                .map(protocolMapper::toLightDTO).toList();
    }

    @Transactional
    @Override
    public void addTouristToProtocol(int protocolId, PendingTourist pending, int touristId) {
        ProtocolFormDTO protocol = getProtocolFormById(protocolId);
        int nextRow = protocol.getContentRows().stream()
                .mapToInt(row -> row.getRowNum() != null ? row.getRowNum() : 0)
                .max().orElse(0) + 1;
        ProtocolContentFormRow content = new ProtocolContentFormRow(
                protocolId, nextRow,
                touristId, "",
                pending.getKindOfTourism().getId(),
                pending.getGrade().getId(),
                pending.getCertificationId(),
                ""
        );
        protocol.getContentRows().add(content);
        saveProtocolInner(protocol);
    }

    @Override
    public int countProtocols() {
        return (int) protocolRepository.count();
    }

    @Override
    public int getProtocolIndex(int highLightedId) {
        return protocolRepository.countOfRowsBefore(highLightedId);
    }

    @Override
    public ProtocolDTO getProtocolById(int id) {
        return protocolRepository.findById(id).map(protocolMapper::toDTO).orElseThrow(() -> new ProtocolNotFoundException(id));
    }

    @Override
    public ProtocolFormDTO getProtocolFormById(int id) {
        return protocolRepository.findById(id).map(protocolMapper::toFormDTO).orElseThrow(() -> new ProtocolNotFoundException(id));
    }

    @Transactional
    @Override
    public void saveProtocol(ProtocolFormDTO protocolFormDTO) {
        saveProtocolInner(protocolFormDTO);
    }

    @Transactional
    @Override
    public void deleteProtocol(int protocolId) {
        Protocol protocol = protocolRepository.findById(protocolId).orElseThrow(() -> new ProtocolNotFoundException(protocolId));
        protocolRepository.delete(protocol);
    }

    private void saveProtocolInner(ProtocolFormDTO protocolFormDTO) {
        protocolRepository.save(protocolMapper.toEntity(protocolFormDTO));
    }
}