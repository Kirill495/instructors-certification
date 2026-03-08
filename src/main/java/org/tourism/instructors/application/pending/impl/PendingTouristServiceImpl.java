package org.tourism.instructors.application.pending.impl;

import org.springframework.stereotype.Service;
import org.tourism.instructors.api.pending.mapper.PendingTouristMapper;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.domain.pending.ConversationState;
import org.tourism.instructors.domain.pending.PendingTourist;
import org.tourism.instructors.domain.pending.repository.PendingTouristRepository;

import java.util.List;

@Service
public class PendingTouristServiceImpl implements PendingTouristService {

    private final PendingTouristRepository pendingTouristRepository;
    private final PendingTouristMapper mapper;
    public PendingTouristServiceImpl(PendingTouristRepository pendingTouristRepository, PendingTouristMapper mapper) {
        this.pendingTouristRepository = pendingTouristRepository;
        this.mapper = mapper;
    }


    @Override
    public void register(ConversationState state)  {
        pendingTouristRepository.save(mapper.toEntity(state));
    }

    @Override
    public List<PendingTourist> findAllPending() {
        return pendingTouristRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    @Override
    public PendingTourist approve(int id) {
        PendingTourist pending = findById(id);
        pending.setStatus("APPROVED");
        return pendingTouristRepository.save(pending);
    }

    @Override
    public PendingTourist reject(int id) {
        PendingTourist pending = findById(id);
        pending.setStatus("REJECTED");
        return pendingTouristRepository.save(pending);
    }

    private PendingTourist findById(int id) {
        return pendingTouristRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pending tourist not found: " + id));
    }

    @Override
    public boolean existsByChatId (long chatId) {
        return pendingTouristRepository.existsByChatId(chatId);
    }
}