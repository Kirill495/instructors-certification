package org.tourism.instructors.api.pending;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tourism.instructors.application.pending.PendingTouristService;

@RestController
@RequestMapping("/api/pending")
public class PendingTouristRestController {

    private final PendingTouristService pendingTouristService;

    public PendingTouristRestController(PendingTouristService pendingTouristService) {
        this.pendingTouristService = pendingTouristService;
    }

    @PostMapping("/{id}/unlink")
    public ResponseEntity<Void> unlinkTourist(@PathVariable int id) {
        pendingTouristService.unlinkTourist(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/link")
    public ResponseEntity<Void> linkTourist(@PathVariable int id, @RequestParam("tourist_id") int touristId) {
        pendingTouristService.linkTourist(id, touristId);
        return ResponseEntity.ok().build();
    }
}