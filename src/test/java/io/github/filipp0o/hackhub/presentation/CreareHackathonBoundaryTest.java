package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.CreareHackathonControl;
import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.UtenteRepository;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.TipoStatoHackathon;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class CreareHackathonBoundaryTest {

    private SessioneUtente sessione;
    private UtenteRepositoryFinto utenteRepository;
    private HackathonRepositoryFinto hackathonRepository;
    private CreareHackathonBoundary boundary;
    private MockMvc mockMvc;

    private static final String RICHIESTA_VALIDA = """
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
              "giudiceId": 2,
              "mentoriIds": [3, 4]
            }
            """;

    @BeforeEach
    void configuraBoundary() {
        sessione = new SessioneUtente();
        sessione.registra(new Utente(1L));

        hackathonRepository = new HackathonRepositoryFinto();
        utenteRepository = new UtenteRepositoryFinto();

        CreareHackathonControl control = new CreareHackathonControl(
                utenteRepository, hackathonRepository
        );

        boundary = new CreareHackathonBoundary(control, sessione);
        mockMvc = standaloneSetup(boundary)
                .setControllerAdvice(new ErroriRichiestaHandler())
                .build();
    }

    @Test
    void creaHackathonTramiteApiRest() throws Exception {
        mockMvc.perform(post("/api/hackathons")
                        .contentType(MediaType.APPLICATION_JSON)
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
                                  "giudiceId": 2,
                                  "mentoriIds": [3, 4]
                                }
                                """))
                .andExpect(status().isCreated());

        Hackathon hackathonSalvato = hackathonRepository.hackathonSalvato;

        assertAll(
                () -> assertNotNull(hackathonSalvato),
                () -> assertEquals(
                        "HackHub Challenge", hackathonSalvato.getNome()
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
                        1L, hackathonSalvato.getOrganizzatore().getId()
                ),
                () -> assertEquals(
                        2L, hackathonSalvato.getGiudice().getId()
                ),
                () -> assertEquals(
                        List.of(3L, 4L),
                        hackathonSalvato.getMentori().stream()
                                .map(Utente::getId)
                                .toList()
                ),
                () -> assertEquals(
                        TipoStatoHackathon.IN_ISCRIZIONE,
                        hackathonSalvato.getStato()
                )
        );
    }

    @Test
    void nonCreaHackathonConDatiNonValidi() {
        var richiesta =
                new CreareHackathonBoundary.RichiestaCreazioneHackathon(
                        " ",
                        "Regolamento",
                        "Qualità e innovazione",
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 9, 12),
                        "Camerino",
                        new BigDecimal("1000.00"),
                        4,
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
                () -> new CreareHackathonBoundary(null, new SessioneUtente())
        );
    }

    @Test
    void restituisceRiepilogoSenzaCreareHackathon() throws Exception {
        mockMvc.perform(post("/api/hackathons/verifica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RICHIESTA_VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dati.nome").value("HackHub Challenge"))
                .andExpect(jsonPath("$.dati.importoPremio").value(1000.00))
                .andExpect(jsonPath("$.giudice.id").value(2))
                .andExpect(jsonPath("$.giudice.email").value("giudice@example.com"))
                .andExpect(jsonPath("$.giudice.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.mentori[0].id").value(3))
                .andExpect(jsonPath("$.mentori[1].id").value(4));

        assertNull(hackathonRepository.hackathonSalvato);
        assertEquals(0, hackathonRepository.numeroSalvataggi);
    }

    @Test
    void rifiutaDatiEStaffNonValidiAllaVerificaEConsenteCorrezione()
            throws Exception {
        for (String richiesta : List.of(
                RICHIESTA_VALIDA.replace("HackHub Challenge", " "),
                RICHIESTA_VALIDA.replace("2026-09-01", "2026-09-11"),
                RICHIESTA_VALIDA.replace("1000.00", "0"),
                RICHIESTA_VALIDA.replace("1000.00", "null"),
                RICHIESTA_VALIDA.replace("[3, 4]", "[]"),
                RICHIESTA_VALIDA.replace("[3, 4]", "[999]"),
                RICHIESTA_VALIDA.replace("[3, 4]", "[null]"),
                RICHIESTA_VALIDA.replace("[3, 4]", "null"),
                RICHIESTA_VALIDA.replace("\"giudiceId\": 2", "\"giudiceId\": null"),
                RICHIESTA_VALIDA.replace("\"giudiceId\": 2", "\"giudiceId\": 999"),
                "{", "null", ""
        )) {
            mockMvc.perform(post("/api/hackathons/verifica")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(richiesta))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/hackathons/verifica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RICHIESTA_VALIDA))
                .andExpect(status().isOk());

        assertNull(hackathonRepository.hackathonSalvato);
        assertEquals(0, hackathonRepository.numeroSalvataggi);
    }

    @Test
    void ricontrollaDatiModificatiAllaConferma() throws Exception {
        mockMvc.perform(post("/api/hackathons/verifica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RICHIESTA_VALIDA))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/hackathons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RICHIESTA_VALIDA.replace("HackHub Challenge", " ")))
                .andExpect(status().isBadRequest());

        assertEquals(0, hackathonRepository.numeroSalvataggi);

        mockMvc.perform(post("/api/hackathons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RICHIESTA_VALIDA.replace(
                                "HackHub Challenge", "Nuovo nome"
                        )))
                .andExpect(status().isCreated());

        assertEquals("Nuovo nome", hackathonRepository.hackathonSalvato.getNome());
        assertEquals(1, hackathonRepository.numeroSalvataggi);
    }

    @Test
    void ricaricaLoStaffAllaConfermaDopoIlRiepilogo() throws Exception {
        mockMvc.perform(post("/api/hackathons/verifica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RICHIESTA_VALIDA))
                .andExpect(status().isOk());

        utenteRepository.giudiceDisponibile = false;

        mockMvc.perform(post("/api/hackathons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RICHIESTA_VALIDA))
                .andExpect(status().isBadRequest());

        assertEquals(0, hackathonRepository.numeroSalvataggi);
    }

    @Test
    void verificaRichiedeSessione() throws Exception {
        sessione.svuota();

        mockMvc.perform(post("/api/hackathons/verifica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RICHIESTA_VALIDA))
                .andExpect(status().isUnauthorized());

        assertEquals(0, hackathonRepository.numeroSalvataggi);
    }

    @Test
    void erroreDiSalvataggioResta500EConservaLaCausa() throws Exception {
        for (RuntimeException causa : List.of(
                new RuntimeException("Salvataggio fallito"),
                new IllegalArgumentException("Errore del repository"),
                new IllegalStateException("Repository non disponibile")
        )) {
            hackathonRepository.erroreSalvataggio = causa;

            mockMvc.perform(post("/api/hackathons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(RICHIESTA_VALIDA))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.messaggio")
                            .value("L'hackathon non è stato creato"))
                    .andExpect(risultato -> assertSame(
                            causa, risultato.getResolvedException().getCause()
                    ));

            assertNull(hackathonRepository.hackathonSalvato);
        }

        hackathonRepository.erroreSalvataggio = null;

        mockMvc.perform(post("/api/hackathons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RICHIESTA_VALIDA))
                .andExpect(status().isCreated());

        assertEquals(1, hackathonRepository.numeroSalvataggi);
    }

    private static class UtenteRepositoryFinto implements UtenteRepository {

        private boolean giudiceDisponibile = true;

        @Override
        public List<Utente> recuperaUtentiAssegnabili() {
            return List.of(
                            Utente.ricostruisci(2L, "giudice@example.com", "hash"),
                            Utente.ricostruisci(3L, "mentore1@example.com", "hash"),
                            Utente.ricostruisci(4L, "mentore2@example.com", "hash")
                    ).stream()
                    .filter(utente -> giudiceDisponibile
                            || !utente.getId().equals(2L))
                    .toList();
        }

        @Override
        public boolean esistePerEmail(String email) {
            throw new UnsupportedOperationException("Non utilizzato in questo test");
        }

        @Override
        public Utente recuperaPerEmail(String email) {
            throw new UnsupportedOperationException("Non utilizzato in questo test");
        }

        @Override
        public void salva(Utente utente) {
            throw new UnsupportedOperationException("Non utilizzato in questo test");
        }
    }

    private static class HackathonRepositoryFinto implements HackathonRepository {

        private Hackathon hackathonSalvato;
        private int numeroSalvataggi;
        private RuntimeException erroreSalvataggio;

        @Override
        public List<Hackathon> ottieniHackathonValutabili(Utente giudice) {
            return List.of();
        }

        @Override
        public List<Hackathon> ottieniHackathonSegnalabili(Utente mentore) {
            return List.of();
        }

        @Override
        public void salva(Hackathon hackathon) {
            if (erroreSalvataggio != null) {
                throw erroreSalvataggio;
            }
            numeroSalvataggi++;
            hackathonSalvato = hackathon;
        }

        @Override
        public List<Hackathon> ottieniHackathonApertiAlleIscrizioni() {
            throw new UnsupportedOperationException("Non utilizzato in questo test");
        }

        @Override
        public List<Hackathon> ottieniTuttiHackathon() {
            throw new UnsupportedOperationException("Non utilizzato in questo test");
        }

        @Override
        public Hackathon recuperaHackathon(Long hackathonId) {
            throw new UnsupportedOperationException("Non utilizzato in questo test");
        }
    }
}