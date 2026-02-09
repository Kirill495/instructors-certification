package org.tourism.instructors.application.protocol.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
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
    public List<ProtocolDTO> getProtocols () {
        return protocolRepository.findAll().stream().map(protocolMapper::toDTO).toList();
    }

    @Override
    public ProtocolDTO getProtocolById (int id) {
        return protocolRepository.findById(id).map(protocolMapper::toDTO).orElseThrow(() -> new RuntimeException("Протокол с ID:" + id + " не найден"));
    }

    @Override
    public void saveProtocol (ProtocolDTO protocolDTO) {
        protocolRepository.save(protocolMapper.toEntity(protocolDTO));
    }
}
