package org.tourism.instructors.application.user;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.tourism.instructors.api.user.dto.UserDTO;
import org.tourism.instructors.application.user.exception.UserNotFoundException;
import org.tourism.instructors.application.user.impl.UserServiceImpl;
import org.tourism.instructors.domain.user.User;
import org.tourism.instructors.domain.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserServiceImpl userService;

    // --- findAll ---

    @Nested
    class FindAll {

        @Test
        void delegatesToRepository() {
            User user = new User();
            when(userRepository.findAll()).thenReturn(List.of(user));

            List<User> result = userService.findAll();

            assertEquals(1, result.size());
            assertEquals(user, result.getFirst());
        }

        @Test
        void returnsEmptyListWhenNoUsers() {
            when(userRepository.findAll()).thenReturn(List.of());

            assertTrue(userService.findAll().isEmpty());
        }
    }

    // --- findById ---

    @Nested
    class FindById {

        @Test
        void returnsMappedDtoWhenFound() {
            User user = new User();
            user.setId(1);
            user.setName("alice");
            user.setRole("ADMIN");
            when(userRepository.findById(1)).thenReturn(Optional.of(user));

            UserDTO result = userService.findById(1);

            assertEquals(1, result.getId());
            assertEquals("alice", result.getName());
            assertEquals("ADMIN", result.getRole());
        }

        @Test
        void throwsWhenNotFound() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.findById(99));
        }
    }

    // --- createUser ---

    @Nested
    class CreateUser {

        @Test
        void encodesPasswordAndSaves() {
            UserDTO dto = new UserDTO();
            dto.setName("bob");
            dto.setPassword("secret");
            dto.setRole("USER");
            when(passwordEncoder.encode("secret")).thenReturn("hashed");

            userService.createUser(dto);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertEquals("bob", saved.getName());
            assertEquals("hashed", saved.getPassword());
            assertEquals("USER", saved.getRole());
        }

        @Test
        void doesNotSetId() {
            UserDTO dto = new UserDTO();
            dto.setName("carol");
            dto.setPassword("pass");
            dto.setRole("USER");
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");

            userService.createUser(dto);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertNull(captor.getValue().getId());
        }
    }

    // --- updateUser ---

    @Nested
    class UpdateUser {

        @Test
        void updatesRoleAndPasswordWhenPasswordProvided() {
            User user = new User();
            user.setId(1);
            user.setRole("USER");
            user.setPassword("old_hash");
            when(userRepository.findById(1)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newpass")).thenReturn("new_hash");

            UserDTO dto = new UserDTO();
            dto.setRole("ADMIN");
            dto.setPassword("newpass");

            userService.updateUser(1, dto);

            assertEquals("ADMIN", user.getRole());
            assertEquals("new_hash", user.getPassword());
            verify(userRepository).save(user);
        }

        @Test
        void updatesOnlyRoleWhenPasswordIsNull() {
            User user = new User();
            user.setId(1);
            user.setRole("USER");
            user.setPassword("old_hash");
            when(userRepository.findById(1)).thenReturn(Optional.of(user));

            UserDTO dto = new UserDTO();
            dto.setRole("ADMIN");
            dto.setPassword(null);

            userService.updateUser(1, dto);

            assertEquals("ADMIN", user.getRole());
            assertEquals("old_hash", user.getPassword());
            verify(passwordEncoder, never()).encode(any());
            verify(userRepository).save(user);
        }

        @Test
        void updatesOnlyRoleWhenPasswordIsBlank() {
            User user = new User();
            user.setId(1);
            user.setRole("USER");
            user.setPassword("old_hash");
            when(userRepository.findById(1)).thenReturn(Optional.of(user));

            UserDTO dto = new UserDTO();
            dto.setRole("ADMIN");
            dto.setPassword("   ");

            userService.updateUser(1, dto);

            assertEquals("ADMIN", user.getRole());
            assertEquals("old_hash", user.getPassword());
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        void throwsWhenUserNotFound() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            UserDTO dto = new UserDTO();
            assertThrows(UserNotFoundException.class, () -> userService.updateUser(99, dto));
        }
    }

    // --- deleteUser ---

    @Nested
    class DeleteUser {

        @Test
        void delegatesToRepository() {
            userService.deleteUser(5);
            verify(userRepository).deleteById(5);
        }
    }

    // --- changePassword ---

    @Nested
    class ChangePassword {

        @Test
        void encodesAndSavesNewPassword() {
            User user = new User();
            user.setName("dave");
            user.setPassword("old_hash");
            when(userRepository.findByName("dave")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newpass")).thenReturn("new_hash");

            userService.changePassword("dave", "newpass");

            assertEquals("new_hash", user.getPassword());
            verify(userRepository).save(user);
        }

        @Test
        void throwsWhenUsernameNotFound() {
            when(userRepository.findByName("ghost")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.changePassword("ghost", "pass"));
        }
    }
}