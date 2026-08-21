package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.application.SegnalareViolazioneControl;
import io.github.filipp0o.hackhub.application.SegnalazioneRepository;
import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.NotificaSegnalazione;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Segnalazione;
import io.github.filipp0o.hackhub.domain.StatoSegnalazione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SegnalareViolazioneBoundaryTest {

    private Hackathon hackathon;
    private Partecipazione partecipazione;
    private SegnalazioneRepositoryFinto segnalazioneRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void configuraBoundary() {
        Utente organizzatore = new Utente(1L);
        Utente mentore = new Utente(3L);

        hackathon = creaHackathonInCorso(
                organizzatore,
                mentore
        );

        Utente responsabile = new Utente(4L);

        Team team = Team.crea(
                "Team Alpha",
                responsabile,
                responsabile
        );

        partecipazione = new Partecipazione(
                hackathon,
                team
        );

        segnalazioneRepository =
                new SegnalazioneRepositoryFinto();

        SegnalareViolazioneControl control =
                new SegnalareViolazioneControl(
                        new HackathonRepositoryFinto(
                                hackathon
                        ),
                        new PartecipazioneRepositoryFinto(
                                partecipazione
                        ),
                        segnalazioneRepository
                );

        mockMvc = standaloneSetup(
                new SegnalareViolazioneBoundary(control)
        ).build();
    }

    @Test
    void restituisceHackathonSegnalabili()
            throws Exception {
        mockMvc.perform(
                        get("/api/segnalazioni/hackathons")
                                .param("mentoreId", "3")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(hackathon.getId().intValue()))
                .andExpect(jsonPath("$[0].nome")
                        .value("HackHub 2026"));
    }

    @Test
    void restituiscePartecipazioniSegnalabili()
            throws Exception {
        mockMvc.perform(
                        get(
                                "/api/segnalazioni/hackathons/{hackathonId}/partecipazioni",
                                hackathon.getId()
                        ).param("mentoreId", "3")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(
                                partecipazione
                                        .getId()
                                        .intValue()
                        ))
                .andExpect(jsonPath("$[0].nomeTeam")
                        .value("Team Alpha"))
                .andExpect(jsonPath("$[0].responsabileId")
                        .value(4))
                .andExpect(jsonPath("$[0].regolamento")
                        .value("Regolamento ufficiale"));
    }

    @Test
    void registraSegnalazioneENotificaTramiteApiRest()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/api/segnalazioni/hackathons/{hackathonId}/partecipazioni/{partecipazioneId}",
                                hackathon.getId(),
                                partecipazione.getId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "mentoreId": 3,
                                          "descrizione": "Uso di materiale non consentito"
                                        }
                                        """)
                )
                .andExpect(status().isCreated());

        Segnalazione segnalazione =
                segnalazioneRepository.segnalazioneSalvata;

        NotificaSegnalazione notifica =
                segnalazioneRepository.notificaSalvata;

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
                        partecipazione,
                        segnalazione.getPartecipazione()
                ),
                () -> assertEquals(
                        3L,
                        segnalazione
                                .getMentoreSegnalante()
                                .getId()
                ),
                () -> assertSame(
                        segnalazione,
                        notifica.getSegnalazione()
                ),
                () -> assertEquals(
                        1L,
                        notifica.getDestinatario().getId()
                ),
                () -> assertFalse(notifica.getLetta())
        );
    }

    @Test
    void restituisceNotFoundPerPartecipazioneSconosciuta()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/api/segnalazioni/hackathons/{hackathonId}/partecipazioni/{partecipazioneId}",
                                hackathon.getId(),
                                Long.MAX_VALUE
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "mentoreId": 3,
                                          "descrizione": "Violazione"
                                        }
                                        """)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new SegnalareViolazioneBoundary(null)
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

    private static class HackathonRepositoryFinto
            implements HackathonRepository {

        private final Hackathon hackathon;

        private HackathonRepositoryFinto(
                Hackathon hackathon
        ) {
            this.hackathon = hackathon;
        }

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
            boolean assegnato = hackathon.getMentori()
                    .stream()
                    .anyMatch(utente -> Objects.equals(
                            utente.getId(),
                            mentore.getId()
                    ));

            return assegnato
                    ? List.of(hackathon)
                    : List.of();
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
        public boolean esistePartecipazione(
                Team team,
                Hackathon hackathon
        ) {
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
    }

    private static class SegnalazioneRepositoryFinto
            implements SegnalazioneRepository {

        private Segnalazione segnalazioneSalvata;
        private NotificaSegnalazione notificaSalvata;

        @Override
        public List<Segnalazione> ottieniSegnalazioniDaEsaminare(
                Utente organizzatore
        ) {
            return List.of();
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
            notificaSalvata = notifica;
        }

        @Override
        public void salvaNotifica(
                NotificaSegnalazione notifica
        ) {
            notificaSalvata = notifica;
        }
    }
}