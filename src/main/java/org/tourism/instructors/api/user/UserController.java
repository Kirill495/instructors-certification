package org.tourism.instructors.api.user;

import static org.tourism.instructors.api.util.CommonAttributes.SUCCESS_MESSAGE_ATTRIBUTE;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.api.user.dto.UserDTO;
import org.tourism.instructors.application.user.UserService;

@Controller
@RequestMapping("/admin/users")
public class UserController {

    private static final String REDIRECT_URL = "redirect:/admin/users";

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new UserDTO());
        return "admin/users/edit";
    }

    @PostMapping("/new")
    public String createUser(@ModelAttribute UserDTO dto, RedirectAttributes redirectAttributes) {
        userService.createUser(dto);
        redirectAttributes.addFlashAttribute(
                SUCCESS_MESSAGE_ATTRIBUTE, "Пользователь успешно создан");
        return REDIRECT_URL;
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable int id, Model model) {
        model.addAttribute("user", userService.findById(id));
        return "admin/users/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateUser(
            @PathVariable int id,
            @ModelAttribute UserDTO dto,
            RedirectAttributes redirectAttributes) {
        userService.updateUser(id, dto);
        redirectAttributes.addFlashAttribute(
                SUCCESS_MESSAGE_ATTRIBUTE, "Пользователь успешно обновлён");
        return REDIRECT_URL;
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable int id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute(
                SUCCESS_MESSAGE_ATTRIBUTE, "Пользователь успешно удалён");
        return REDIRECT_URL;
    }
}
