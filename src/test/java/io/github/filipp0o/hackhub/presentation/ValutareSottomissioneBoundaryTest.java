package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.application.ValutareSottomissioneControl;
import io.github.filipp0o.hackhub.application.ValutazioneRepository;
import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.domain.Valutazione;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ValutareSottomissioneBoundaryTest {

    private Hackathon hackathon;
    private Sottomissione sottomissione;
    private ValutazioneRepositoryFinto valutazioneRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void configuraBoundary() {
        Utente giudice = new Utente(2L);
        hackathon = creaHackathonInValutazione(giudice);

        Utente responsabile = new Utente(4L);

        Team team = Team.crea(
                "Team Alpha",
                responsabile,
                responsabile
        );

        Partecipazione partecipazione =
                new Partecipazione(hackathon, team);

        sottomissione = new Sottomissione(
                partecipazione,
                "Repository del progetto"
        );

        valutazioneRepository =
                new ValutazioneRepositoryFinto();

        ValutareSottomissioneControl control =
                new ValutareSottomissioneControl(
                        new HackathonRepositoryFinto(
                                hackathon
                        ),
                        new PartecipazioneRepositoryFinto(
                                partecipazione
                        ),
                        valutazioneRepository
                );

        mockMvc = standaloneSetup(
                new ValutareSottomissioneBoundary(control)
        ).build();
    }

    @Test
    void restituisceHackathonValutabili()
            throws Exception {
        mockMvc.perform(
                        get("/api/valutazioni/hackathons")
                                .param("giudiceId", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(hackathon.getId().intValue()))
                .andExpect(jsonPath("$[0].nome")
                        .value("HackHub 2026"));
    }

    @Test
    void restituisceSottomissioniDaValutare()
            throws Exception {
        mockMvc.perform(
                        get(
                                "/api/valutazioni/hackathons/{hackathonId}/sottomissioni",
                                hackathon.getId()
                        ).param("giudiceId", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(
                                sottomissione
                                        .getId()
                                        .intValue()
                        ))
                .andExpect(jsonPath("$[0].contenuto")
                        .value("Repository del progetto"))
                .andExpect(jsonPath(
                        "$[0].criteriValutazione"
                ).value("Qualità e innovazione"));
    }

    @Test
    void registraValutazioneTramiteApiRest()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/api/valutazioni/hackathons/{hackathonId}/sottomissioni/{sottomissioneId}",
                                hackathon.getId(),
                                sottomissione.getId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "giudiceId": 2,
                                          "giudizio": "Ottimo progetto",
                                          "punteggio": 9
                                        }
                                        """)
                )
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
    void restituisceNotFoundPerHackathonSconosciuto()
            throws Exception {
        mockMvc.perform(
                        get(
                                "/api/valutazioni/hackathons/{hackathonId}/sottomissioni",
                                Long.MAX_VALUE
                        ).param("giudiceId", "2")
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new ValutareSottomissioneBoundary(null)
        );
    }

    private Hackathon creaHackathonInValutazione(
            Utente giudice
    ) {
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

        private HackathonRepositoryFinto(
                Hackathon hackathon
        ) {
            this.hackathon = hackathon;
        }

        @Override
        public List<Hackathon> ottieniHackathonValutabili(
                Utente giudice
        ) {
            if (Objects.equals(
                    hackathon.getGiudice().getId(),
                    giudice.getId()
            )) {
                return List.of(hackathon);
            }

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
        public List<Partecipazione>
        recuperaPartecipazioniNonEscluse(
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
    }

    private static class ValutazioneRepositoryFinto
            implements ValutazioneRepository {

        private Valutazione valutazioneSalvata;

        @Override
        public void salva(Valutazione valutazione) {
            valutazioneSalvata = valutazione;
        }
    }
}