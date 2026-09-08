package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.EsaminareSegnalazioneControl;
import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.application.SegnalazioneRepository;
import io.github.filipp0o.hackhub.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class EsaminareSegnalazioneBoundaryTest {

    private Segnalazione segnalazione;
    private Partecipazione partecipazione;
    private SegnalazioneRepositoryFinto segnalazioneRepository;
    private PartecipazioneRepositoryFinto partecipazioneRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void configuraBoundary() {
        SessioneUtente sessione = new SessioneUtente();
        sessione.registra(new Utente(1L));

        Utente organizzatore = new Utente(1L);
        Utente mentore = new Utente(3L);

        Hackathon hackathon = creaHackathonInCorso(
                organizzatore,
                mentore
        );

        Utente responsabile = new Utente(4L);
        Team team = Team.crea("Team Alpha", responsabile, responsabile);

        partecipazione = new Partecipazione(hackathon, team);

        segnalazione = Segnalazione.crea(
                mentore,
                partecipazione,
                "Uso di materiale non consentito"
        );

        segnalazioneRepository =
                new SegnalazioneRepositoryFinto(segnalazione);

        partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        EsaminareSegnalazioneControl control =
                new EsaminareSegnalazioneControl(
                        segnalazioneRepository,
                        partecipazioneRepository
                );

        mockMvc = standaloneSetup(
                new EsaminareSegnalazioneBoundary(control, sessione)
        ).build();
    }

    @Test
    void restituisceSegnalazioniDaEsaminare() throws Exception {
        mockMvc.perform(get("/api/segnalazioni/da-esaminare"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(segnalazione.getId().intValue()))
                .andExpect(jsonPath("$[0].descrizione")
                        .value("Uso di materiale non consentito"))
                .andExpect(jsonPath("$[0].nomeTeam")
                        .value("Team Alpha"));
    }

    @Test
    void restituisceSegnalazioneSelezionata() throws Exception {
        mockMvc.perform(get(
                        "/api/segnalazioni/{segnalazioneId}",
                        segnalazione.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(segnalazione.getId().intValue()))
                .andExpect(jsonPath("$.descrizione")
                        .value("Uso di materiale non consentito"))
                .andExpect(jsonPath("$.nomeTeam")
                        .value("Team Alpha"));
    }

    @Test
    void registraDecisioneDiArchiviazione() throws Exception {
        mockMvc.perform(post(
                        "/api/segnalazioni/{segnalazioneId}/decisione",
                        segnalazione.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "esito": "ARCHIVIATA",
                                  "motivazione": "Segnalazione non fondata"
                                }
                                """))
                .andExpect(status().isNoContent());

        assertAll(
                () -> assertEquals(
                        StatoSegnalazione.ESAMINATA,
                        segnalazione.getStato()
                ),
                () -> assertEquals(
                        EsitoSegnalazione.ARCHIVIATA,
                        segnalazione.getEsito()
                ),
                () -> assertEquals(
                        "Segnalazione non fondata",
                        segnalazione.getMotivazione()
                ),
                () -> assertEquals(
                        1L,
                        segnalazione.getEsaminatore().getId()
                ),
                () -> assertSame(
                        segnalazione,
                        segnalazioneRepository.segnalazioneSalvata
                ),
                () -> assertEquals(
                        StatoPartecipazione.ATTIVA,
                        partecipazione.getStato()
                ),
                () -> assertNull(
                        partecipazioneRepository.partecipazioneSalvata
                )
        );
    }

    @Test
    void registraDecisioneConEsclusioneDelTeam() throws Exception {
        mockMvc.perform(post(
                        "/api/segnalazioni/{segnalazioneId}/decisione",
                        segnalazione.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "esito": "VIOLAZIONE_CON_ESCLUSIONE",
                                  "motivazione": "Violazione confermata"
                                }
                                """))
                .andExpect(status().isNoContent());

        assertAll(
                () -> assertEquals(
                        StatoSegnalazione.ESAMINATA,
                        segnalazione.getStato()
                ),
                () -> assertEquals(
                        StatoPartecipazione.ESCLUSA,
                        partecipazione.getStato()
                ),
                () -> assertSame(
                        partecipazione,
                        partecipazioneRepository.partecipazioneSalvata
                ),
                () -> assertSame(
                        segnalazione,
                        segnalazioneRepository.segnalazioneSalvata
                )
        );
    }

    @Test
    void restituisceNotFoundPerSegnalazioneSconosciuta() throws Exception {
        mockMvc.perform(get(
                        "/api/segnalazioni/{segnalazioneId}",
                        Long.MAX_VALUE
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new EsaminareSegnalazioneBoundary(
                        null,
                        new SessioneUtente()
                )
        );
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

    private static class SegnalazioneRepositoryFinto
            implements SegnalazioneRepository {

        private final Segnalazione segnalazione;
        private Segnalazione segnalazioneSalvata;

        private SegnalazioneRepositoryFinto(Segnalazione segnalazione) {
            this.segnalazione = segnalazione;
        }

        @Override
        public List<Segnalazione> ottieniSegnalazioniDaEsaminare(
                Utente organizzatore
        ) {
            Long organizzatoreHackathonId = segnalazione
                    .getPartecipazione()
                    .getHackathon()
                    .getOrganizzatore()
                    .getId();

            boolean autorizzato = Objects.equals(
                    organizzatoreHackathonId,
                    organizzatore.getId()
            );

            boolean daEsaminare = segnalazione.getStato()
                    == StatoSegnalazione.DA_ESAMINARE;

            return autorizzato && daEsaminare
                    ? List.of(segnalazione)
                    : List.of();
        }

        @Override
        public void salva(Segnalazione segnalazione) {
            segnalazioneSalvata = segnalazione;
        }

        @Override
        public void salvaConNotifica(
                Segnalazione segnalazione,
                NotificaSegnalazione notifica
        ) {
            segnalazioneSalvata = segnalazione;
        }

        @Override
        public void salvaNotifica(NotificaSegnalazione notifica) {
        }
    }

    private static class PartecipazioneRepositoryFinto
            implements PartecipazioneRepository {

        private Partecipazione partecipazioneSalvata;

        @Override
        public List<Partecipazione> ottieniPartecipazioni(
                Hackathon hackathon
        ) {
            return List.of();
        }

        @Override
        public List<Partecipazione> recuperaPartecipazioniNonEscluse(
                Hackathon hackathon
        ) {
            return List.of();
        }

        @Override
        public void salva(Partecipazione partecipazione) {
            partecipazioneSalvata = partecipazione;
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
}