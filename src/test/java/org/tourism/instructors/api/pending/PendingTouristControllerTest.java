package org.tourism.instructors.api.pending;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tourism.instructors.api.bot.TouristRegistrationBot;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.api.tourist.mapper.TouristMapper;
import org.tourism.instructors.application.pending.PendingTouristService;
import org.tourism.instructors.application.protocol.ProtocolService;
import org.tourism.instructors.application.tourist.TouristService;
import org.tourism.instructors.domain.pending.PendingTourist;
import org.tourism.instructors.domain.tourist.model.Tourist;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PendingTouristControllerTest {

    @Mock
    PendingTouristService pendingTouristService;
    @Mock
    TouristRegistrationBot bot;
    @Mock
    TouristService touristService;
    @Mock
    TouristMapper touristMapper;
    @Mock
    ProtocolService protocolService;

    @InjectMocks
    PendingTouristController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    class ListPending {

        @Test
        void addsAllPendingToModelAndReturnsView() throws Exception {
            PendingTourist pending = new PendingTourist();
            when(pendingTouristService.findAllPending()).thenReturn(List.of(pending));

            mockMvc.perform(get("/pending"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("pending/list"))
                    .andExpect(model().attributeExists("pending"));
        }

        @Test
        void addsEmptyListWhenNoPending() throws Exception {
            when(pendingTouristService.findAllPending()).thenReturn(List.of());

            mockMvc.perform(get("/pending"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("pending/list"))
                    .andExpect(model().attribute("pending", List.of()));
        }
    }

    @Nested
    class LinkTourist {

        @Test
        void callsServiceAndRedirects() throws Exception {
            mockMvc.perform(post("/pending/1/link").param("touristId", "42"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/pending"));

            verify(pendingTouristService).linkTourist(1, 42);
        }
    }

    @Nested
    class UnlinkTourist {

        @Test
        void callsServiceAndRedirects() throws Exception {
            mockMvc.perform(post("/pending/5/unlink"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/pending"));

            verify(pendingTouristService).unlinkTourist(5);
        }
    }

    @Nested
    class Approve {

        @Test
        void withLinkedTouristAndProtocol_addsToProtocolSendsBotMessageAndRedirects() throws Exception {
            Tourist tourist = new Tourist();
            tourist.setId(7);
            PendingTourist pending = pendingTouristWith(100L, "Petrov", "Ivan");
            pending.setTourist(tourist);
            when(pendingTouristService.approve(1)).thenReturn(pending);

            mockMvc.perform(post("/pending/1/approve").param("protocolId", "3"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/pending"));

            verify(protocolService).addTouristToProtocol(3, pending, 7);
            verify(bot).send(100L, "Ваша заявка одобрена ✅.");
        }

        @Test
        void withLinkedTouristWithoutProtocol_skipsProtocolAndRedirects() throws Exception {
            Tourist tourist = new Tourist();
            tourist.setId(7);
            PendingTourist pending = pendingTouristWith(100L, "Petrov", "Ivan");
            pending.setTourist(tourist);
            when(pendingTouristService.approve(1)).thenReturn(pending);

            mockMvc.perform(post("/pending/1/approve"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/pending"));

            verify(protocolService, never()).addTouristToProtocol(anyInt(), any(), anyInt());
            verify(bot).send(100L, "Ваша заявка одобрена ✅.");
        }

        @Test
        void withNoLinkedTourist_createsNewTouristWithTelegramContactOnly() throws Exception {
            PendingTourist pending = pendingTouristWith(200L, "Sidorov", "Petr");
            pending.setTgUsername("petr_tg");
            TouristDTO dto = new TouristDTO();
            when(pendingTouristService.approve(2)).thenReturn(pending);
            when(touristMapper.toDTO(pending)).thenReturn(dto);
            when(touristService.findTouristByTelegramId(200L)).thenReturn(Optional.of(new TouristDTO()));

            mockMvc.perform(post("/pending/2/approve"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/pending"));

            ArgumentCaptor<TouristDTO> captor = ArgumentCaptor.forClass(TouristDTO.class);
            verify(touristService).save(captor.capture());
            assertEquals(1, captor.getValue().getContactInfo().size());
        }

        @Test
        void withNoLinkedTourist_withEmailAndPhone_createsNewTouristWithThreeContacts() throws Exception {
            PendingTourist pending = pendingTouristWith(300L, "Volkova", "Anna");
            pending.setTgUsername("anna_tg");
            pending.setEmail("anna@example.com");
            pending.setPhoneNumber("+79001234567");
            TouristDTO dto = new TouristDTO();
            when(pendingTouristService.approve(3)).thenReturn(pending);
            when(touristMapper.toDTO(pending)).thenReturn(dto);
            when(touristService.findTouristByTelegramId(300L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/pending/3/approve"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/pending"));

            ArgumentCaptor<TouristDTO> captor = ArgumentCaptor.forClass(TouristDTO.class);
            verify(touristService).save(captor.capture());
            assertEquals(3, captor.getValue().getContactInfo().size());
        }

        @Test
        void withNoLinkedTouristAndProtocol_addsResolvedTouristToProtocol() throws Exception {
            PendingTourist pending = pendingTouristWith(400L, "Novikov", "Sergei");
            pending.setTgUsername("sergei_tg");
            TouristDTO resolvedDto = new TouristDTO();
            resolvedDto.setId(55);
            TouristDTO dto = new TouristDTO();
            when(pendingTouristService.approve(4)).thenReturn(pending);
            when(touristMapper.toDTO(pending)).thenReturn(dto);
            when(touristService.findTouristByTelegramId(400L)).thenReturn(Optional.of(resolvedDto));

            mockMvc.perform(post("/pending/4/approve").param("protocolId", "10"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/pending"));

            verify(protocolService).addTouristToProtocol(10, pending, 55);
        }
    }

    @Nested
    class Reject {

        @Test
        void callsServiceSendsMessageAndRedirects() throws Exception {
            PendingTourist pending = pendingTouristWith(500L, "Smirnova", "Olga");
            when(pendingTouristService.reject(4)).thenReturn(pending);

            mockMvc.perform(post("/pending/4/reject"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/pending"));

            verify(bot).send(500L, "Ваша заявка отклонена ❌ Обратитесь к администратору.");
        }
    }

    private PendingTourist pendingTouristWith(Long chatId, String lastName, String firstName) {
        PendingTourist p = new PendingTourist();
        p.setChatId(chatId);
        p.setLastName(lastName);
        p.setFirstName(firstName);
        return p;
    }
}