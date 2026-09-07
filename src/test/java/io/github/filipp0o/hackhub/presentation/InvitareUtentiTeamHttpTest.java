package io.github.filipp0o.hackhub.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipp0o.hackhub.application.InvitoRepository;
import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.application.UtenteRepository;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InvitareUtentiTeamHttpTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UtenteRepository utenti;
    @Autowired private TeamRepository teams;
    @Autowired private InvitoRepository inviti;

    @Test
    void sessioneAssenteRichiedeAccesso() throws Exception {
        mvc.perform(get("/api/inviti/utenti-invitabili")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/inviti").contentType(MediaType.APPLICATION_JSON)
                .content("{\"utenteInvitatoId\":1}")).andExpect(status().isUnauthorized());
    }

    @Test
    void elencoEsponeSoloIdEdEmailDiAltriAccount() throws Exception {
        Utente creatore = registra();
        Utente destinatario = registra();
        var risultato = mvc.perform(get("/api/inviti/utenti-invitabili").session(accedi(creatore)))
                .andExpect(status().isOk()).andReturn();
        var lista = mapper.readTree(risultato.getResponse().getContentAsString()).get("utentiInvitabili");
        boolean trovato = false;
        for (var elemento : lista) {
            assertEquals(2, elemento.size());
            assertTrue(elemento.has("id"));
            assertTrue(elemento.has("email"));
            assertNotEquals(creatore.getId().longValue(), elemento.get("id").asLong());
            trovato |= elemento.get("id").asLong() == destinatario.getId();
        }
        assertTrue(trovato);
    }

    @Test
    void invitoUsaTeamDellaSessioneEConservaComposizione() throws Exception {
        Utente creatore = registra();
        Utente destinatario = registra();
        Team team = creaTeam(creatore);

        invia(accedi(creatore), destinatario.getId()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.messaggio").value("Invito registrato"));

        var ricevuti = inviti.recuperaInvitiRicevuti(destinatario);
        assertEquals(1, ricevuti.size());
        assertSame(team, ricevuti.getFirst().ottieniTeam());
        assertFalse(ricevuti.getFirst().isAccettato());
        assertEquals(1, team.numeroMembri());
    }

    @Test
    void sessioniDistinteInvitanoDalProprioTeam() throws Exception {
        Utente primo = registra();
        Utente secondo = registra();
        Utente destinatario = registra();
        Team teamPrimo = creaTeam(primo);
        Team teamSecondo = creaTeam(secondo);
        invia(accedi(primo), destinatario.getId()).andExpect(status().isCreated());
        invia(accedi(secondo), destinatario.getId()).andExpect(status().isCreated());

        var ricevuti = inviti.recuperaInvitiRicevuti(destinatario);
        assertEquals(2, ricevuti.size());
        assertSame(teamPrimo, ricevuti.get(0).ottieniTeam());
        assertSame(teamSecondo, ricevuti.get(1).ottieniTeam());
    }

    @Test
    void utenteSenzaTeamOSempliceMembroRiceveConflitto() throws Exception {
        Utente creatore = registra();
        Utente membro = registra();
        Utente destinatario = registra();
        var sessione = accedi(membro);
        invia(sessione, destinatario.getId()).andExpect(status().isConflict());
        creaTeam(creatore).aggiungiMembro(membro);
        invia(sessione, destinatario.getId()).andExpect(status().isConflict());
        assertTrue(inviti.recuperaInvitiRicevuti(destinatario).isEmpty());
    }

    @Test
    void selezioniNonValideNonRegistranoInviti() throws Exception {
        Utente creatore = registra();
        creaTeam(creatore);
        var sessione = accedi(creatore);
        for (Long id : new Long[]{null, 0L, -1L, Long.MAX_VALUE, creatore.getId()}) {
            invia(sessione, id).andExpect(status().isBadRequest());
        }
        assertTrue(inviti.recuperaInvitiRicevuti(creatore).isEmpty());
    }

    @Test
    void corpoMalformatoOAssenteVieneRifiutato() throws Exception {
        var sessione = accedi(registra());
        for (String corpo : new String[]{"", "{", "null"}) {
            mvc.perform(post("/api/inviti").session(sessione)
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest());
        }
    }

    private Utente registra() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/registrazione").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new RegistrarsiBoundary.RichiestaRegistrazione(
                        email, "password-valida")))).andExpect(status().isCreated());
        return utenti.recuperaPerEmail(email);
    }

    private MockHttpSession accedi(Utente utente) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/accesso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new EffettuareAccessoBoundary.RichiestaAccesso(
                                utente.recuperaEmail(), "password-valida")))).andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private Team creaTeam(Utente creatore) {
        Team team = Team.crea("Team di prova", creatore, creatore);
        teams.salva(team);
        return team;
    }

    private ResultActions invia(MockHttpSession sessione, Long id) throws Exception {
        return mvc.perform(post("/api/inviti").session(sessione)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new InvitareUtentiTeamBoundary.RichiestaInvito(id))));
    }
}
