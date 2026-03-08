package org.tourism.instructors.api.pending;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.api.bot.TouristRegistrationBot;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.api.tourist.mapper.TouristMapper;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.application.tourist.TouristService;
import org.tourism.instructors.domain.pending.PendingTourist;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoDetails;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoItem;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoType;
import org.tourism.instructors.domain.tourist.model.contactinfo.TelegramDetails;

import java.util.ArrayList;
import java.util.List;

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
        TouristDTO dto = touristMapper.toDTO(pending);
        List<ContactInfoItem> contactInfo = new ArrayList<>();
        ContactInfoDetails details = new TelegramDetails(pending.getChatId(), pending.getTgUsername());

        ContactInfoItem tgItem = new ContactInfoItem();
        tgItem.setType(ContactInfoType.TELEGRAM);
        tgItem.setValue(pending.getChatId().toString());
        tgItem.setDetails(details);
        contactInfo.add(tgItem);
        if (Strings.isNotBlank(pending.getEmail())) {
            ContactInfoItem emailItem = new ContactInfoItem();
            emailItem.setType(ContactInfoType.EMAIL);
            emailItem.setValue(pending.getEmail());
            contactInfo.add(emailItem);
        }
        if (Strings.isNotBlank(pending.getPhoneNumber())) {
            ContactInfoItem phoneItem = new ContactInfoItem();
            phoneItem.setType(ContactInfoType.PHONE_NUMBER);
            phoneItem.setValue(pending.getPhoneNumber());
            contactInfo.add(phoneItem);
        }
        dto.setContactInfo(contactInfo);
        touristService.save(dto);
        bot.send(pending.getChatId(), "Ваша заявка одобрена ✅.");
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