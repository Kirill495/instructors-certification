package org.tourism.instructors.api.reports;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tourism.instructors.application.reports.ReportService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/protocols")
    public ResponseEntity<byte[]> reportProtocols(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTill) throws IOException {

        byte[] reportData = reportService.exportProtocols(Optional.ofNullable(dateFrom), Optional.ofNullable(dateTill));
        String filename = createFileName("Присвоения званий", "xlsx", dateFrom, dateTill);

        return createResponse(reportData, filename);

    }

    ResponseEntity<byte[]> createResponse(byte[] reportData, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
        headers.setContentLength(reportData.length);
        return ResponseEntity.ok().headers(headers).body(reportData);
    }

    String createFileName(String namePattern, String extension, LocalDate from, LocalDate till) {
        if ((from == null) && till == null) {
            return String.format("%s.%s", namePattern, extension);
        } else {
            return String.format("%s [%s - %s].%s", namePattern,
                    from == null ? "" : from,
                    till == null ? "" : till,
                    extension);
        }
    }
}
