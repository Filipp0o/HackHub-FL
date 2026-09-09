package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.CreareTeamControl;
import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class CreareTeamBoundaryTest {

    private TeamRepositoryFinto teamRepository;
    private SessioneUtente sessione;
    private MockMvc mockMvc;

    @BeforeEach
    void configuraBoundary() {
        sessione = new SessioneUtente();
        sessione.registra(new Utente(1L));
        teamRepository = new TeamRepositoryFinto();

        CreareTeamControl control = new CreareTeamControl(teamRepository);

        mockMvc = standaloneSetup(
                new CreareTeamBoundary(control, sessione)
        ).setControllerAdvice(new ErroriRichiestaHandler()).build();
    }

    @Test
    void creaTeamTramiteApiRest() throws Exception {
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"ByteBuilders\"}"))
                .andExpect(status().isCreated());

        Team team = teamRepository.teamSalvato;
        assertNotNull(team);

        assertAll(
                () -> assertEquals("ByteBuilders", team.getNome()),
                () -> assertEquals(1L, team.getResponsabile().getId()),
                () -> assertEquals(1, team.numeroMembri()),
                () -> assertEquals(1L, team.getMembri().get(0).getId()),
                () -> assertEquals(1, teamRepository.tentativiSalvataggio)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"nome\":\"\"}",
            "{\"nome\":\"   \"}"
    })
    void rifiutaNomeNonValido(String richiesta) throws Exception {
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messaggio")
                        .value("Il nome del team è obbligatorio"));

        assertNull(teamRepository.teamSalvato);
        assertEquals(0, teamRepository.tentativiSalvataggio);
    }

    @Test
    void rifiutaUtenteGiaInTeam() throws Exception {
        teamRepository.appartieneGiaAUnTeam = true;

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"ByteBuilders\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messaggio")
                        .value("L'utente appartiene già a un team"));

        assertNull(teamRepository.teamSalvato);
        assertEquals(0, teamRepository.tentativiSalvataggio);
    }

    @Test
    void comunicaFallimentoSalvataggio() throws Exception {
        teamRepository.fallisceSalvataggio = true;

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"ByteBuilders\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.messaggio")
                        .value("Il team non è stato creato"));

        assertNull(teamRepository.teamSalvato);
        assertEquals(1, teamRepository.tentativiSalvataggio);
    }

    @Test
    void rifiutaSessioneNonAutenticata() throws Exception {
        sessione.svuota();

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"ByteBuilders\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messaggio").value("Accesso richiesto"));

        assertNull(teamRepository.teamSalvato);
        assertEquals(0, teamRepository.tentativiSalvataggio);
    }

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new CreareTeamBoundary(null, new SessioneUtente())
        );
    }

    @Test
    void avviaCreazioneERestituisceRiepilogoSenzaSalvare() throws Exception {
        mockMvc.perform(get("/api/teams/creazione"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsabileId").value(1))
                .andExpect(jsonPath("$.messaggio").value(
                        "Inserire il nome del team: ne diventerai automaticamente membro e responsabile"
                ));

        mockMvc.perform(post("/api/teams/verifica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"ByteBuilders\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("ByteBuilders"))
                .andExpect(jsonPath("$.responsabileId").value(1));

        assertNull(teamRepository.teamSalvato);
        assertEquals(0, teamRepository.tentativiSalvataggio);

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"ByteBuilders\"}"))
                .andExpect(status().isCreated());

        assertEquals(1, teamRepository.tentativiSalvataggio);
    }

    @Test
    void avvioRifiutatoSeUtenteGiaInTeam() throws Exception {
        teamRepository.appartieneGiaAUnTeam = true;

        mockMvc.perform(get("/api/teams/creazione"))
                .andExpect(status().isConflict());

        assertEquals(0, teamRepository.tentativiSalvataggio);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"nome\":\"   \"}",
            "{",
            "null",
            ""
    })
    void correggeNomePrimaDelRiepilogo(String richiesta) throws Exception {
        mockMvc.perform(post("/api/teams/verifica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/teams/verifica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Corretto\"}"))
                .andExpect(status().isOk());

        assertEquals(0, teamRepository.tentativiSalvataggio);
    }

    @Test
    void ricontrollaAppartenenzaENomeDopoRiepilogo() throws Exception {
        mockMvc.perform(post("/api/teams/verifica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"ByteBuilders\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"   \"}"))
                .andExpect(status().isBadRequest());

        teamRepository.appartieneGiaAUnTeam = true;

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"ByteBuilders\"}"))
                .andExpect(status().isConflict());

        assertNull(teamRepository.teamSalvato);
        assertEquals(0, teamRepository.tentativiSalvataggio);
    }

    @Test
    void avvioEVerificaRichiedonoSessione() throws Exception {
        sessione.svuota();

        mockMvc.perform(get("/api/teams/creazione"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/teams/verifica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"ByteBuilders\"}"))
                .andExpect(status().isUnauthorized());

        assertEquals(0, teamRepository.tentativiSalvataggio);
    }

    private static class TeamRepositoryFinto implements TeamRepository {

        private Team teamSalvato;
        private boolean appartieneGiaAUnTeam;
        private boolean fallisceSalvataggio;
        private int tentativiSalvataggio;

        @Override
        public boolean verificaAppartenenzaTeam(Utente utente) {
            return appartieneGiaAUnTeam;
        }

        @Override
        public void salva(Team team) {
            tentativiSalvataggio++;

            if (fallisceSalvataggio) {
                throw new IllegalStateException(
                        "Errore simulato del repository"
                );
            }

            teamSalvato = team;
        }

        @Override
        public Team recuperaTeam(Utente utente) {
            throw new UnsupportedOperationException("Non utilizzato in questo test");
        }
    }
}