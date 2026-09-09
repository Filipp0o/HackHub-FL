package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.application.SegnalareViolazioneControl;
import io.github.filipp0o.hackhub.application.SegnalazioneRepository;
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

class SegnalareViolazioneBoundaryTest {

    private SessioneUtente sessione;
    private Hackathon hackathon;
    private Partecipazione partecipazione;
    private SegnalazioneRepositoryFinto segnalazioneRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void configuraBoundary() {
        sessione = new SessioneUtente();
        sessione.registra(new Utente(3L));

        Utente organizzatore = new Utente(1L);
        Utente mentore = new Utente(3L);

        hackathon = creaHackathonInCorso(organizzatore, mentore);
        hackathon.assegnaId(1L);

        Utente responsabile = new Utente(4L);
        Team team = Team.crea("Team Alpha", responsabile, responsabile);

        partecipazione = new Partecipazione(hackathon, team);
        partecipazione.assegnaId(1L);

        segnalazioneRepository = new SegnalazioneRepositoryFinto();

        SegnalareViolazioneControl control = new SegnalareViolazioneControl(
                new HackathonRepositoryFinto(hackathon),
                new PartecipazioneRepositoryFinto(partecipazione),
                segnalazioneRepository
        );

        mockMvc = standaloneSetup(
                new SegnalareViolazioneBoundary(control, sessione)
        ).setControllerAdvice(new ErroriRichiestaHandler()).build();
    }

