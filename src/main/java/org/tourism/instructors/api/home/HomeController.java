package org.tourism.instructors.api.home;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.application.protocol.ProtocolService;
import org.tourism.instructors.application.tourist.TouristService;

@Controller
public class HomeController {
    private final ProtocolService protocolService;
    private final TouristService touristService;
    private final CatalogService catalogService;
    private final PendingTouristService pendingTouristService;

    public HomeController(ProtocolService protocolService,
                          TouristService touristService,
                          CatalogService catalogService,
                          PendingTouristService pendingTouristService) {
        this.protocolService = protocolService;
        this.touristService = touristService;
        this.catalogService = catalogService;
        this.pendingTouristService = pendingTouristService;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        // Add statistics to the model (optional)
        DashboardStats stats = new DashboardStats();
        stats.setProtocolsCount(protocolService.countProtocols());
        stats.setTouristsCount(touristService.countTourists());
        stats.setKindsCount(catalogService.countActiveKindsOfTourism());
        stats.setGradesCount(catalogService.countActiveGrades());
        stats.setPendingCount(pendingTouristService.countPending());

        UserInfo userInfo = new UserInfo();
        userInfo.setName(authentication.getName());
        userInfo.setAdmin(authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
        model.addAttribute("userInfo", userInfo);
        model.addAttribute("stats", stats);

        return "index";
    }

    @Getter
    @Setter
    public static class UserInfo {
        private String name;
        private boolean isAdmin;
    }
    /**
     * Simple DTO for dashboard statistics
     */
    @Getter
    @Setter
    public static class DashboardStats {
        private int protocolsCount;
        private int touristsCount;
        private int kindsCount;
        private int gradesCount;
        private int pendingCount;
    }

}
