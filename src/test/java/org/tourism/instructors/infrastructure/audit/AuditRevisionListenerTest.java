package org.tourism.instructors.infrastructure.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuditRevisionListenerTest {

    @Mock Authentication authentication;

    final AuditRevisionListener listener = new AuditRevisionListener();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void newRevision_withAuthenticatedUser_setsUsernameFromAuthentication() {
        when(authentication.getName()).thenReturn("testUser");
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        AuditRevision revision = new AuditRevision();
        listener.newRevision(revision);

        assertEquals("testUser", revision.getUsername());
    }

    @Test
    void newRevision_withNullAuthentication_setsSystemAsUsername() {
        SecurityContextHolder.clearContext();

        AuditRevision revision = new AuditRevision();
        listener.newRevision(revision);

        assertEquals("system", revision.getUsername());
    }
}
