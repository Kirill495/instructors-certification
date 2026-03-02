package org.tourism.instructors.api.home;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.application.protocol.ProtocolService;
import org.tourism.instructors.application.tourist.TouristService;

@Controller
public class HomeController {
    private final ProtocolService protocolService;
    private final TouristService touristService;
    private final CatalogService catalogService;

    public HomeController(ProtocolService protocolService,
                          TouristService touristService,
                          CatalogService catalogService) {
        this.protocolService = protocolService;
        this.touristService = touristService;
        this.catalogService = catalogService;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        // Add statistics to the model (optional)
        DashboardStats stats = new DashboardStats();
        stats.setProtocolsCount(protocolService.countProtocols());
        stats.setTouristsCount(touristService.countTourists());
        stats.setKindsCount(catalogService.countActiveKindsOfTourism());
        stats.setGradesCount(catalogService.countActiveGrades());

        UserInfo userInfo = new UserInfo();
        userInfo.setName(authentication.getName());
        userInfo.setAdmin(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        model.addAttribute("userInfo", userInfo);
        model.addAttribute("stats", stats);

        return "index";
    }

    public static class UserInfo {
        private String name;
        private boolean isAdmin;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public void setAdmin (boolean admin) {
            isAdmin = admin;
        }

        public boolean isAdmin () {
            return isAdmin;
        }
    }
    /**
     * Simple DTO for dashboard statistics
     */
    public static class DashboardStats {
        private int protocolsCount;
        private int touristsCount;
        private int kindsCount;
        private int gradesCount;

        public int getProtocolsCount() {
            return protocolsCount;
        }

        public void setProtocolsCount(int protocolsCount) {
            this.protocolsCount = protocolsCount;
        }

        public int getTouristsCount() {
            return touristsCount;
        }

        public void setTouristsCount(int touristsCount) {
            this.touristsCount = touristsCount;
        }

        public int getKindsCount() {
            return kindsCount;
        }

        public void setKindsCount(int kindsCount) {
            this.kindsCount = kindsCount;
        }

        public int getGradesCount() {
            return gradesCount;
        }

        public void setGradesCount(int gradesCount) {
            this.gradesCount = gradesCount;
        }
    }
}
