package org.tourism.instructors.api.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.tourism.instructors.application.user.UserService;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock UserService userService;

    @Mock Authentication authentication;

    @InjectMocks ProfileController controller;

    @Nested
    class PasswordForm {

        @Test
        void get_returnsPasswordView() {
            assertEquals("profile/password", controller.passwordForm());
        }
    }

    @Nested
    class ChangePassword {

        @Test
        void passwordsMismatch_redirectsBackToPasswordForm() {
            RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

            String view = controller.changePassword("newPass", "different", authentication, attrs);

            assertEquals("redirect:/profile/password", view);
        }

        @Test
        void passwordsMismatch_addsErrorFlashAttribute() {
            RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

            controller.changePassword("newPass", "different", authentication, attrs);

            assertEquals("Пароли не совпадают", attrs.getFlashAttributes().get("errorMessage"));
        }

        @Test
        void passwordsMismatch_doesNotCallService() {
            controller.changePassword(
                    "newPass", "different", authentication, new RedirectAttributesModelMap());

            verify(userService, never()).changePassword(any(), any());
        }

        @Test
        void passwordsMatch_callsServiceWithUsernameAndNewPassword() {
            when(authentication.getName()).thenReturn("admin");

            controller.changePassword(
                    "secret", "secret", authentication, new RedirectAttributesModelMap());

            verify(userService).changePassword("admin", "secret");
        }

        @Test
        void passwordsMatch_redirectsToHome() {
            when(authentication.getName()).thenReturn("admin");
            RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

            String view = controller.changePassword("secret", "secret", authentication, attrs);

            assertEquals("redirect:/", view);
        }

        @Test
        void passwordsMatch_addsSuccessFlashAttribute() {
            when(authentication.getName()).thenReturn("admin");
            RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

            controller.changePassword("secret", "secret", authentication, attrs);

            assertEquals(
                    "Пароль успешно изменён", attrs.getFlashAttributes().get("successMessage"));
        }
    }
}
