package org.tourism.instructors.application.protocol;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.tourism.instructors.api.bot.TouristRegistrationBot;
import org.tourism.instructors.api.protocol.dto.ProtocolForListDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "telegram.bot.token=test",
                "telegram.bot.username=test_bot",
                "telegram.bot.mini-app-url=http://localhost",
                "app.admin.login=admin",
                "app.admin.password=admin",
                "app.user.login=user",
                "app.user.password=user"
        }
)
@Testcontainers
class ProtocolServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockitoBean
    TouristRegistrationBot bot;

    @Autowired
    ProtocolService protocolService;

    @Test
    void getProtocolsForList() {
        Page<ProtocolForListDTO> result = protocolService.getProtocolsForList(null, PageRequest.of(0, 10));
        assertEquals(2, result.getTotalElements());
    }
}