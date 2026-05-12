package org.tourism.instructors.infrastructure.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationEventListenerTest {

    @Mock
    Authentication authentication;

    final AuthenticationEventListener listener = new AuthenticationEventListener();

    @Test
    void onSuccess_accessesAuthenticationNameForLogging() {
        when(authentication.getName()).thenReturn("alice");
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        assertDoesNotThrow(() -> listener.onSuccess(event));

        verify(authentication).getName();
    }

    @Test
    void onFailure_accessesAuthenticationNameForLogging() {
        when(authentication.getName()).thenReturn("bob");
        AbstractAuthenticationFailureEvent event = mock(AbstractAuthenticationFailureEvent.class);
        when(event.getAuthentication()).thenReturn(authentication);

        assertDoesNotThrow(() -> listener.onFailure(event));

        verify(authentication).getName();
    }
}