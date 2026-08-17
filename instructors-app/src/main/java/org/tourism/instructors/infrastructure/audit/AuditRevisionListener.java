package org.tourism.instructors.infrastructure.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditRevisionListener implements RevisionListener {
    @Override
    public void newRevision(Object revisionEntity) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userName = (auth != null) ? auth.getName() : "system";
        ((AuditRevision) revisionEntity).setUsername(userName);
    }
}
