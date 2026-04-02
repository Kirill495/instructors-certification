package org.tourism.instructors.application.tourist.impl;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.api.tourist.dto.TouristSummaryDTO;
import org.tourism.instructors.api.tourist.mapper.TouristMapper;
import org.tourism.instructors.application.tourist.TouristService;
import org.tourism.instructors.application.tourist.exception.TouristCannotBeDeletedException;
import org.tourism.instructors.application.tourist.exception.TouristNotFoundException;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;
import org.tourism.instructors.domain.tourist.model.Tourist;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoItem;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoType;
import org.tourism.instructors.domain.tourist.repository.ContactInfoRepository;
import org.tourism.instructors.domain.tourist.repository.TouristRepository;

import java.util.*;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
public class TouristServiceImpl implements TouristService {

    private final TouristRepository touristRepository;
    private final ProtocolRepository protocolRepository;
    private final TouristMapper touristMapper;
    private final ContactInfoRepository contactInfoRepository;

    public TouristServiceImpl(TouristRepository touristRepository,
                              ProtocolRepository protocolRepository,
                              TouristMapper touristMapper,
                              ContactInfoRepository contactInfoRepository) {
        this.touristRepository = touristRepository;
        this.protocolRepository = protocolRepository;
        this.touristMapper = touristMapper;
        this.contactInfoRepository = contactInfoRepository;
    }

    @Override
    public int countTourists() {
        return (int) touristRepository.count();
    }

    @Override
    public Page<TouristDTO> getAllTourists(Pageable pageable) {
        Page<Tourist> touristsPage = touristRepository.findAll(pageable);
        Map<Integer, List<ProtocolRepository.GradeAssignmentProjection>> assignments = getAssignments(touristsPage.getContent());
        List<TouristDTO> touristDTOs = touristsPage.stream()
                .map(tourist -> touristMapper.toDTO(tourist, getTouristAssignments(tourist, assignments)))
                .toList();
        return new PageImpl<>(touristDTOs, pageable, touristsPage.getTotalElements());
    }

    @Override
    public List<TouristSummaryDTO> searchLightTourists(String query) {
        List<String> queryParts = Arrays.asList(query.split(" "));
        return searchTouristsInnerByFullName(queryParts).stream().map(touristMapper::toLightDTO).toList();
    }

    @Override
    public Page<TouristDTO> searchTourists(String query, Pageable pageable) {

        List<Tourist> tourists = searchTouristsInner(query);
        Map<Integer, List<ProtocolRepository.GradeAssignmentProjection>> assignments = getAssignments(tourists);
        List<TouristDTO> touristDTOs = tourists.stream().map(tourist -> touristMapper.toDTO(tourist, getTouristAssignments(tourist, assignments))).toList();
        return new PageImpl<>(touristDTOs, pageable, touristDTOs.size());
    }

    @Transactional
    @Override
    public void delete(int touristId) {
        if (!protocolRepository.getAssignments(List.of(touristId)).isEmpty()) {
            throw new TouristCannotBeDeletedException("У туриста есть присвоения званий. Невозможно удалить.", touristId);
        }
        touristRepository.deleteById(touristId);
    }

    @Transactional
    @Override
    public void save(TouristDTO touristDTO) {
        Tourist tourist = touristMapper.toEntity(touristDTO);
        tourist.getContactInfo().forEach(item -> item.setTourist(tourist));
        touristRepository.save(tourist);
    }

    @Transactional
    @Override
    public TouristSummaryDTO saveAndReturn(TouristDTO touristDTO) {
        Tourist tourist = touristMapper.toEntity(touristDTO);
        if (tourist.getContactInfo() != null) {
            tourist.getContactInfo().forEach(item -> item.setTourist(tourist));
        }
        Tourist saved = touristRepository.save(tourist);
        return touristMapper.toLightDTO(saved);
    }

    @Override
    public TouristDTO findTouristById(int id) {
        Tourist tourist = touristRepository.findById(id).orElseThrow(() -> new TouristNotFoundException(id));
        List<ProtocolRepository.GradeAssignmentProjection> assignments = protocolRepository.getAssignments(List.of(id));
        List<ContactInfoItem> contactInfo = getContactInfo(tourist);
        return touristMapper.toDTO(tourist, assignments, contactInfo);
    }

    @Override
    public Optional<TouristDTO> findTouristByTelegramId(long chatId) {
        return contactInfoRepository.findTouristIdByTypeAndValue(ContactInfoType.TELEGRAM, String.valueOf(chatId))
                .map(ContactInfoItem::getTourist)
                .map(touristMapper::toDTO);
    }

    private @NonNull List<ContactInfoItem> getContactInfo(Tourist tourist) {
        return contactInfoRepository.findAllByTouristId(tourist.getId())
                .stream()
                .sorted(Comparator.comparing(ContactInfoItem::getType).thenComparing(ContactInfoItem::getValue))
                .toList();
    }

    private static @NonNull List<ProtocolRepository.GradeAssignmentProjection> getTouristAssignments(Tourist tourist, Map<Integer, List<ProtocolRepository.GradeAssignmentProjection>> assignments) {
        return assignments.getOrDefault(tourist.getId(), Collections.emptyList())
                .stream()
                .sorted(Comparator.comparing(ProtocolRepository.GradeAssignmentProjection::getProtocolDate))
                .toList();
    }

    private @NonNull Map<Integer, List<ProtocolRepository.GradeAssignmentProjection>> getAssignments(List<Tourist> tourists) {
        return protocolRepository
                .getAssignments(tourists.stream().map(Tourist::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(ProtocolRepository.GradeAssignmentProjection::getTouristId));
    }

    private List<Tourist> searchTouristsInner(String query) {
        if (containsOnlyDigits(query)) {
            return touristRepository.searchByCertificationId(query);
        } else {
            return touristRepository.searchByLastNameStartingWithIgnoreCase(query);
        }
    }

    private List<Tourist> searchTouristsInnerByFullName(List<String> fullNameParts) {
        if (fullNameParts.size() == 3) {
            return touristRepository.searchByLastNameStartingWithIgnoreCaseAndFirstNameStartingWithIgnoreCaseAndMiddleNameStartingWithIgnoreCase(fullNameParts.getFirst(), fullNameParts.get(1), fullNameParts.getLast());
        } else if (fullNameParts.size() == 2) {
            return touristRepository.searchByLastNameStartingWithIgnoreCaseAndFirstNameStartingWithIgnoreCase(fullNameParts.getFirst(), fullNameParts.getLast());
        } else {
            return searchTouristsInner(fullNameParts.getFirst());
        }
    }

    private boolean containsOnlyDigits(String str) {
        return str.matches("^\\d+$");
    }

}