    @Test
    void restituisceHackathonSegnalabili() throws Exception {
        mockMvc.perform(get("/api/segnalazioni/hackathons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(hackathon.getId().intValue()))
                .andExpect(jsonPath("$[0].nome").value("HackHub 2026"));
    }

    @Test
    void restituiscePartecipazioniSegnalabili() throws Exception {
        mockMvc.perform(get(
                        "/api/segnalazioni/hackathons/{hackathonId}/partecipazioni",
                        hackathon.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(partecipazione.getId().intValue()))
                .andExpect(jsonPath("$[0].nomeTeam").value("Team Alpha"))
                .andExpect(jsonPath("$[0].responsabileId").value(4))
                .andExpect(jsonPath("$[0].regolamento")
                        .value("Regolamento ufficiale"));
    }

    @Test
    void registraSegnalazioneENotificaTramiteApiRest() throws Exception {
        mockMvc.perform(post(
                        "/api/segnalazioni/hackathons/{hackathonId}/partecipazioni/{partecipazioneId}",
                        hackathon.getId(),
                        partecipazione.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "descrizione": "Uso di materiale non consentito"
                                }
                                """))
                .andExpect(status().isCreated());

        Segnalazione segnalazione = segnalazioneRepository.segnalazioneSalvata;
        NotificaSegnalazione notifica = segnalazioneRepository.notificaSalvata;

        assertAll(
                () -> assertNotNull(segnalazione),
                () -> assertNotNull(notifica),
                () -> assertEquals(
                        "Uso di materiale non consentito",
                        segnalazione.getDescrizione()
                ),
                () -> assertEquals(
                        StatoSegnalazione.DA_ESAMINARE,
                        segnalazione.getStato()
                ),
                () -> assertSame(
                        partecipazione, segnalazione.getPartecipazione()
                ),
                () -> assertEquals(
                        3L, segnalazione.getMentoreSegnalante().getId()
                ),
                () -> assertSame(
                        segnalazione, notifica.getSegnalazione()
                ),
                () -> assertEquals(
                        1L, notifica.getDestinatario().getId()
                ),
                () -> assertFalse(notifica.getLetta())
        );
    }

    @Test
    void restituisceNotFoundPerPartecipazioneSconosciuta() throws Exception {
        mockMvc.perform(post(
                        "/api/segnalazioni/hackathons/{hackathonId}/partecipazioni/{partecipazioneId}",
                        hackathon.getId(),
                        Long.MAX_VALUE
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "descrizione": "Violazione"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new SegnalareViolazioneBoundary(null, new SessioneUtente())
        );
    }

    @Test
    void verificaDescrizioneSenzaSalvareSegnalazioneONotifica() throws Exception {
        mockMvc.perform(post(
                        "/api/segnalazioni/hackathons/1/partecipazioni/1/verifica"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descrizione\":\"Violazione del regolamento\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hackathonId").value(1))
                .andExpect(jsonPath("$.partecipazioneId").value(1))
                .andExpect(jsonPath("$.descrizione")
                        .value("Violazione del regolamento"));

        assertNull(segnalazioneRepository.segnalazioneSalvata);
        assertNull(segnalazioneRepository.notificaSalvata);
        assertEquals(0, segnalazioneRepository.numeroSalvataggi);

        mockMvc.perform(post("/api/segnalazioni/hackathons/1/partecipazioni/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descrizione\":\"Violazione del regolamento\"}"))
                .andExpect(status().isCreated());

        assertNotNull(segnalazioneRepository.notificaSalvata);
        assertEquals(1, segnalazioneRepository.numeroSalvataggi);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"descrizione\":null}",
            "{\"descrizione\":\"   \"}",
            "{",
            "null",
            ""
    })
    void correggeDescrizionePrimaDelRiepilogo(String richiesta) throws Exception {
        mockMvc.perform(post(
                        "/api/segnalazioni/hackathons/1/partecipazioni/1/verifica"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(
                        "/api/segnalazioni/hackathons/1/partecipazioni/1/verifica"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descrizione\":\"Corretta\"}"))
                .andExpect(status().isOk());

        assertEquals(0, segnalazioneRepository.numeroSalvataggi);
    }

    @Test
    void confermaRicontrollaDescrizioneEPartecipazione() throws Exception {
        mockMvc.perform(post(
                        "/api/segnalazioni/hackathons/1/partecipazioni/1/verifica"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descrizione\":\"Violazione\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/segnalazioni/hackathons/1/partecipazioni/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descrizione\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/segnalazioni/hackathons/1/partecipazioni/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descrizione\":\"Violazione\"}"))
                .andExpect(status().isNotFound());

        assertEquals(0, segnalazioneRepository.numeroSalvataggi);
    }

    @Test
    void verificaRichiedeSessione() throws Exception {
        sessione.svuota();

        mockMvc.perform(post(
                        "/api/segnalazioni/hackathons/1/partecipazioni/1/verifica"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descrizione\":\"Violazione\"}"))
                .andExpect(status().isUnauthorized());

        assertEquals(0, segnalazioneRepository.numeroSalvataggi);
    }

    private Hackathon creaHackathonInCorso(
            Utente organizzatore,
            Utente mentore
    ) {
        LocalDate oggi = LocalDate.now();

        DatiHackathon dati = new DatiHackathon(
                "HackHub 2026",
                "Regolamento ufficiale",
                "Criteri di valutazione",
                oggi.minusDays(5),
                oggi.minusDays(2),
                oggi.plusDays(2),
                "Camerino",
                BigDecimal.valueOf(5000),
                5
        );

        Hackathon risultato = Hackathon.crea(
                dati,
                organizzatore,
                new Utente(2L),
                List.of(mentore)
        );

        risultato.aggiornaStato(oggi);
        return risultato;
    }

    private static class HackathonRepositoryFinto implements HackathonRepository {

        private final Hackathon hackathon;

        private HackathonRepositoryFinto(Hackathon hackathon) {
            this.hackathon = hackathon;
        }

        @Override
        public List<Hackathon> ottieniHackathonValutabili(Utente giudice) {
            return List.of();
        }

        @Override
        public List<Hackathon> ottieniHackathonSegnalabili(Utente mentore) {
            boolean assegnato = hackathon.getMentori().stream()
                    .anyMatch(utente -> Objects.equals(
                            utente.getId(), mentore.getId()
                    ));

            return assegnato ? List.of(hackathon) : List.of();
        }

        @Override
        public void salva(Hackathon hackathon) {
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

    private static class PartecipazioneRepositoryFinto
            implements PartecipazioneRepository {

        private final Partecipazione partecipazione;

        private PartecipazioneRepositoryFinto(Partecipazione partecipazione) {
            this.partecipazione = partecipazione;
        }

        @Override
        public List<Partecipazione> ottieniPartecipazioni(Hackathon hackathon) {
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
            throw new UnsupportedOperationException("Non utilizzato in questo test");
        }

        @Override
        public Partecipazione recuperaPartecipazione(
                Team team,
                Hackathon hackathon
        ) {
            throw new UnsupportedOperationException("Non utilizzato in questo test");
        }

        @Override
        public List<Partecipazione> recuperaPartecipazioniInHackathonNonConclusi(
                Team team
        ) {
            throw new UnsupportedOperationException("Non utilizzato in questo test");
        }
    }

    private static class SegnalazioneRepositoryFinto
            implements SegnalazioneRepository {

        private Segnalazione segnalazioneSalvata;
        private NotificaSegnalazione notificaSalvata;
        private int numeroSalvataggi;

        @Override
        public List<Segnalazione> ottieniSegnalazioniDaEsaminare(
                Utente organizzatore
        ) {
            return List.of();
        }

        @Override
        public void salva(Segnalazione segnalazione) {
            numeroSalvataggi++;
            segnalazioneSalvata = segnalazione;
        }

        @Override
        public void salvaConNotifica(
                Segnalazione segnalazione,
                NotificaSegnalazione notifica
        ) {
            numeroSalvataggi++;
            segnalazioneSalvata = segnalazione;
            notificaSalvata = notifica;
        }

        @Override
        public void salvaNotifica(NotificaSegnalazione notifica) {
            numeroSalvataggi++;
            notificaSalvata = notifica;
        }
    }
}