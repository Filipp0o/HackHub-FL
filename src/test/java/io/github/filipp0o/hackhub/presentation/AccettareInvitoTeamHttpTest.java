package io.github.filipp0o.hackhub.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipp0o.hackhub.application.*;
import io.github.filipp0o.hackhub.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AccettareInvitoTeamHttpTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UtenteRepository utenti;
    @Autowired private TeamRepository teams;
    @Autowired private InvitoRepository inviti;
    @Autowired private PartecipazioneRepository partecipazioni;

    @Test
    void sessioneAssenteRichiedeAccesso() throws Exception {
        mvc.perform(get("/api/inviti/ricevuti"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/inviti/accettazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invitoId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elencoIsolaSessioniEdEsponeSoloDatiDellInvito() throws Exception {
        Scenario s = prepara();
        MockHttpSession altraSessione = accedi(registra());

        mvc.perform(get("/api/inviti/ricevuti").session(altraSessione))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messaggio")
                        .value("Nessun invito disponibile"))
                .andExpect(jsonPath("$.invitiRicevuti").isEmpty());

        var risultato = mvc.perform(
                        get("/api/inviti/ricevuti").session(s.sessione()))
                .andExpect(status().isOk())
                .andReturn();

        var lista = mapper.readTree(
                risultato.getResponse().getContentAsString()
        ).get("invitiRicevuti");

        assertEquals(1, lista.size());

        var elemento = lista.get(0);
        assertEquals(3, elemento.size());
        assertEquals(
                s.invito().getId().longValue(),
                elemento.get("id").asLong()
        );
        assertEquals(
                s.team().getId().longValue(),
                elemento.get("teamId").asLong()
        );
        assertEquals(
                s.team().getNome(),
                elemento.get("nomeTeam").asText()
        );

        invariato(s);
    }

    @Test
    void accettazioneCompletaRimuoveInvitoENonDuplicaMembro() throws Exception {
        Scenario s = prepara();

        accetta(s.sessione(), s.invito().getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messaggio")
                        .value("Accettazione completata"));

        assertTrue(s.invito().isAccettato());
        assertEquals(
                s.team().getId(),
                teams.recuperaTeam(s.destinatario()).getId()
        );
        assertTrue(
                inviti.recuperaInvitiRicevuti(s.destinatario()).isEmpty()
        );

        mvc.perform(get("/api/inviti/ricevuti").session(s.sessione()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitiRicevuti").isEmpty());

        accetta(s.sessione(), s.invito().getId())
                .andExpect(status().isNotFound());

        assertEquals(2, s.team().numeroMembri());
    }

    @Test
    void sessioneAltruiEIdSconosciutoNonConsentonoAccettazione() throws Exception {
        Scenario s = prepara();

        accetta(accedi(registra()), s.invito().getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messaggio")
                        .value("Invito non disponibile"));

        accetta(s.sessione(), Long.MAX_VALUE)
                .andExpect(status().isNotFound());

        invariato(s);
    }

    @Test
    void appartenenzaAdAltroTeamRestituisceConflitto() throws Exception {
        Scenario s = prepara();
        Team altro = creaTeam(s.destinatario());

        accetta(s.sessione(), s.invito().getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messaggio")
                        .value("L'utente appartiene già a un team"));

        assertEquals(
                altro.getId(),
                teams.recuperaTeam(s.destinatario()).getId()
        );

        invariato(s);
    }

    @Test
    void capienzaSuperataNonAccettaInvitoENonAggiungeMembro() throws Exception {
        Scenario s = prepara();
        LocalDate oggi = LocalDate.now();

        Hackathon hackathon = Hackathon.crea(
                new DatiHackathon(
                        "Hackathon di prova",
                        "Regolamento",
                        "Criteri",
                        oggi.plusDays(1),
                        oggi.plusDays(2),
                        oggi.plusDays(3),
                        "Camerino",
                        BigDecimal.TEN,
                        1
                ),
                new Utente(10001L),
                new Utente(10002L),
                List.of(new Utente(10003L))
        );

        partecipazioni.salva(
                Partecipazione.crea(hackathon, s.team())
        );

        accetta(s.sessione(), s.invito().getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messaggio").value(
                        "L'ingresso supera la dimensione massima consentita per il team"
                ));

        assertFalse(
                teams.verificaAppartenenzaTeam(s.destinatario())
        );

        invariato(s);
    }

    @Test
    void selezioneNonValidaOCorpoMalformatoNonModificanoDati() throws Exception {
        Scenario s = prepara();

        for (Long id : new Long[]{null, 0L, -1L}) {
            accetta(s.sessione(), id)
                    .andExpect(status().isBadRequest());
        }

        for (String corpo : new String[]{
                "", "{", "null", "{}", "{\"invitoId\":\"abc\"}"
        }) {
            mvc.perform(post("/api/inviti/accettazione")
                            .session(s.sessione())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isBadRequest());
        }

        invariato(s);
    }

    private Scenario prepara() throws Exception {
        Utente creatore = registra();
        Utente destinatario = registra();
        Team team = creaTeam(creatore);

        mvc.perform(post("/api/inviti")
                        .session(accedi(creatore))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new InvitareUtentiTeamBoundary.RichiestaInvito(
                                        destinatario.getId()
                                )
                        )))
                .andExpect(status().isCreated());

        var ricevuti = inviti.recuperaInvitiRicevuti(destinatario);
        assertEquals(1, ricevuti.size());

        return new Scenario(
                destinatario,
                team,
                ricevuti.get(0),
                accedi(destinatario)
        );
    }

    private void invariato(Scenario s) {
        assertFalse(s.invito().isAccettato());
        assertEquals(1, s.team().numeroMembri());

        var ricevuti = inviti.recuperaInvitiRicevuti(s.destinatario());

        assertEquals(1, ricevuti.size());
        assertEquals(s.invito().getId(), ricevuti.get(0).getId());
    }

    private Utente registra() throws Exception {
        String email = UUID.randomUUID() + "@example.com";

        mvc.perform(post("/api/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new RegistrarsiBoundary.RichiestaRegistrazione(
                                        email, "password-valida"
                                )
                        )))
                .andExpect(status().isCreated());

        return utenti.recuperaPerEmail(email);
    }

    private MockHttpSession accedi(Utente utente) throws Exception {
        var risultato = mvc.perform(post("/api/accesso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new EffettuareAccessoBoundary.RichiestaAccesso(
                                        utente.recuperaEmail(),
                                        "password-valida"
                                )
                        )))
                .andExpect(status().isOk())
                .andReturn();

        var sessione = (MockHttpSession) risultato
                .getRequest().getSession(false);

        assertNotNull(sessione);
        return sessione;
    }

    private Team creaTeam(Utente creatore) {
        Team team = Team.crea(
                "Team " + UUID.randomUUID(), creatore, creatore
        );
        teams.salva(team);
        return team;
    }

    private ResultActions accetta(
            MockHttpSession sessione, Long id
    ) throws Exception {
        return mvc.perform(post("/api/inviti/accettazione")
                .session(sessione)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                        new AccettareInvitoTeamBoundary.RichiestaAccettazione(id)
                )));
    }

    private record Scenario(
            Utente destinatario,
            Team team,
            Invito invito,
            MockHttpSession sessione
    ) { }
}