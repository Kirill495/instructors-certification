package org.tourism.instructors.api.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.tourism.instructors.application.protocol.ProtocolService;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.domain.tourist.TouristService;

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
    public String home(Model model) {
        // Add statistics to the model (optional)
        DashboardStats stats = new DashboardStats();
        stats.setProtocolsCount(protocolService.countProtocols());
        stats.setTouristsCount(touristService.countTourists());
        stats.setKindsCount(catalogService.countActiveKindsOfTourism());
        stats.setGradesCount(catalogService.countActiveGrades());

        model.addAttribute("stats", stats);

        return "index";
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
