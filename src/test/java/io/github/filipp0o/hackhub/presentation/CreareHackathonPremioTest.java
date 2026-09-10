package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.CreareHackathonControl;
import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.infrastructure.InMemoryHackathonRepository;
import io.github.filipp0o.hackhub.infrastructure.InMemoryPartecipazioneRepository;
import io.github.filipp0o.hackhub.infrastructure.InMemoryUtenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class CreareHackathonPremioTest {

    private static final String ERRORE_PREMIO =
            "L'importo del premio deve essere esprimibile in centesimi "
                    + "e non superare 99999999999999999.99 EUR";

    private Utente organizzatore;
    private Utente giudice;
    private Utente mentore;
    private InMemoryHackathonRepository repository;
    private CreareHackathonControl control;
    private MockMvc mockMvc;

    @BeforeEach
    void prepara() {
        organizzatore = Utente.ricostruisci(
                1L, "organizzatore@example.com", "hash-organizzatore"
        );
        giudice = Utente.ricostruisci(
                2L, "giudice@example.com", "hash-giudice"
        );
        mentore = Utente.ricostruisci(
                3L, "mentore@example.com", "hash-mentore"
        );

        repository = new InMemoryHackathonRepository(
                new InMemoryPartecipazioneRepository()
        );
        control = new CreareHackathonControl(
                new InMemoryUtenteRepository(
                        List.of(organizzatore, giudice, mentore)
                ),
                repository
        );

        SessioneUtente sessione = new SessioneUtente();
        sessione.registra(organizzatore);

        mockMvc = standaloneSetup(
                new CreareHackathonBoundary(control, sessione)
        )
                .setControllerAdvice(new ErroriRichiestaHandler())
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0.001",
            "1.234",
            "100000000000000000.00",
            "1E+100"
    })
    void rifiutaPremioNonRappresentabileEConsenteCorrezione(
            String premio
    ) throws Exception {
        DatiHackathon dati = dati(premio);

        assertEquals(
                List.of(ERRORE_PREMIO),
                control.verificaInformazioniEStaff(
                        dati, giudice, List.of(mentore)
                )
        );

        IllegalArgumentException errore = assertThrows(
                IllegalArgumentException.class,
                () -> control.crea(
                        dati, organizzatore, giudice, List.of(mentore)
                )
        );
        assertEquals(ERRORE_PREMIO, errore.getMessage());

        for (String percorso : List.of(
                "/api/hackathons/verifica",
                "/api/hackathons"
        )) {
            mockMvc.perform(post(percorso)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(richiesta(premio)))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("$.messaggio").value(ERRORE_PREMIO)
                    );
        }

        assertTrue(repository.ottieniTuttiHackathon().isEmpty());

        mockMvc.perform(post("/api/hackathons/verifica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta("100.50")))
                .andExpect(status().isOk());

        assertTrue(repository.ottieniTuttiHackathon().isEmpty());

        mockMvc.perform(post("/api/hackathons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta("100.50")))
                .andExpect(status().isCreated());

        assertEquals(1, repository.ottieniTuttiHackathon().size());
        assertEquals(
                new BigDecimal("100.50"),
                repository.ottieniTuttiHackathon()
                        .getFirst().getImportoPremio()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0.01",
            "1",
            "10.000",
            "1E+2",
            "99999999999999999.99"
    })
    void accettaPremioEsattoNelRiepilogoENellaConferma(
            String premio
    ) throws Exception {
        assertTrue(
                control.verificaInformazioniEStaff(
                        dati(premio), giudice, List.of(mentore)
                ).isEmpty()
        );

        mockMvc.perform(post("/api/hackathons/verifica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta(premio)))
                .andExpect(status().isOk());

        assertTrue(repository.ottieniTuttiHackathon().isEmpty());

        mockMvc.perform(post("/api/hackathons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta(premio)))
                .andExpect(status().isCreated());

        Hackathon salvato = repository
                .ottieniTuttiHackathon().getFirst();

        assertEquals(1, repository.ottieniTuttiHackathon().size());
        assertEquals(
                0,
                new BigDecimal(premio)
                        .compareTo(salvato.getImportoPremio())
        );
    }

    private DatiHackathon dati(String premio) {
        return new DatiHackathon(
                "HackHub",
                "Regolamento",
                "Criteri",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 12),
                "Camerino",
                new BigDecimal(premio),
                5
        );
    }

    private String richiesta(String premio) {
        return """
                {
                  "nome": "HackHub",
                  "regolamento": "Regolamento",
                  "criteriValutazione": "Criteri",
                  "scadenzaIscrizioni": "2026-10-01",
                  "dataInizio": "2026-10-10",
                  "dataFine": "2026-10-12",
                  "luogo": "Camerino",
                  "importoPremio": %s,
                  "dimensioneMassimaTeam": 5,
                  "giudiceId": 2,
                  "mentoriIds": [3]
                }
                """.formatted(premio);
    }
}