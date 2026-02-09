package org.tourism.instructors.domain.protocol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.protocol.Protocol;

@Repository
public interface ProtocolRepository extends JpaRepository<Protocol, Integer> {
}
