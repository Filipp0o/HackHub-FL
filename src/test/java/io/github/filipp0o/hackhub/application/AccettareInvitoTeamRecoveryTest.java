package io.github.filipp0o.hackhub.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipp0o.hackhub.application.AccettareInvitoTeamControl.*;
import io.github.filipp0o.hackhub.domain.Invito;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.infrastructure.InMemoryInvitoRepository;
import io.github.filipp0o.hackhub.infrastructure.InMemoryPartecipazioneRepository;
import io.github.filipp0o.hackhub.infrastructure.InMemoryTeamRepository;
import io.github.filipp0o.hackhub.presentation.AccettareInvitoTeamBoundary;
import io.github.filipp0o.hackhub.presentation.SessioneUtente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccettareInvitoTeamRecoveryTest {

    enum PuntoGuasto {
        INVITO_PRIMA, INVITO_DOPO, TEAM_PRIMA, TEAM_DOPO
    }

    static Stream<Arguments> guasti() {
        return Stream.of(PuntoGuasto.values()).flatMap(punto -> Stream.of(
                new RuntimeException("Dettagli interni del repository"),
                new IllegalArgumentException("Dati del repository non validi"),
                new IllegalStateException("Repository non disponibile"),
                new InvitoNonDisponibileException(),
                new UtenteGiaInTeamException(),
                new DimensioneMassimaSuperataException()
        ).map(causa -> Arguments.of(punto, causa)));
    }

    @ParameterizedTest
    @MethodSource("guasti")
    void ripristinaOggettiERepositoryEConsenteNuovoTentativo(
            PuntoGuasto punto, RuntimeException causa
    ) {
        Scenario s = new Scenario();
        s.punto = punto;
        s.causa = causa;

        var errore = assertThrows(AccettazioneInvitoFallitaException.class,
                () -> s.control.richiediAccettazioneInvito(s.destinatario, s.invito));

        assertSame(causa, errore.getCause());
        assertEquals("Accettazione non completata", errore.getMessage());
        s.verificaRipristino();
        assertEquals(1, s.tentativiInvito);
        assertEquals(punto.name().startsWith("INVITO") ? 0 : 1, s.tentativiTeam);

        s.punto = null;
        assertDoesNotThrow(() ->
                s.control.richiediAccettazioneInvito(s.destinatario, s.invito));
        s.verificaSuccesso();
    }

    @ParameterizedTest
    @MethodSource("guasti")
    void httpRestituisce500PoiPermetteDiAccettareLoStessoInvito(
            PuntoGuasto punto, RuntimeException causa
    ) throws Exception {
        Scenario s = new Scenario();
        MockMvc mvc = s.mvc();
        s.punto = punto;
        s.causa = causa;
        String richiesta = "{\"invitoId\":" + s.invito.getId() + "}";

        var risposta = mvc.perform(post("/api/inviti/accettazione")
                        .contentType(MediaType.APPLICATION_JSON).content(richiesta))
                .andExpect(status().isInternalServerError()).andReturn();
        var corpo = new ObjectMapper().readTree(
                risposta.getResponse().getContentAsString());
        assertEquals("Accettazione non completata", corpo.get("messaggio").asText());
        assertTrue(corpo.get("invitiRicevuti").isEmpty());
        s.verificaRipristino();

        var elenco = mvc.perform(get("/api/inviti/ricevuti"))
                .andExpect(status().isOk()).andReturn();
        var ricevuti = new ObjectMapper().readTree(
                elenco.getResponse().getContentAsString()).get("invitiRicevuti");
        assertEquals(1, ricevuti.size());
        assertEquals(s.invito.getId().longValue(), ricevuti.get(0).get("id").asLong());

        s.punto = null;
        mvc.perform(post("/api/inviti/accettazione")
                        .contentType(MediaType.APPLICATION_JSON).content(richiesta))
                .andExpect(status().isOk());
        s.verificaSuccesso();

        mvc.perform(post("/api/inviti/accettazione")
                        .contentType(MediaType.APPLICATION_JSON).content(richiesta))
                .andExpect(status().isNotFound());
        s.verificaSuccesso();
    }

    @Test
    void richiestaAssenteONonLeggibileRestituisce400ConEsitoAccettazione()
            throws Exception {
        Scenario s = new Scenario();
        MockMvc mvc = s.mvc();

        for (String richiesta : List.of("", "null", "{", "{}",
                "{\"invitoId\":null}", "{\"invitoId\":0}",
                "{\"invitoId\":-1}", "{\"invitoId\":\"abc\"}")) {
            var risposta = mvc.perform(post("/api/inviti/accettazione")
                            .contentType(MediaType.APPLICATION_JSON).content(richiesta))
                    .andExpect(status().isBadRequest()).andReturn();
            var corpo = new ObjectMapper().readTree(
                    risposta.getResponse().getContentAsString());
            assertEquals("È necessario selezionare un invito",
                    corpo.get("messaggio").asText());
            assertTrue(corpo.get("invitiRicevuti").isEmpty());
            s.verificaRipristino();
        }
        assertEquals(0, s.tentativiInvito);
        assertEquals(0, s.tentativiTeam);

        mvc.perform(get("/api/inviti/ricevuti")).andExpect(status().isOk());
        mvc.perform(post("/api/inviti/accettazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invitoId\":" + s.invito.getId() + "}"))
                .andExpect(status().isOk());
        s.verificaSuccesso();
    }

    @Test
    void rifiutoPerAppartenenzaNonRimuoveMembriPreesistenti() {
        Scenario s = new Scenario();
        s.team.aggiungiMembro(s.destinatario);
        var membriPrima = s.team.getMembri();

        assertThrows(UtenteGiaInTeamException.class, () ->
                s.control.richiediAccettazioneInvito(s.destinatario, s.invito));

        assertFalse(s.invito.isAccettato());
        assertEquals(membriPrima, s.team.getMembri());
        assertEquals(0, s.tentativiInvito);
        assertEquals(0, s.tentativiTeam);
    }

    @Test
    void aggiuntaRifiutataRipristinaInvitoSenzaRimuovereIlMembroEsistente() {
        Scenario s = new Scenario();
        s.team.aggiungiMembro(s.destinatario);
        var membriPrima = s.team.getMembri();
        var repositoryIncoerente = new InMemoryTeamRepository() {
            @Override
            public boolean verificaAppartenenzaTeam(Utente utente) {
                return false;
            }
        };
        var control = new AccettareInvitoTeamControl(
                s.inviti, repositoryIncoerente, new InMemoryPartecipazioneRepository());

        var errore = assertThrows(AccettazioneInvitoFallitaException.class,
                () -> control.richiediAccettazioneInvito(s.destinatario, s.invito));

        assertInstanceOf(IllegalArgumentException.class, errore.getCause());
        assertFalse(s.invito.isAccettato());
        assertEquals(membriPrima, s.team.getMembri());
        assertEquals(0, s.tentativiInvito);
    }

    @Test
    void annullamentoMembroUsaLaStessaIstanzaEPreservaIlResponsabile() {
        Scenario s = new Scenario();
        s.team.aggiungiMembro(s.destinatario);

        s.team.annullaAggiuntaMembroNonRegistrata(new Utente(s.destinatario.getId()));
        assertEquals(3, s.team.numeroMembri());

        s.team.annullaAggiuntaMembroNonRegistrata(s.destinatario);
        s.team.annullaAggiuntaMembroNonRegistrata(s.destinatario);
        s.team.annullaAggiuntaMembroNonRegistrata(s.creatore);
        s.team.annullaAggiuntaMembroNonRegistrata(new Utente(s.creatore.getId()));

        assertEquals(List.of(s.creatore, s.membroPreesistente), s.team.getMembri());
        assertSame(s.creatore, s.team.getResponsabile());
    }

    private static class Scenario {
        private final Utente creatore = new Utente(1L);
        private final Utente destinatario = new Utente(2L);
        private final Utente membroPreesistente = new Utente(3L);
        private final Team team = Team.crea("ByteBuilders", creatore, creatore);
        private final Invito invito = Invito.crea(team, destinatario);
        private PuntoGuasto punto;
        private RuntimeException causa;
        private int tentativiInvito;
        private int tentativiTeam;

        private final InMemoryInvitoRepository inviti = new InMemoryInvitoRepository() {
            @Override
            public void salva(Invito valore) {
                tentativiInvito++;
                if (punto == PuntoGuasto.INVITO_PRIMA) throw causa;
                super.salva(valore);
                if (punto == PuntoGuasto.INVITO_DOPO) throw causa;
            }
        };

        private final InMemoryTeamRepository teams = new InMemoryTeamRepository() {
            @Override
            public void salva(Team valore) {
                tentativiTeam++;
                if (punto == PuntoGuasto.TEAM_PRIMA) throw causa;
                super.salva(valore);
                if (punto == PuntoGuasto.TEAM_DOPO) throw causa;
            }
        };

        private final AccettareInvitoTeamControl control = new AccettareInvitoTeamControl(
                inviti, teams, new InMemoryPartecipazioneRepository());

        Scenario() {
            team.aggiungiMembro(membroPreesistente);
            teams.salva(team);
            inviti.salva(invito);
            tentativiInvito = 0;
            tentativiTeam = 0;
        }

        MockMvc mvc() {
            SessioneUtente sessione = new SessioneUtente();
            sessione.registra(destinatario);
            return MockMvcBuilders.standaloneSetup(
                    new AccettareInvitoTeamBoundary(control, sessione)).build();
        }

        void verificaRipristino() {
            assertAll(
                    () -> assertFalse(invito.isAccettato()),
                    () -> assertEquals(List.of(creatore, membroPreesistente), team.getMembri()),
                    () -> assertSame(creatore, team.getResponsabile()),
                    () -> assertSame(team, invito.ottieniTeam()),
                    () -> assertSame(destinatario, invito.getDestinatario()),
                    () -> assertEquals(List.of(invito), inviti.recuperaInvitiRicevuti(destinatario)),
                    () -> assertSame(team, teams.recuperaTeam(creatore)),
                    () -> assertFalse(teams.verificaAppartenenzaTeam(destinatario)),
                    () -> assertEquals(1L, invito.getId()),
                    () -> assertEquals(1L, team.getId())
            );
        }

        void verificaSuccesso() {
            assertAll(
                    () -> assertTrue(invito.isAccettato()),
                    () -> assertEquals(List.of(creatore, membroPreesistente, destinatario),
                            team.getMembri()),
                    () -> assertTrue(inviti.recuperaInvitiRicevuti(destinatario).isEmpty()),
                    () -> assertSame(team, teams.recuperaTeam(destinatario))
            );
        }
    }
}