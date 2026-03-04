package org.tourism.instructors.api.pending;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.api.bot.TouristRegistrationBot;
import org.tourism.instructors.api.tourist.mapper.TouristMapper;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.application.tourist.TouristService;
import org.tourism.instructors.domain.pending.PendingTourist;

@Controller
@RequestMapping("/admin/pending")
public class PendingTouristController {

    private final PendingTouristService pendingTouristService;
    private final TouristRegistrationBot bot;
    private final TouristService touristService;
    private final TouristMapper touristMapper;

    public PendingTouristController(PendingTouristService pendingTouristService,
                                    TouristRegistrationBot bot, TouristService touristService, TouristMapper touristMapper) {
        this.pendingTouristService = pendingTouristService;
        this.bot = bot;
        this.touristService = touristService;
        this.touristMapper = touristMapper;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pending", pendingTouristService.findAllPending());
        return "admin/pending/list";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable int id, RedirectAttributes redirectAttributes) {
        PendingTourist pending = pendingTouristService.approve(id);
        bot.send(pending.getChatId(), "Ваша заявка одобрена ✅ Вы добавлены в систему.");
        touristService.save(touristMapper.toDTO(pending));
        redirectAttributes.addFlashAttribute("successMessage", "Турист " + pending.getFullName() + " добавлен");
        return "redirect:/admin/pending";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable int id, RedirectAttributes redirectAttributes) {
        PendingTourist pending = pendingTouristService.reject(id);
        bot.send(pending.getChatId(), "Ваша заявка отклонена ❌ Обратитесь к администратору.");
        redirectAttributes.addFlashAttribute("errorMessage", "Заявка " + pending.getFullName() + " отклонена");
        return "redirect:/admin/pending";
    }
}