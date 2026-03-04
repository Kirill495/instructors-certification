package org.tourism.instructors.application.pending;

import org.tourism.instructors.domain.pending.PendingTourist;

import java.util.List;

public interface PendingTouristService {
    void register(Long chatId, String tgUsername, String lastName, String firstName, String middleName,
                  String dateOfBirth, String email, String phoneNumber);
    List<PendingTourist> findAllPending();
    PendingTourist approve(int id);
    PendingTourist reject(int id);

    boolean existsByChatId (long chatId);
}