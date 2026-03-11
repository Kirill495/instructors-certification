package org.tourism.instructors.application.pending.impl;

import org.springframework.stereotype.Service;
import org.tourism.instructors.api.pending.mapper.PendingTouristMapper;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.domain.pending.ConversationState;
import org.tourism.instructors.domain.pending.PendingTourist;
import org.tourism.instructors.domain.pending.repository.PendingTouristRepository;
import org.tourism.instructors.domain.tourist.model.Tourist;
import org.tourism.instructors.domain.tourist.repository.TouristRepository;

import java.util.List;

@Service
public class PendingTouristServiceImpl implements PendingTouristService {

    private final PendingTouristRepository pendingTouristRepository;
    private final TouristRepository touristRepository;
    private final PendingTouristMapper mapper;

    public PendingTouristServiceImpl(PendingTouristRepository pendingTouristRepository,
                                     TouristRepository touristRepository,
                                     PendingTouristMapper mapper) {
        this.pendingTouristRepository = pendingTouristRepository;
        this.touristRepository = touristRepository;
        this.mapper = mapper;
    }


    @Override
    public void register(ConversationState state)  {
        pendingTouristRepository.save(mapper.toEntity(state));
    }

    @Override
    public PendingTourist getById(int id) {
        return findById(id);
    }

    @Override
    public PendingTourist linkTourist(int pendingId, int touristId) {
        PendingTourist pending = findById(pendingId);
        Tourist tourist = touristRepository.findById(touristId)
                .orElseThrow(() -> new IllegalArgumentException("Tourist not found: " + touristId));
        pending.setTourist(tourist);
        return pendingTouristRepository.save(pending);
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

    @Override
    public PendingTourist unlinkTourist(int pendingId) {
        PendingTourist pending = findById(pendingId);
        pending.setTourist(null);
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

    @Override
    public int countPending() {
        return pendingTouristRepository.countByStatus("PENDING");
    }
}