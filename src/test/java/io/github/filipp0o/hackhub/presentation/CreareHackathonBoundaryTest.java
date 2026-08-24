package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.CreareHackathonControl;
import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.UtenteRepository;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.StatoHackathon;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class CreareHackathonBoundaryTest {

    private HackathonRepositoryFinto hackathonRepository;
    private CreareHackathonBoundary boundary;
    private MockMvc mockMvc;

    @BeforeEach
    void configuraBoundary() {
        hackathonRepository =
                new HackathonRepositoryFinto();

        CreareHackathonControl control =
                new CreareHackathonControl(
                        new UtenteRepositoryFinto(),
                        hackathonRepository
                );

        boundary = new CreareHackathonBoundary(control);

        mockMvc = standaloneSetup(boundary).build();
    }

    @Test
    void creaHackathonTramiteApiRest() throws Exception {
        mockMvc.perform(
                        post("/api/hackathons")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "nome": "HackHub Challenge",
                                          "regolamento": "Regolamento",
                                          "criteriValutazione": "Qualità e innovazione",
                                          "scadenzaIscrizioni": "2026-09-01",
                                          "dataInizio": "2026-09-10",
                                          "dataFine": "2026-09-12",
                                          "luogo": "Camerino",
                                          "importoPremio": 1000.00,
                                          "dimensioneMassimaTeam": 4,
                                          "organizzatoreId": 1,
                                          "giudiceId": 2,
                                          "mentoriIds": [3, 4]
                                        }
                                        """)
                )
                .andExpect(status().isCreated());

        Hackathon hackathonSalvato =
                hackathonRepository.hackathonSalvato;

        assertAll(
                () -> assertNotNull(hackathonSalvato),
                () -> assertEquals(
                        "HackHub Challenge",
                        hackathonSalvato.getNome()
                ),
                () -> assertEquals(
                        LocalDate.of(2026, 9, 10),
                        hackathonSalvato.getDataInizio()
                ),
                () -> assertEquals(
                        new BigDecimal("1000.00"),
                        hackathonSalvato.getImportoPremio()
                ),
                () -> assertEquals(
                        1L,
                        hackathonSalvato
                                .getOrganizzatore()
                                .getId()
                ),
                () -> assertEquals(
                        2L,
                        hackathonSalvato
                                .getGiudice()
                                .getId()
                ),
                () -> assertEquals(
                        List.of(3L, 4L),
                        hackathonSalvato.getMentori()
                                .stream()
                                .map(Utente::getId)
                                .toList()
                ),
                () -> assertEquals(
                        StatoHackathon.IN_ISCRIZIONE,
                        hackathonSalvato.getStato()
                )
        );
    }

    @Test
    void nonCreaHackathonConDatiNonValidi() {
        CreareHackathonBoundary
                .RichiestaCreazioneHackathon richiesta =
                new CreareHackathonBoundary
                        .RichiestaCreazioneHackathon(
                        " ",
                        "Regolamento",
                        "Qualità e innovazione",
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 9, 12),
                        "Camerino",
                        new BigDecimal("1000.00"),
                        4,
                        1L,
                        2L,
                        List.of(3L)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> boundary.creaHackathon(richiesta)
        );

        assertNull(hackathonRepository.hackathonSalvato);
    }

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new CreareHackathonBoundary(null)
        );
    }

    private static class UtenteRepositoryFinto
            implements UtenteRepository {

        @Override
        public List<Utente> recuperaUtentiAssegnabili() {
            return List.of();
        }
    }

    private static class HackathonRepositoryFinto
            implements HackathonRepository {

        private Hackathon hackathonSalvato;

        @Override
        public List<Hackathon> ottieniHackathonValutabili(
                Utente giudice
        ) {
            return List.of();
        }

        @Override
        public List<Hackathon> ottieniHackathonSegnalabili(
                Utente mentore
        ) {
            return List.of();
        }

        @Override
        public void salva(Hackathon hackathon) {
            hackathonSalvato = hackathon;
        }

        @Override
        public List<Hackathon> ottieniHackathonApertiAlleIscrizioni() {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }

        @Override
        public List<Hackathon> ottieniTuttiHackathon() {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }

        @Override
        public Hackathon recuperaHackathon(
                Long hackathonId
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }
}