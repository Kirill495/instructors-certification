package org.tourism.instructors.api.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
import org.tourism.instructors.application.protocol.ProtocolService;
import org.tourism.instructors.application.protocol.exception.ProtocolNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProtocolExceptionHandlerTest {

    @Mock
    ProtocolService protocolService;

    @InjectMocks
    ProtocolExceptionHandler handler;

    final ProtocolNotFoundException exception = new ProtocolNotFoundException(42);

    @Test
    void handleProtocolNotFound_returnsEditView() {
        String view = handler.handleProtocolNotFound(exception, new ExtendedModelMap());

        assertEquals("protocol/edit", view);
    }

    @Test
    void handleProtocolNotFound_addsExceptionMessageAsErrorAttribute() {
        ExtendedModelMap model = new ExtendedModelMap();

        handler.handleProtocolNotFound(exception, model);

        assertTrue(model.containsAttribute("errorMessage"));
        assertEquals(exception.getMessage(), model.get("errorMessage"));
    }

    @Test
    void handleProtocolNotFound_fetchesProtocolByIdAndAddsItToModel() {
        ProtocolDTO dto = mock(ProtocolDTO.class);
        when(protocolService.getProtocolById(42)).thenReturn(dto);
        ExtendedModelMap model = new ExtendedModelMap();

        handler.handleProtocolNotFound(exception, model);

        verify(protocolService).getProtocolById(42);
        assertEquals(dto, model.get("model"));
    }
}