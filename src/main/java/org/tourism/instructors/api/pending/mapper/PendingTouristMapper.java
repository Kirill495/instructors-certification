package org.tourism.instructors.api.pending.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tourism.instructors.domain.pending.ConversationState;
import org.tourism.instructors.domain.pending.PendingTourist;

@Mapper(componentModel = "spring")
public interface PendingTouristMapper {
    @Mapping(target = "dateOfBirth", dateFormat = "dd.MM.yyyy", source = "dateOfBirth")
    PendingTourist toEntity(ConversationState conversation);
}
