package org.tourism.instructors.api.user;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.api.user.dto.UserDTO;
import org.tourism.instructors.application.user.UserService;
import org.tourism.instructors.domain.user.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    UserService userService;

    @Mock
    RedirectAttributes redirectAttributes;

    @InjectMocks
    UserController controller;

    @Nested
    class ListUsersTest {

        Model model = new ExtendedModelMap();

        @Test
        void returnsListView() {
            when(userService.findAll()).thenReturn(List.of());

            String view = controller.listUsers(model);

            assertEquals("admin/users/list", view);
        }

        @Test
        void putsUsersIntoModel() {
            List<User> users = List.of(new User(), new User());
            when(userService.findAll()).thenReturn(users);

            controller.listUsers(model);

            assertEquals(users, model.getAttribute("users"));
        }
    }

    @Nested
    class NewUserFormTest {

        Model model = new ExtendedModelMap();

        @Test
        void returnsEditTemplateWithEmptyUser() {
            String view = controller.newUserForm(model);

            assertEquals("admin/users/edit", view);
            UserDTO user = (UserDTO) model.getAttribute("user");
            assertNotNull(user);
            assertTrue(user.isNewItem());
        }
    }

    @Nested
    class CreateUserTest {

        @Test
        void createsUserAndRedirects() {
            UserDTO dto = new UserDTO();

            String view = controller.createUser(dto, redirectAttributes);

            verify(userService).createUser(dto);
            assertEquals("redirect:/admin/users", view);
        }

        @Test
        void addsSuccessFlashAttribute() {
            controller.createUser(new UserDTO(), redirectAttributes);

            verify(redirectAttributes).addFlashAttribute("successMessage", "Пользователь успешно создан");
        }
    }

    @Nested
    class EditUserFormTest {

        Model model = new ExtendedModelMap();

        @Test
        void returnsEditTemplateWithUser() {
            UserDTO dto = new UserDTO();
            dto.setId(5);
            when(userService.findById(5)).thenReturn(dto);

            String view = controller.editUserForm(5, model);

            assertEquals("admin/users/edit", view);
            assertEquals(dto, model.getAttribute("user"));
        }
    }

    @Nested
    class UpdateUserTest {

        @Test
        void updatesUserWithPathVariableIdAndRedirects() {
            UserDTO dto = new UserDTO();

            String view = controller.updateUser(3, dto, redirectAttributes);

            verify(userService).updateUser(3, dto);
            assertEquals("redirect:/admin/users", view);
        }

        @Test
        void addsSuccessFlashAttribute() {
            controller.updateUser(3, new UserDTO(), redirectAttributes);

            verify(redirectAttributes).addFlashAttribute("successMessage", "Пользователь успешно обновлён");
        }
    }

    @Nested
    class DeleteUserTest {

        @Test
        void deletesUserAndRedirects() {
            String view = controller.deleteUser(7, redirectAttributes);

            verify(userService).deleteUser(7);
            assertEquals("redirect:/admin/users", view);
        }

        @Test
        void addsSuccessFlashAttribute() {
            controller.deleteUser(7, redirectAttributes);

            verify(redirectAttributes).addFlashAttribute("successMessage", "Пользователь успешно удалён");
        }
    }
}