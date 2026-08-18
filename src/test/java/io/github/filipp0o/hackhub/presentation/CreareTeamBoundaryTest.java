package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.CreareTeamControl;
import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class CreareTeamBoundaryTest {

    private TeamRepositoryFinto teamRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void configuraBoundary() {
        teamRepository = new TeamRepositoryFinto();

        CreareTeamControl control =
                new CreareTeamControl(teamRepository);

        mockMvc = standaloneSetup(
                new CreareTeamBoundary(control)
        ).build();
    }

    @Test
    void creaTeamTramiteApiRest() throws Exception {
        mockMvc.perform(
                        post("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "utenteId": 1,
                                          "nome": "ByteBuilders"
                                        }
                                        """)
                )
                .andExpect(status().isCreated());

        Team teamSalvato = teamRepository.teamSalvato;

        assertAll(
                () -> assertNotNull(teamSalvato),
                () -> assertEquals(
                        "ByteBuilders",
                        teamSalvato.getNome()
                ),
                () -> assertEquals(
                        1L,
                        teamSalvato
                                .getResponsabile()
                                .getId()
                ),
                () -> assertTrue(
                        teamSalvato.getMembri().stream()
                                .anyMatch(utente ->
                                        utente.getId().equals(1L)
                                )
                )
        );
    }

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new CreareTeamBoundary(null)
        );
    }

    private static class TeamRepositoryFinto
            implements TeamRepository {

        private Team teamSalvato;

        @Override
        public boolean verificaAppartenenzaTeam(
                Utente utente
        ) {
            return false;
        }

        @Override
        public void salva(Team team) {
            teamSalvato = team;
        }
    }
}