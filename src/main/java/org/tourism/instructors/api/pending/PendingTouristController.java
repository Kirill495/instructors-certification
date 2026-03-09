package org.tourism.instructors.api.pending;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.api.bot.TouristRegistrationBot;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.api.tourist.mapper.TouristMapper;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.application.protocol.ProtocolService;
import org.tourism.instructors.application.tourist.TouristService;
import org.tourism.instructors.domain.pending.PendingTourist;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoDetails;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoItem;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoType;
import org.tourism.instructors.domain.tourist.model.contactinfo.TelegramDetails;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/pending")
public class PendingTouristController {

    private final PendingTouristService pendingTouristService;
    private final TouristRegistrationBot bot;
    private final TouristService touristService;
    private final TouristMapper touristMapper;
    private final ProtocolService protocolService;

    public PendingTouristController(PendingTouristService pendingTouristService,
                                    TouristRegistrationBot bot,
                                    TouristService touristService,
                                    TouristMapper touristMapper,
                                    ProtocolService protocolService) {
        this.pendingTouristService = pendingTouristService;
        this.bot = bot;
        this.touristService = touristService;
        this.touristMapper = touristMapper;
        this.protocolService = protocolService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pending", pendingTouristService.findAllPending());
        return "admin/pending/list";
    }

    @GetMapping("/{id}/link")
    public String linkForm(@PathVariable int id, Model model) {
        model.addAttribute("pending", pendingTouristService.getById(id));
        return "admin/pending/link";
    }

    @PostMapping("/{id}/link")
    public String linkTourist(@PathVariable int id, @RequestParam int touristId) {
        pendingTouristService.linkTourist(id, touristId);
        return "redirect:/pending";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable int id,
                          @RequestParam(required = false) Integer touristId,
                          @RequestParam(required = false) Integer protocolId,
                          RedirectAttributes redirectAttributes) {
        if (touristId != null) {
            pendingTouristService.linkTourist(id, touristId);
        }

        PendingTourist pending = pendingTouristService.approve(id);
        Integer resolvedTouristId = null;

        if (pending.getTourist() == null) {
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
            resolvedTouristId = touristService.findTouristByTelegramId(pending.getChatId())
                    .map(TouristDTO::getId).orElse(null);
        } else {
            resolvedTouristId = pending.getTourist().getId();
        }

        if (protocolId != null && resolvedTouristId != null) {
            protocolService.addTouristToProtocol(protocolId, pending, resolvedTouristId);
        }

        bot.send(pending.getChatId(), "Ваша заявка одобрена ✅.");
        redirectAttributes.addFlashAttribute("successMessage", "Турист " + pending.getFullName() + " одобрен");
        return "redirect:/pending";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable int id, RedirectAttributes redirectAttributes) {
        PendingTourist pending = pendingTouristService.reject(id);
        bot.send(pending.getChatId(), "Ваша заявка отклонена ❌ Обратитесь к администратору.");
        redirectAttributes.addFlashAttribute("errorMessage", "Заявка " + pending.getFullName() + " отклонена");
        return "redirect:/pending";
    }
}