package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.IscrivereTeamHackathonControl;
import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Team;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class IscrivereTeamHackathonBoundaryTest {

    private Hackathon hackathon;
    private Team team;

    private PartecipazioneRepositoryFinto
            partecipazioneRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void configuraBoundary() {
        Utente responsabile =
                new Utente(1L);

        team = Team.crea(
                "ByteBuilders",
                responsabile,
                responsabile
        );

        hackathon =
                creaHackathonAperto();

        HackathonRepositoryFinto
                hackathonRepository =
                new HackathonRepositoryFinto(
                        hackathon
                );

        TeamRepositoryFinto
                teamRepository =
                new TeamRepositoryFinto(
                        team
                );

        partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        IscrivereTeamHackathonControl control =
                new IscrivereTeamHackathonControl(
                        hackathonRepository,
                        teamRepository,
                        partecipazioneRepository
                );

        mockMvc = standaloneSetup(
                new IscrivereTeamHackathonBoundary(
                        control
                )
        ).build();
    }

    @Test
    void restituisceHackathonApertiAlleIscrizioni()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/iscrizioni/hackathons"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].id")
                                .value(
                                        hackathon
                                                .getId()
                                                .intValue()
                                )
                )
                .andExpect(
                        jsonPath("$[0].nome")
                                .value("HackHub 2026")
                )
                .andExpect(
                        jsonPath(
                                "$[0].dimensioneMassimaTeam"
                        ).value(5)
                );
    }

    @Test
    void iscriveTeamTramiteApiRest()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/iscrizioni/hackathons/{hackathonId}",
                                hackathon.getId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "utenteId": 1
                                        }
                                        """)
                )
                .andExpect(
                        status().isCreated()
                );

        Partecipazione partecipazione =
                partecipazioneRepository
                        .partecipazioneSalvata;

        assertAll(
                () -> assertNotNull(
                        partecipazione
                ),
                () -> assertSame(
                        team,
                        partecipazione.getTeam()
                ),
                () -> assertSame(
                        hackathon,
                        partecipazione.getHackathon()
                ),
                () -> assertEquals(
                        1,
                        partecipazioneRepository
                                .numeroSalvataggi
                )
        );
    }

    @Test
    void restituisceNotFoundPerHackathonNonDisponibile()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/iscrizioni/hackathons/{hackathonId}",
                                Long.MAX_VALUE
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "utenteId": 1
                                        }
                                        """)
                )
                .andExpect(
                        status().isNotFound()
                );
    }

    @Test
    void restituisceConflictSeTeamGiaIscritto()
            throws Exception {

        partecipazioneRepository
                .partecipazioneEsistente =
                true;

        mockMvc.perform(
                        post(
                                "/api/iscrizioni/hackathons/{hackathonId}",
                                hackathon.getId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "utenteId": 1
                                        }
                                        """)
                )
                .andExpect(
                        status().isConflict()
                );

        assertEquals(
                0,
                partecipazioneRepository.numeroSalvataggi
        );
    }

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new IscrivereTeamHackathonBoundary(
                                null
                        )
        );
    }

    private Hackathon creaHackathonAperto() {
        LocalDate oggi =
                LocalDate.now();

        DatiHackathon dati =
                new DatiHackathon(
                        "HackHub 2026",
                        "Regolamento ufficiale",
                        "Criteri di valutazione",
                        oggi.plusDays(1),
                        oggi.plusDays(2),
                        oggi.plusDays(5),
                        "Camerino",
                        BigDecimal.valueOf(5_000),
                        5
                );

        return Hackathon.crea(
                dati,
                new Utente(10L),
                new Utente(20L),
                List.of(
                        new Utente(30L)
                )
        );
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
        public List<Hackathon>
        ottieniHackathonValutabili(
                Utente giudice
        ) {
            return List.of();
        }

        @Override
        public List<Hackathon>
        ottieniHackathonSegnalabili(
                Utente mentore
        ) {
            return List.of();
        }

        @Override
        public List<Hackathon>
        ottieniHackathonApertiAlleIscrizioni() {
            return List.of(hackathon);
        }

        @Override
        public void salva(
                Hackathon hackathon
        ) {
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

    private static class TeamRepositoryFinto
            implements TeamRepository {

        private final Team team;

        private TeamRepositoryFinto(
                Team team
        ) {
            this.team = team;
        }

        @Override
        public boolean verificaAppartenenzaTeam(
                Utente utente
        ) {
            return true;
        }

        @Override
        public Team recuperaTeam(
                Utente utente
        ) {
            return team;
        }

        @Override
        public void salva(
                Team team
        ) {
        }
    }

    private static class PartecipazioneRepositoryFinto
            implements PartecipazioneRepository {

        private boolean
                partecipazioneEsistente;

        private Partecipazione
                partecipazioneSalvata;

        private int numeroSalvataggi;

        @Override
        public List<Partecipazione>
        ottieniPartecipazioni(
                Hackathon hackathon
        ) {
            return List.of();
        }

        @Override
        public List<Partecipazione>
        recuperaPartecipazioniNonEscluse(
                Hackathon hackathon
        ) {
            return List.of();
        }

        @Override
        public boolean esistePartecipazione(
                Team team,
                Hackathon hackathon
        ) {
            return partecipazioneEsistente;
        }

        @Override
        public void salva(
                Partecipazione partecipazione
        ) {
            partecipazioneSalvata =
                    partecipazione;

            numeroSalvataggi++;
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
}