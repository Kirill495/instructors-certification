package org.tourism.instructors.api.enums;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.tourism.instructors.domain.protocol.ProtocolStatus;
import org.tourism.instructors.domain.tourist.model.Gender;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoType;

@RestController
@RequestMapping("/api/enums")
public class EnumsPresentation {

    private static final Map<String, Class<? extends Enum<?>>> REGISTRY =
            Map.of(
                    "Gender", Gender.class,
                    "ProtocolStatus", ProtocolStatus.class,
                    "ContactInfoType", ContactInfoType.class);

    private final MessageSource messageSource;

    public EnumsPresentation(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @GetMapping("/{enumClass}")
    public Map<String, String> getEnumLabels(@PathVariable String enumClass, Locale locale) {
        Class<? extends Enum<?>> clazz = REGISTRY.get(enumClass);
        if (clazz == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown enum: " + enumClass);
        }

        return Arrays.stream(clazz.getEnumConstants())
                .collect(
                        Collectors.toMap(
                                Enum::name,
                                e ->
                                        Objects.requireNonNullElse(
                                                messageSource.getMessage(
                                                        "enum."
                                                                + clazz.getSimpleName()
                                                                + "."
                                                                + e.name(),
                                                        null,
                                                        e.name(),
                                                        locale),
                                                e.name()),
                                (a, b) -> a,
                                LinkedHashMap::new));
    }
}
