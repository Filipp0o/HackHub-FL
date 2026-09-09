package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.application.ValutareSottomissioneControl;
import io.github.filipp0o.hackhub.application.ValutazioneRepository;
import io.github.filipp0o.hackhub.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ValutareSottomissioneBoundaryTest {

    private Hackathon hackathon;
    private Sottomissione sottomissione;
    private ValutazioneRepositoryFinto valutazioneRepository;
    private MockMvc mockMvc;
    private SessioneUtente sessione;

    @BeforeEach
    void configuraBoundary() {
        sessione = new SessioneUtente();
        sessione.registra(new Utente(2L));

        Utente giudice = new Utente(2L);
        hackathon = creaHackathonInValutazione(giudice);
        hackathon.assegnaId(1L);

        Utente responsabile = new Utente(4L);
        Team team = Team.crea("Team Alpha", responsabile, responsabile);

        Partecipazione partecipazione =
                new Partecipazione(hackathon, team);

        sottomissione = new Sottomissione(
                partecipazione,
                "Repository del progetto"
        );
        sottomissione.assegnaId(1L);

        valutazioneRepository = new ValutazioneRepositoryFinto();

        ValutareSottomissioneControl control =
                new ValutareSottomissioneControl(
                        new HackathonRepositoryFinto(hackathon),
                        new PartecipazioneRepositoryFinto(partecipazione),
                        valutazioneRepository
                );

        mockMvc = standaloneSetup(
                new ValutareSottomissioneBoundary(control, sessione)
        ).setControllerAdvice(new ErroriRichiestaHandler()).build();
    }

    @Test
    void restituisceHackathonValutabili() throws Exception {
        mockMvc.perform(get("/api/valutazioni/hackathons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(hackathon.getId().intValue()))
                .andExpect(jsonPath("$[0].nome")
                        .value("HackHub 2026"));
    }

    @Test
    void restituisceSottomissioniDaValutare() throws Exception {
        mockMvc.perform(get(
                        "/api/valutazioni/hackathons/{hackathonId}/sottomissioni",
                        hackathon.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(sottomissione.getId().intValue()))
                .andExpect(jsonPath("$[0].contenuto")
                        .value("Repository del progetto"))
                .andExpect(jsonPath("$[0].criteriValutazione")
                        .value("Qualità e innovazione"));
    }

    @Test
    void registraValutazioneTramiteApiRest() throws Exception {
        mockMvc.perform(post(
                        "/api/valutazioni/hackathons/{hackathonId}/sottomissioni/{sottomissioneId}",
                        hackathon.getId(),
                        sottomissione.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "giudizio": "Ottimo progetto",
                                  "punteggio": 9
                                }
                                """))
                .andExpect(status().isCreated());

        Valutazione valutazione =
                valutazioneRepository.valutazioneSalvata;

        assertAll(
                () -> assertNotNull(valutazione),
                () -> assertSame(
                        valutazione,
                        sottomissione.getValutazione()
                ),
                () -> assertEquals(
                        "Ottimo progetto",
                        valutazione.getGiudizio()
                ),
                () -> assertEquals(
                        BigDecimal.valueOf(9),
                        valutazione.getPunteggio()
                ),
                () -> assertEquals(
                        2L,
                        valutazione.getGiudice().getId()
                )
        );
    }

    @Test
    void restituisceNotFoundPerHackathonSconosciuto() throws Exception {
        mockMvc.perform(get(
                        "/api/valutazioni/hackathons/{hackathonId}/sottomissioni",
                        Long.MAX_VALUE
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new ValutareSottomissioneBoundary(
                        null,
                        new SessioneUtente()
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"giudizio\":\"Buon lavoro\"}",
            "{\"giudizio\":\"Buon lavoro\",\"punteggio\":null}",
            "{\"giudizio\":\"Buon lavoro\",\"punteggio\":-0.1}",
            "{\"giudizio\":\"Buon lavoro\",\"punteggio\":10.1}",
            "{\"giudizio\":\" \",\"punteggio\":8}",
            "{\"giudizio\":\"Buon lavoro\",\"punteggio\":\"abc\"}",
            ""
    })
    void restituisceBadRequestPerDatiNonValidi(String richiesta) throws Exception {
        mockMvc.perform(post(
                        "/api/valutazioni/hackathons/{hackathonId}/sottomissioni/{sottomissioneId}",
                        hackathon.getId(), sottomissione.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta))
                .andExpect(status().isBadRequest());

        assertAll(
                () -> assertNull(sottomissione.getValutazione()),
                () -> assertNull(valutazioneRepository.valutazioneSalvata),
                () -> assertEquals(0, valutazioneRepository.numeroSalvataggi)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"generico", "argomento", "stato"})
    void restituisceInternalServerErrorEConsenteNuovoTentativo(String tipo)
            throws Exception {
        valutazioneRepository.erroreSalvataggio = switch (tipo) {
            case "argomento" -> new IllegalArgumentException("Errore interno");
            case "stato" -> new IllegalStateException("Errore interno");
            default -> new RuntimeException("Errore interno");
        };

        mockMvc.perform(post(
                        "/api/valutazioni/hackathons/{hackathonId}/sottomissioni/{sottomissioneId}",
                        hackathon.getId(), sottomissione.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"giudizio":"Buon lavoro","punteggio":8.5}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.messaggio")
                        .value("La valutazione non è stata registrata"));

        assertAll(
                () -> assertNull(sottomissione.getValutazione()),
                () -> assertNull(valutazioneRepository.valutazioneSalvata),
                () -> assertEquals(0, valutazioneRepository.numeroSalvataggi)
        );

        valutazioneRepository.erroreSalvataggio = null;

        mockMvc.perform(post(
                        "/api/valutazioni/hackathons/{hackathonId}/sottomissioni/{sottomissioneId}",
                        hackathon.getId(), sottomissione.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"giudizio":"Buon lavoro","punteggio":8.5}
                                """))
                .andExpect(status().isCreated());

        assertNotNull(sottomissione.getValutazione());
        assertSame(sottomissione.getValutazione(), valutazioneRepository.valutazioneSalvata);
        assertEquals(new BigDecimal("8.5"), sottomissione.getValutazione().getPunteggio());
        assertEquals(1, valutazioneRepository.numeroSalvataggi);
    }

    @Test
    void restituisceConflictSeHackathonNonPiuValutabile() throws Exception {
        hackathon.registraPartecipazioneVincitrice(sottomissione.getPartecipazione());
        hackathon.concludi();

        // Il repository finto conserva il candidato per verificare il ricontrollo.
        mockMvc.perform(get(
                        "/api/valutazioni/hackathons/{hackathonId}/sottomissioni",
                        hackathon.getId()
                ))
                .andExpect(status().isConflict());

        mockMvc.perform(post(
                        "/api/valutazioni/hackathons/{hackathonId}/sottomissioni/{sottomissioneId}",
                        hackathon.getId(), sottomissione.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"giudizio":"Buon lavoro","punteggio":8}
                                """))
                .andExpect(status().isConflict());

        assertNull(sottomissione.getValutazione());
        assertEquals(0, valutazioneRepository.numeroSalvataggi);
    }

    @Test
    void restituisceConflictSeSottomissioneValutataPrimaDellaConferma()
            throws Exception {
        ValutareSottomissioneControl control = new ValutareSottomissioneControl(
                new HackathonRepositoryFinto(hackathon),
                new PartecipazioneRepositoryFinto(sottomissione.getPartecipazione()),
                valutazioneRepository
        ) {
            @Override
            public void verificaDatiValutazione(DatiValutazione dati) {
                super.verificaDatiValutazione(dati);
                // Simula una valutazione intervenuta dopo la selezione.
                Valutazione.crea(sottomissione, hackathon.getGiudice(), dati);
            }
        };

        MockMvc mvc = standaloneSetup(
                new ValutareSottomissioneBoundary(control, sessione)
        ).setControllerAdvice(new ErroriRichiestaHandler()).build();

        mvc.perform(post(
                        "/api/valutazioni/hackathons/{hackathonId}/sottomissioni/{sottomissioneId}",
                        hackathon.getId(), sottomissione.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"giudizio":"Buon lavoro","punteggio":8}
                                """))
                .andExpect(status().isConflict());

        assertNotNull(sottomissione.getValutazione());
        assertNull(valutazioneRepository.valutazioneSalvata);
        assertEquals(0, valutazioneRepository.numeroSalvataggi);
    }

    @Test
    void mantieneUnauthorizedSenzaSessione() throws Exception {
        sessione.svuota();

        mockMvc.perform(get("/api/valutazioni/hackathons"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(
                        "/api/valutazioni/hackathons/{hackathonId}/sottomissioni",
                        hackathon.getId()
                ))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(
                        "/api/valutazioni/hackathons/{hackathonId}/sottomissioni/{sottomissioneId}",
                        hackathon.getId(), sottomissione.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"giudizio":"Buon lavoro","punteggio":8}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messaggio").value("Accesso richiesto"));

        assertNull(sottomissione.getValutazione());
        assertEquals(0, valutazioneRepository.numeroSalvataggi);
    }

    private Hackathon creaHackathonInValutazione(Utente giudice) {
        LocalDate oggi = LocalDate.now();

        DatiHackathon dati = new DatiHackathon(
                "HackHub 2026",
                "Regolamento ufficiale",
                "Qualità e innovazione",
                oggi.minusDays(10),
                oggi.minusDays(5),
                oggi.minusDays(1),
                "Camerino",
                BigDecimal.valueOf(5000),
                5
        );

        Hackathon risultato = Hackathon.crea(
                dati,
                new Utente(1L),
                giudice,
                List.of(new Utente(3L))
        );

        risultato.aggiornaStato(oggi);
        return risultato;
    }

    private static class HackathonRepositoryFinto
            implements HackathonRepository {

        private final Hackathon hackathon;

        private HackathonRepositoryFinto(Hackathon hackathon) {
            this.hackathon = hackathon;
        }

        @Override
        public List<Hackathon> ottieniHackathonValutabili(Utente giudice) {
            if (Objects.equals(
                    hackathon.getGiudice().getId(),
                    giudice.getId()
            )) {
                return List.of(hackathon);
            }
            return List.of();
        }

        @Override
        public List<Hackathon> ottieniHackathonSegnalabili(Utente mentore) {
            return List.of();
        }

        @Override
        public void salva(Hackathon hackathon) {
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
        public Hackathon recuperaHackathon(Long hackathonId) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }

    private static class PartecipazioneRepositoryFinto
            implements PartecipazioneRepository {

        private final Partecipazione partecipazione;

        private PartecipazioneRepositoryFinto(
                Partecipazione partecipazione
        ) {
            this.partecipazione = partecipazione;
        }

        @Override
        public List<Partecipazione> ottieniPartecipazioni(
                Hackathon hackathon
        ) {
            if (partecipazione.getHackathon() == hackathon) {
                return List.of(partecipazione);
            }
            return List.of();
        }

        @Override
        public List<Partecipazione> recuperaPartecipazioniNonEscluse(
                Hackathon hackathon
        ) {
            return ottieniPartecipazioni(hackathon);
        }

        @Override
        public void salva(Partecipazione partecipazione) {
        }

        @Override
        public boolean esistePartecipazione(Team team, Hackathon hackathon) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }

        @Override
        public Partecipazione recuperaPartecipazione(
                Team team,
                Hackathon hackathon
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }

        @Override
        public List<Partecipazione> recuperaPartecipazioniInHackathonNonConclusi(
                Team team
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }

    private static class ValutazioneRepositoryFinto
            implements ValutazioneRepository {

        private RuntimeException erroreSalvataggio;
        private Valutazione valutazioneSalvata;
        private int numeroSalvataggi;

        @Override
        public void salva(Valutazione valutazione) {
            if (erroreSalvataggio != null) {
                throw erroreSalvataggio;
            }

            valutazioneSalvata = valutazione;
            numeroSalvataggi++;
        }
    }
}