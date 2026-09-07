package io.github.filipp0o.hackhub.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipp0o.hackhub.application.CodificatorePassword;
import io.github.filipp0o.hackhub.application.UtenteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class RegistrarsiHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UtenteRepository repository;

    @Autowired
    private CodificatorePassword codificatore;

    @Test
    void getRichiedeDatiSenzaCreareAccount() throws Exception {
        int prima = repository.recuperaUtentiAssegnabili().size();

        mockMvc.perform(get("/api/registrazione"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.messaggio")
                        .value("Inserire email e password"));

        assertEquals(
                prima,
                repository.recuperaUtentiAssegnabili().size()
        );
    }

    @Test
    void postRegistraConBCryptSenzaEsporreCredenziali() throws Exception {
        String email = nuovaEmail();
        String password = "Password-di-prova-123!";

        invia(email, password)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messaggio")
                        .value("Registrazione completata"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        var utente = repository.recuperaUtentiAssegnabili().stream()
                .filter(u -> email.equals(u.recuperaEmail()))
                .findFirst()
                .orElseThrow();

        assertNotNull(utente.getId());
        assertNotEquals(password, utente.recuperaPasswordHash());

        assertTrue(codificatore.verifica(
                password,
                utente.recuperaPasswordHash()
        ));
        assertFalse(codificatore.verifica(
                "errata",
                utente.recuperaPasswordHash()
        ));
    }

    @Test
    void secondaRegistrazioneRestituisceConflictSenzaSovrascrivere()
            throws Exception {

        String email = nuovaEmail();

        invia(email, "prima-password")
                .andExpect(status().isCreated());

        int prima = repository.recuperaUtentiAssegnabili().size();

        var originale = repository.recuperaUtentiAssegnabili().stream()
                .filter(u -> email.equals(u.recuperaEmail()))
                .findFirst()
                .orElseThrow();

        String hashOriginale = originale.recuperaPasswordHash();

        invia(email, "seconda-password")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messaggio")
                        .value("L'email è già registrata"));

        assertEquals(
                prima,
                repository.recuperaUtentiAssegnabili().size()
        );

        var riletto = repository.recuperaUtentiAssegnabili().stream()
                .filter(u -> email.equals(u.recuperaEmail()))
                .findFirst()
                .orElseThrow();

        assertEquals(originale.getId(), riletto.getId());
        assertEquals(hashOriginale, riletto.recuperaPasswordHash());
    }

    @Test
    void campiAssentiOVuotiRestituisconoBadRequest() throws Exception {
        int prima = repository.recuperaUtentiAssegnabili().size();

        for (String valore : new String[]{null, "", " "}) {
            invia(valore, "password-valida")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messaggio")
                            .value("Dati di registrazione da correggere"));

            invia(nuovaEmail(), valore)
                    .andExpect(status().isBadRequest());
        }

        assertEquals(
                prima,
                repository.recuperaUtentiAssegnabili().size()
        );
    }

    @Test
    void passwordOltreLimiteBCryptNonCreaAccount() throws Exception {
        String email = nuovaEmail();

        // 37 caratteri, ma 74 byte in UTF-8:
        // oltre il limite BCrypt di 72 byte.
        invia(email, "è".repeat(37))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messaggio")
                        .value("Dati di registrazione da correggere"));

        assertFalse(repository.esistePerEmail(email));
    }

    @Test
    void jsonMalformatoOCorpoAssenteRestituisconoBadRequest()
            throws Exception {

        int prima = repository.recuperaUtentiAssegnabili().size();

        mockMvc.perform(post("/api/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/registrazione")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        assertEquals(
                prima,
                repository.recuperaUtentiAssegnabili().size()
        );
    }

    private ResultActions invia(String email, String password)
            throws Exception {

        String json = objectMapper.writeValueAsString(
                new RegistrarsiBoundary.RichiestaRegistrazione(
                        email,
                        password
                )
        );

        return mockMvc.perform(post("/api/registrazione")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String nuovaEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }
}