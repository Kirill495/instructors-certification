package org.tourism.instructors.application.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.tourism.instructors.application.user.impl.UserDetailsServiceImpl;
import org.tourism.instructors.domain.user.User;
import org.tourism.instructors.domain.user.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserDetailsServiceImpl userDetailsService;

    @Test
    void returnsUserDetailsWithCorrectFields() {
        User user = new User();
        user.setName("alice");
        user.setPassword("hashed_password");
        user.setRole("ADMIN");
        when(userRepository.findByName("alice")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("alice");

        assertEquals("alice", result.getUsername());
        assertEquals("hashed_password", result.getPassword());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN")));
    }

    @Test
    void throwsUsernameNotFoundExceptionWhenUserMissing() {
        when(userRepository.findByName("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("ghost"));
    }
}