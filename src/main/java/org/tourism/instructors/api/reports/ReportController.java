package org.tourism.instructors.api.reports;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tourism.instructors.application.reports.ReportService;

import java.io.IOException;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController (ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/protocols")
    public ResponseEntity<byte[]> reportProtocols() throws IOException {
        byte[] reportData = reportService.exportProtocols();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "protocols.xlsx");
        headers.setContentLength(reportData.length);
        return ResponseEntity.ok().headers(headers).body(reportData);
    }
}
