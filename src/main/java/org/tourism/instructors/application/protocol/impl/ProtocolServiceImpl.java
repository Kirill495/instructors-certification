package org.tourism.instructors.application.protocol.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolForListDTO;
import org.tourism.instructors.api.protocol.mapper.ProtocolMapper;
import org.tourism.instructors.application.protocol.ProtocolService;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;

import java.util.List;

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
        Page<Integer> protocolIdPage;
        if (searchString != null && !searchString.trim().isEmpty()) {
            protocolIdPage = protocolRepository.searchByTouristLastNameStartingWithIgnoreCase(searchString, pageable);
        } else {
            protocolIdPage = protocolRepository.findAllProtocols(pageable);
        }
        if (protocolIdPage.isEmpty()) {
            return Page.empty();
        }
        List<ProtocolForListDTO> protocols = protocolRepository.getProtocolWithContentByIDs(protocolIdPage.getContent(), pageable.getSort()).stream().map(protocolMapper::toProtocolForListDTO).toList();
        return new PageImpl<>(protocols, pageable, protocolIdPage.getTotalElements());
    }

    @Override
    public int countProtocols () {
        return (int) protocolRepository.count();
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
}
