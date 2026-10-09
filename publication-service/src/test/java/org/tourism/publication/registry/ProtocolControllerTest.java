package org.tourism.publication.registry;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.tourism.publication.registry.dto.AssignmentResponse;
import org.tourism.publication.registry.dto.ProtocolResponse;

@WebMvcTest(ProtocolController.class)
class ProtocolControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ProtocolRegistry protocolRegistry;

    @Test
    void testGetProtocolByNumber_whenProtocolDoesNotExists_thenReturnsNotFound() throws Exception {
        when(protocolRegistry.findProtocolByNumber("1")).thenReturn(Optional.empty());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/protocols/1"))
                .andExpect(status().isNotFound());
        verify(protocolRegistry).findProtocolByNumber(anyString());
    }

    @Test
    void testGetProtocolByNumber_whenProtocolExists_thenReturnsProtocol() throws Exception {
        String assignmentDate = "2026-10-08";

        ProtocolResponse protocol = buildProtocol("1", LocalDate.parse(assignmentDate));
        AssignmentResponse assignment = protocol.assignments().getFirst();
        when(protocolRegistry.findProtocolByNumber("1")).thenReturn(Optional.of(protocol));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/protocols/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(protocol.number()))
                .andExpect(jsonPath("$.date").value(protocol.date().toString()))
                .andExpect(jsonPath("$.orderNumber").value(protocol.orderNumber()))
                .andExpect(jsonPath("$.assignments", hasSize(1)))
                .andExpect(jsonPath("$.assignments[0].lastName").value(assignment.lastName()))
                .andExpect(jsonPath("$.assignments[0].firstName").value(assignment.firstName()))
                .andExpect(jsonPath("$.assignments[0].middleName").value(assignment.middleName()))
                .andExpect(jsonPath("$.assignments[0].grade").value(assignment.grade()))
                .andExpect(
                        jsonPath("$.assignments[0].kindOfTourism")
                                .value(assignment.kindOfTourism()))
                .andExpect(jsonPath("$.assignments[0].club").value(assignment.club()))
                .andExpect(jsonPath("$.assignments[0].assignmentDate").value(assignmentDate))
                .andExpect(
                        jsonPath("$.assignments[0].validUntil")
                                .value(assignment.assignmentDate().toString()));

        verify(protocolRegistry).findProtocolByNumber(anyString());
    }

    @Test
    void testFindProtocolsInPeriod_whenPeriodsNotCorrect_thenReturnsBadRequest() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/protocols")
                                .param("since", "2026-10-09")
                                .param("till", "2026-10-09"))
                .andExpect(status().isBadRequest());
        verify(protocolRegistry, never())
                .findProtocolsInPeriod(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testFindProtocolsInPeriod_whenSinceParameterIsIncorrect_thenReturnsBadRequest()
            throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/protocols")
                                .param("since", "2026-20-09")
                                .param("till", "2026-10-09"))
                .andExpect(status().isBadRequest());
        verify(protocolRegistry, never())
                .findProtocolsInPeriod(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testFindProtocolsInPeriod_whenNoProtocolsExists_thenReturnsEmptyList() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/protocols")
                                .param("since", "2026-10-09")
                                .param("till", "2026-10-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        verify(protocolRegistry).findProtocolsInPeriod(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testFindProtocolsInPeriod_whenOneProtocolsExists_thenReturnsListOfOneProtocol()
            throws Exception {
        String since = "2026-10-09";
        String till = "2026-10-10";
        ProtocolResponse protocol = buildProtocol("1", LocalDate.parse(since));
        when(protocolRegistry.findProtocolsInPeriod(LocalDate.parse(since), LocalDate.parse(till)))
                .thenReturn(List.of(protocol));
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/protocols")
                                .param("since", since)
                                .param("till", till))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        verify(protocolRegistry).findProtocolsInPeriod(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testFindProtocolsInPeriod_whenSinceParameterNotSpecified_thenReturnsBadRequest()
            throws Exception {
        String till = "2026-10-09";
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/protocols").param("till", till))
                .andExpect(status().isBadRequest());
        verify(protocolRegistry, never())
                .findProtocolsInPeriod(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testFindProtocolsInPeriod_whenTillParameterNotSpecified_thenReturnsBadRequest()
            throws Exception {
        String since = "2026-10-09";
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/protocols").param("since", since))
                .andExpect(status().isBadRequest());
        verify(protocolRegistry, never())
                .findProtocolsInPeriod(any(LocalDate.class), any(LocalDate.class));
    }

    private static ProtocolResponse buildProtocol(String number, LocalDate assignmentDate) {
        return new ProtocolResponse(
                number, assignmentDate, "order", List.of(buildAssignment(assignmentDate)));
    }

    private static AssignmentResponse buildAssignment(LocalDate assignmentDate) {
        return new AssignmentResponse(
                1,
                "Smith",
                "John",
                "",
                "instructor",
                "trekking",
                "",
                assignmentDate,
                assignmentDate.plusYears(2));
    }
}
