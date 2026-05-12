package org.tourism.instructors.api.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tourism.instructors.application.reports.ReportService;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    ReportService reportService;

    @InjectMocks
    ReportController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    class CreateFileName {

        @Test
        void bothDatesNull_returnsNameWithExtensionOnly() {
            assertEquals("Отчёт.xlsx", controller.createFileName("Отчёт", "xlsx", null, null));
        }

        @Test
        void bothDatesPresent_returnsNameWithRange() {
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate till = LocalDate.of(2024, 12, 31);
            assertEquals("Отчёт [2024-01-01 - 2024-12-31].xlsx",
                    controller.createFileName("Отчёт", "xlsx", from, till));
        }

        @Test
        void onlyFromPresent_returnsNameWithOpenEndRange() {
            LocalDate from = LocalDate.of(2024, 1, 1);
            assertEquals("Отчёт [2024-01-01 - ].xlsx",
                    controller.createFileName("Отчёт", "xlsx", from, null));
        }

        @Test
        void onlyTillPresent_returnsNameWithOpenStartRange() {
            LocalDate till = LocalDate.of(2024, 12, 31);
            assertEquals("Отчёт [ - 2024-12-31].xlsx",
                    controller.createFileName("Отчёт", "xlsx", null, till));
        }
    }

    @Nested
    class CreateResponse {

        @Test
        void setsExcelContentType() {
            ResponseEntity<byte[]> response = controller.createResponse(new byte[0], "report.xlsx");
            assertEquals(
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                    response.getHeaders().getContentType());
        }

        @Test
        void setsContentLengthToDataSize() {
            byte[] data = new byte[]{1, 2, 3, 4, 5};
            ResponseEntity<byte[]> response = controller.createResponse(data, "report.xlsx");
            assertEquals(5, response.getHeaders().getContentLength());
        }

        @Test
        void setsContentDispositionWithUtf8EncodedFilename() {
            ResponseEntity<byte[]> response = controller.createResponse(new byte[0], "Отчёт.xlsx");
            String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            assertNotNull(disposition);
            assertTrue(disposition.startsWith("attachment; filename*=UTF-8''"));
        }

        @Test
        void encodesSpacesAsPercent20NotPlus() {
            ResponseEntity<byte[]> response = controller.createResponse(new byte[0], "My Report.xlsx");
            String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            assertNotNull(disposition);
            assertFalse(disposition.contains("+"), "spaces must be encoded as %20, not +");
            assertTrue(disposition.contains("%20"));
        }

        @Test
        void returnsOkWithBodyEqualToData() {
            byte[] data = {10, 20, 30};
            ResponseEntity<byte[]> response = controller.createResponse(data, "report.xlsx");
            assertEquals(200, response.getStatusCode().value());
            assertArrayEquals(data, response.getBody());
        }
    }

    @Nested
    class ReportProtocols {

        @Test
        void withNoDates_callsServiceWithBothEmpty() throws Exception {
            when(reportService.exportProtocols(Optional.empty(), Optional.empty()))
                    .thenReturn(new byte[0]);

            mockMvc.perform(get("/api/report/protocols"))
                    .andExpect(status().isOk());

            verify(reportService).exportProtocols(Optional.empty(), Optional.empty());
        }

        @Test
        void withBothDates_callsServiceWithParsedDates() throws Exception {
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate till = LocalDate.of(2024, 12, 31);
            when(reportService.exportProtocols(Optional.of(from), Optional.of(till)))
                    .thenReturn(new byte[]{1, 2, 3});

            mockMvc.perform(get("/api/report/protocols")
                            .param("dateFrom", "2024-01-01")
                            .param("dateTill", "2024-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

            verify(reportService).exportProtocols(Optional.of(from), Optional.of(till));
        }

        @Test
        void withOnlyDateFrom_callsServiceWithEmptyTill() throws Exception {
            LocalDate from = LocalDate.of(2024, 6, 1);
            when(reportService.exportProtocols(Optional.of(from), Optional.empty()))
                    .thenReturn(new byte[0]);

            mockMvc.perform(get("/api/report/protocols").param("dateFrom", "2024-06-01"))
                    .andExpect(status().isOk());

            verify(reportService).exportProtocols(Optional.of(from), Optional.empty());
        }

        @Test
        void withOnlyDateTill_callsServiceWithEmptyFrom() throws Exception {
            LocalDate till = LocalDate.of(2024, 6, 30);
            when(reportService.exportProtocols(Optional.empty(), Optional.of(till)))
                    .thenReturn(new byte[0]);

            mockMvc.perform(get("/api/report/protocols").param("dateTill", "2024-06-30"))
                    .andExpect(status().isOk());

            verify(reportService).exportProtocols(Optional.empty(), Optional.of(till));
        }

        @Test
        void responseContainsContentDispositionHeader() throws Exception {
            when(reportService.exportProtocols(any(), any())).thenReturn(new byte[0]);

            mockMvc.perform(get("/api/report/protocols"))
                    .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION));
        }
    }
}