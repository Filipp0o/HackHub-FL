package io.github.filipp0o.hackhub.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipp0o.hackhub.application.UtenteRepository;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EffettuareAccessoHttpTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UtenteRepository repository;
    @Autowired private SessioneUtente sessioneUtente;

    @Test
    void selezioneAccessoNonCreaSessione() throws Exception {
        var risultato = mockMvc.perform(get("/api/accesso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messaggio").value("Inserire email e password"))
                .andReturn();
        assertNull(risultato.getRequest().getSession(false));
    }

    @Test
    void registrazioneEAccessoRealiMemorizzanoUtenteSenzaEsporreCredenziali()
            throws Exception {
        String email = registraUtente();
        int prima = repository.recuperaUtentiAssegnabili().size();
        var risultato = accedi(email, "password-valida", null)
                .andExpect(status().isOk())
                .andExpect(content().json("{\"messaggio\":\"Accesso effettuato\"}", true))
                .andReturn();
        var sessione = (MockHttpSession) risultato.getRequest().getSession(false);
        assertNotNull(sessione);
        assertEquals(repository.recuperaPerEmail(email).getId(),
                utenteNellaSessione(sessione).getId());
        assertEquals(prima, repository.recuperaUtentiAssegnabili().size());
    }

    @Test
    void accessoRinnovaIdSessioneConservandoAttributi() throws Exception {
        String email = registraUtente();
        var sessione = new MockHttpSession();
        sessione.setAttribute("preferenza", "italiano");
        String id = sessione.getId();

        accedi(email, "password-valida", sessione).andExpect(status().isOk());

        assertNotEquals(id, sessione.getId());
        assertEquals("italiano", sessione.getAttribute("preferenza"));
        assertEquals(email, utenteNellaSessione(sessione).recuperaEmail());
    }

    @Test
    void passwordErrataEUtenteAssenteHannoLoStessoEsitoSenzaSessione()
            throws Exception {
        String email = registraUtente();
        for (String candidato : new String[]{email, nuovaEmail()}) {
            var risultato = accedi(candidato, "errata", null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json(
                            "{\"messaggio\":\"Credenziali non valide\"}", true))
                    .andReturn();
            assertNull(risultato.getRequest().getSession(false));
        }
    }

    @Test
    void credenzialiVuoteEPasswordOltreLimiteSonoRifiutate() throws Exception {
        String email = registraUtente();
        for (String valore : new String[]{null, "", " "}) {
            accedi(valore, "password-valida", null)
                    .andExpect(status().isUnauthorized());
            accedi(email, valore, null).andExpect(status().isUnauthorized());
        }
        accedi(email, "è".repeat(37), null).andExpect(status().isUnauthorized());
    }

    @Test
    void corpoAssenteOJsonMalformatoRestituisconoBadRequest() throws Exception {
        for (String corpo : new String[]{"", "{", "null"}) {
            mockMvc.perform(post("/api/accesso")
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void sessioniDistinteMantengonoUtentiDistinti() throws Exception {
        String emailA = registraUtente();
        String emailB = registraUtente();
        var sessioneA = new MockHttpSession();
        var sessioneB = new MockHttpSession();
        accedi(emailA, "password-valida", sessioneA).andExpect(status().isOk());
        accedi(emailB, "password-valida", sessioneB).andExpect(status().isOk());
        mockMvc.perform(get("/api/accesso").session(sessioneA))
                .andExpect(status().isOk());

        assertEquals(emailA, utenteNellaSessione(sessioneA).recuperaEmail());
        assertEquals(emailB, utenteNellaSessione(sessioneB).recuperaEmail());
    }

    @Test
    void soloUnNuovoAccessoRiuscitoSostituisceUtenteERinnovaId() throws Exception {
        String emailA = registraUtente();
        String emailB = registraUtente();
        var sessione = new MockHttpSession();
        accedi(emailA, "password-valida", sessione).andExpect(status().isOk());
        String id = sessione.getId();

        accedi(emailB, "errata", sessione).andExpect(status().isUnauthorized());
        assertEquals(id, sessione.getId());
        assertEquals(emailA, utenteNellaSessione(sessione).recuperaEmail());

        accedi(emailB, "password-valida", sessione).andExpect(status().isOk());
        assertNotEquals(id, sessione.getId());
        assertEquals(emailB, utenteNellaSessione(sessione).recuperaEmail());
    }

    private String registraUtente() throws Exception {
        String email = nuovaEmail();
        mockMvc.perform(post("/api/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrarsiBoundary.RichiestaRegistrazione(
                                        email, "password-valida"))))
                .andExpect(status().isCreated());
        return email;
    }

    private ResultActions accedi(String email, String password, MockHttpSession sessione)
            throws Exception {
        var richiesta = post("/api/accesso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new EffettuareAccessoBoundary.RichiestaAccesso(email, password)));
        if (sessione != null) {
            richiesta.session(sessione);
        }
        return mockMvc.perform(richiesta);
    }

    private Utente utenteNellaSessione(MockHttpSession sessione) {
        var richiesta = new MockHttpServletRequest(sessione.getServletContext());
        richiesta.setSession(sessione);
        var attributi = new ServletRequestAttributes(richiesta);
        var precedenti = RequestContextHolder.getRequestAttributes();
        RequestContextHolder.setRequestAttributes(attributi);
        try {
            return sessioneUtente.recupera();
        } finally {
            try {
                attributi.requestCompleted();
            } finally {
                RequestContextHolder.setRequestAttributes(precedenti);
            }
        }
    }

    private String nuovaEmail() {
        return "accesso-" + UUID.randomUUID() + "@example.com";
    }
}
