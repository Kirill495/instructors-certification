package org.tourism.instructors.api.protocol;

import static org.tourism.instructors.api.util.CommonAttributes.ERROR_MESSAGE_ATTRIBUTE;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.tourism.instructors.application.protocol.ProtocolService;
import org.tourism.instructors.application.protocol.exception.ProtocolNotFoundException;

@ControllerAdvice(assignableTypes = ProtocolController.class)
public class ProtocolExceptionHandler {

    private final ProtocolService protocolService;

    public ProtocolExceptionHandler(ProtocolService protocolService) {
        this.protocolService = protocolService;
    }

    @ExceptionHandler(ProtocolNotFoundException.class)
    public String handleProtocolNotFound(ProtocolNotFoundException exception, Model model) {
        model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, exception.getMessage());
        model.addAttribute("model", protocolService.getProtocolById(exception.getProtocolId()));
        return "protocol/edit";
    }
}
