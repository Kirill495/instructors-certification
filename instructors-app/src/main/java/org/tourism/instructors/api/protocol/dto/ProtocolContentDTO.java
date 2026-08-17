package org.tourism.instructors.api.protocol.dto;

public record ProtocolContentDTO(
        Integer protocolId,
        Integer rowNum,
        Ref tourist,
        Ref grade,
        Ref kindOfTourism,
        String certificationId,
        String club) {
    public record Ref(Integer id, String title) {}
}
