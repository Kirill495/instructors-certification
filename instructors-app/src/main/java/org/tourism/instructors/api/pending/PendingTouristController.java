package org.tourism.instructors.api.pending;

import static org.tourism.instructors.api.util.CommonAttributes.ERROR_MESSAGE_ATTRIBUTE;
import static org.tourism.instructors.api.util.CommonAttributes.SUCCESS_MESSAGE_ATTRIBUTE;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.api.bot.TouristRegistrationBot;
import org.tourism.instructors.api.tourist.dto.ContactInfoItemDTO;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.api.tourist.mapper.TouristMapper;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.application.protocol.ProtocolService;
import org.tourism.instructors.application.tourist.TouristService;
import org.tourism.instructors.domain.pending.PendingTourist;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoDetails;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoType;
import org.tourism.instructors.domain.tourist.model.contactinfo.TelegramDetails;

@Controller
@RequestMapping("/pending")
public class PendingTouristController {

    private static final String REDIRECT_URL = "redirect:/pending";
    private final PendingTouristService pendingTouristService;
    private final TouristRegistrationBot bot;
    private final TouristService touristService;
    private final TouristMapper touristMapper;
    private final ProtocolService protocolService;

    public PendingTouristController(
            PendingTouristService pendingTouristService,
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
        return "pending/list";
    }

    @PostMapping("/{id}/link")
    public String linkTourist(@PathVariable int id, @RequestParam int touristId) {
        pendingTouristService.linkTourist(id, touristId);
        return REDIRECT_URL;
    }

    @PostMapping("/{id}/unlink")
    public String unlinkTourist(@PathVariable int id) {
        pendingTouristService.unlinkTourist(id);
        return REDIRECT_URL;
    }

    @PostMapping("/{id}/approve")
    public String approve(
            @PathVariable int id,
            @RequestParam(required = false) Integer protocolId,
            RedirectAttributes redirectAttributes) {
        PendingTourist pending = pendingTouristService.approve(id);
        Integer resolvedTouristId;

        if (Objects.isNull(pending.getTourist())) {
            TouristDTO dto = touristMapper.toDTO(pending);
            List<ContactInfoItemDTO> contactInfo = new ArrayList<>();
            ContactInfoDetails details =
                    new TelegramDetails(pending.getChatId(), pending.getTgUsername());

            ContactInfoItemDTO tgItem =
                    new ContactInfoItemDTO(
                            null,
                            null,
                            ContactInfoType.TELEGRAM,
                            pending.getChatId().toString(),
                            details);
            contactInfo.add(tgItem);
            if (Strings.isNotBlank(pending.getEmail())) {
                ContactInfoItemDTO emailItem =
                        new ContactInfoItemDTO(
                                null, null, ContactInfoType.EMAIL, pending.getEmail(), null);
                contactInfo.add(emailItem);
            }
            if (Strings.isNotBlank(pending.getPhoneNumber())) {
                ContactInfoItemDTO phoneItem =
                        new ContactInfoItemDTO(
                                null,
                                null,
                                ContactInfoType.PHONE_NUMBER,
                                pending.getPhoneNumber(),
                                null);
                contactInfo.add(phoneItem);
            }
            dto.setContactInfo(contactInfo);
            touristService.save(dto);
            resolvedTouristId =
                    touristService
                            .findTouristByTelegramId(pending.getChatId())
                            .map(TouristDTO::getId)
                            .orElse(null);
        } else {
            resolvedTouristId = pending.getTourist().getId();
        }

        if (protocolId != null && resolvedTouristId != null) {
            protocolService.addTouristToProtocol(protocolId, pending, resolvedTouristId);
        }

        bot.send(pending.getChatId(), "Ваша заявка одобрена ✅.");
        redirectAttributes.addFlashAttribute(
                SUCCESS_MESSAGE_ATTRIBUTE, "Турист " + pending.getFullName() + " одобрен");
        return REDIRECT_URL;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable int id, RedirectAttributes redirectAttributes) {
        PendingTourist pending = pendingTouristService.reject(id);
        bot.send(pending.getChatId(), "Ваша заявка отклонена ❌ Обратитесь к администратору.");
        redirectAttributes.addFlashAttribute(
                ERROR_MESSAGE_ATTRIBUTE, "Заявка " + pending.getFullName() + " отклонена");
        return REDIRECT_URL;
    }
}
