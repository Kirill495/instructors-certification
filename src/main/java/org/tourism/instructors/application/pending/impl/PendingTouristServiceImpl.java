package org.tourism.instructors.application.pending.impl;

import org.springframework.stereotype.Service;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.domain.pending.PendingTourist;
import org.tourism.instructors.domain.pending.repository.PendingTouristRepository;
import org.tourism.instructors.domain.tourist.model.Tourist;
import org.tourism.instructors.domain.tourist.repository.TouristRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PendingTouristServiceImpl implements PendingTouristService {

    private final PendingTouristRepository pendingTouristRepository;
    private final TouristRepository touristRepository;

    public PendingTouristServiceImpl(PendingTouristRepository pendingTouristRepository,
                                     TouristRepository touristRepository) {
        this.pendingTouristRepository = pendingTouristRepository;
        this.touristRepository = touristRepository;
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public void register(Long chatId, String tgUsername, String lastName, String firstName, String middleName,
                         String dateOfBirth, String email, String phoneNumber) {
        PendingTourist pending = new PendingTourist();
        pending.setChatId(chatId);
        pending.setTgUsername(tgUsername);
        pending.setLastName(lastName);
        pending.setFirstName(firstName);
        pending.setMiddleName(middleName);
        pending.setDateOfBirth(dateOfBirth != null ? LocalDate.parse(dateOfBirth, DATE_FORMAT) : null);
        pending.setEmail(email);
        pending.setPhoneNumber(phoneNumber);
        pendingTouristRepository.save(pending);
    }

    @Override
    public List<PendingTourist> findAllPending() {
        return pendingTouristRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    @Override
    public PendingTourist approve(int id) {
        PendingTourist pending = findById(id);

        Tourist tourist = new Tourist();
        tourist.setLastName(pending.getLastName());
        tourist.setFirstName(pending.getFirstName());
        tourist.setMiddleName(pending.getMiddleName());
        tourist.setDateOfBirth(pending.getDateOfBirth());
        tourist.setEmail(pending.getEmail());
        tourist.setPhoneNumber(pending.getPhoneNumber());
        touristRepository.save(tourist);

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