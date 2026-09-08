package io.github.filipp0o.hackhub.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipp0o.hackhub.application.*;
import io.github.filipp0o.hackhub.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AutenticazioneOperazioniHttpTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UtenteRepository utenti;
    @Autowired private TeamRepository teams;
    @Autowired private HackathonRepository hackathons;
    @Autowired private PartecipazioneRepository partecipazioni;
    @Autowired private SottomissioneRepository sottomissioni;
    @Autowired private SegnalazioneRepository segnalazioni;

    @ParameterizedTest
    @CsvSource({
            "POST, /api/teams",
            "POST, /api/hackathons",
            "GET, /api/hackathons/utenti-assegnabili",
            "GET, /api/iscrizioni/hackathons",
            "POST, /api/iscrizioni/hackathons/1",
            "GET, /api/segnalazioni/hackathons?mentoreId=1",
            "GET, /api/segnalazioni/hackathons/1/partecipazioni?mentoreId=1",
            "POST, /api/segnalazioni/hackathons/1/partecipazioni/1",
            "GET, /api/valutazioni/hackathons?giudiceId=1",
            "GET, /api/valutazioni/hackathons/1/sottomissioni?giudiceId=1",
            "POST, /api/valutazioni/hackathons/1/sottomissioni/1",
            "GET, /api/segnalazioni/da-esaminare?organizzatoreId=1",
            "GET, /api/segnalazioni/1?organizzatoreId=1",
            "POST, /api/segnalazioni/1/decisione"
    })
    void richiedeAccessoSuTutteLeOperazioniProtette(
            String metodo,
            String percorso
    ) throws Exception {
        var richiesta = metodo.equals("GET")
                ? get(percorso)
                : post(percorso)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "utenteId": 1,
                                  "organizzatoreId": 1,
                                  "mentoreId": 1,
                                  "giudiceId": 1
                                }
                                """);

        mockMvc.perform(richiesta)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messaggio")
                        .value("Accesso richiesto"));
    }

    @Test
    void mantieneConsultazionePubblicaRegistrazioneEAccesso()
            throws Exception {
        for (String percorso : List.of(
                "/api/hackathons",
                "/api/accesso",
                "/api/registrazione"
        )) {
            mockMvc.perform(get(percorso))
                    .andExpect(status().isOk());
        }

        assertNotNull(registraEAccedi().utente().getId());
    }

    @Test
    void creazioneTeamUsaLaSessioneEIsolaDueUtenti()
            throws Exception {
        Account primo = registraEAccedi();
        Account secondo = registraEAccedi();

        invia("/api/teams", primo, Map.of(
                "nome", "Team-" + UUID.randomUUID(),
                "utenteId", secondo.utente().getId()
        )).andExpect(status().isCreated());

        assertTrue(teams.verificaAppartenenzaTeam(primo.utente()));
        assertFalse(teams.verificaAppartenenzaTeam(secondo.utente()));

        invia("/api/teams", secondo, Map.of(
                "nome", "Team-" + UUID.randomUUID()
        )).andExpect(status().isCreated());

        assertEquals(
                primo.utente().getId(),
                teams.recuperaTeam(primo.utente())
                        .getResponsabile().getId()
        );

        assertEquals(
                secondo.utente().getId(),
                teams.recuperaTeam(secondo.utente())
                        .getResponsabile().getId()
        );

        assertNotEquals(
                teams.recuperaTeam(primo.utente()).getId(),
                teams.recuperaTeam(secondo.utente()).getId()
        );
    }

    @Test
    void creaHackathonConOrganizzatoreDiSessioneEAccountStaffEsistenti()
            throws Exception {
        Account organizzatore = registraEAccedi();
        Account giudice = registraEAccedi();
        Account mentore = registraEAccedi();

        var dati = datiCreazione(
                giudice.utente().getId(),
                List.of(mentore.utente().getId())
        );
        dati.put("organizzatoreId", giudice.utente().getId());

        invia("/api/hackathons", organizzatore, dati)
                .andExpect(status().isCreated());

        Hackathon evento = hackathons.ottieniTuttiHackathon().stream()
                .filter(h -> h.getNome().equals(dati.get("nome")))
                .findFirst()
                .orElseThrow();

        assertSame(organizzatore.utente(), evento.getOrganizzatore());
        assertSame(giudice.utente(), evento.getGiudice());
        assertEquals(List.of(mentore.utente()), evento.getMentori());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MAX_VALUE})
    void rifiutaIdStaffInvalidiOSconosciutiSenzaCreareHackathon(long id)
            throws Exception {
        Account organizzatore = registraEAccedi();
        Account staff = registraEAccedi();
        int prima = hackathons.ottieniTuttiHackathon().size();

        invia("/api/hackathons", organizzatore, datiCreazione(
                id,
                List.of(staff.utente().getId())
        )).andExpect(status().isBadRequest());

        invia("/api/hackathons", organizzatore, datiCreazione(
                staff.utente().getId(),
                List.of(id)
        )).andExpect(status().isBadRequest());

        assertEquals(prima, hackathons.ottieniTuttiHackathon().size());
    }

    @Test
    void rifiutaStaffMancanteSenzaCreareHackathon() throws Exception {
        Account organizzatore = registraEAccedi();
        Account staff = registraEAccedi();
        int prima = hackathons.ottieniTuttiHackathon().size();

        invia("/api/hackathons", organizzatore, datiCreazione(
                null,
                List.of(staff.utente().getId())
        )).andExpect(status().isBadRequest());

        invia("/api/hackathons", organizzatore, datiCreazione(
                staff.utente().getId(),
                List.of()
        )).andExpect(status().isBadRequest());

        invia("/api/hackathons", organizzatore, datiCreazione(
                staff.utente().getId(),
                null
        )).andExpect(status().isBadRequest());

        assertEquals(prima, hackathons.ottieniTuttiHackathon().size());
    }

    @Test
    void elencoStaffEsponeSoltantoIdEdEmail() throws Exception {
        Account account = registraEAccedi();

        var risposta = mockMvc.perform(
                        get("/api/hackathons/utenti-assegnabili")
                                .session(account.sessione())
                )
                .andExpect(status().isOk())
                .andReturn();

        var elenco = objectMapper.readTree(
                risposta.getResponse().getContentAsString()
        );

        assertTrue(elenco.isArray());
        assertFalse(elenco.isEmpty());

        boolean trovato = false;
        for (var utente : elenco) {
            assertEquals(2, utente.size());
            assertTrue(utente.has("id"));
            assertTrue(utente.has("email"));
            trovato |= utente.get("id").asLong()
                    == account.utente().getId();
        }

        assertTrue(trovato);
    }

    @Test
    void iscriveIlProprioTeamAncheSeIlBodyIndicaUnAltroUtente()
            throws Exception {
        Account membro = registraEAccedi();
        Account altro = registraEAccedi();

        Team proprio = creaTeam(membro);
        Team altrui = creaTeam(altro);

        Hackathon evento = creaEvento(
                membro, altro, altro,
                5, 8, 10
        );

        invia(
                "/api/iscrizioni/hackathons/" + evento.getId(),
                membro,
                Map.of("utenteId", altro.utente().getId())
        ).andExpect(status().isCreated());

        assertTrue(partecipazioni.esistePartecipazione(proprio, evento));
        assertFalse(partecipazioni.esistePartecipazione(altrui, evento));
    }

    @Test
    void mentoreEstraneoNonLeggeENonSegnalaImpersonandoQuelloAssegnato()
            throws Exception {
        Account organizzatore = registraEAccedi();
        Account mentore = registraEAccedi();
        Account estraneo = registraEAccedi();

        Hackathon evento = creaEvento(
                organizzatore, organizzatore, mentore,
                -5, -2, 2
        );

        Partecipazione partecipazione = creaPartecipazione(
                evento,
                creaTeam(estraneo)
        );

        String base = "/api/segnalazioni/hackathons/"
                + evento.getId() + "/partecipazioni";

        mockMvc.perform(get("/api/segnalazioni/hackathons")
                        .param("mentoreId", mentore.utente().getId().toString())
                        .session(estraneo.sessione()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(get(base)
                        .param("mentoreId", mentore.utente().getId().toString())
                        .session(estraneo.sessione()))
                .andExpect(status().isNotFound());

        invia(base + "/" + partecipazione.getId(), estraneo, Map.of(
                "mentoreId", mentore.utente().getId(),
                "descrizione", "Violazione"
        )).andExpect(status().isNotFound());

        assertTrue(
                segnalazioni.ottieniSegnalazioniDaEsaminare(
                        organizzatore.utente()
                ).isEmpty()
        );

        invia(base + "/" + partecipazione.getId(), mentore, Map.of(
                "mentoreId", estraneo.utente().getId(),
                "descrizione", "Violazione"
        )).andExpect(status().isCreated());

        assertEquals(
                mentore.utente().getId(),
                segnalazioni.ottieniSegnalazioniDaEsaminare(
                        organizzatore.utente()
                ).getFirst().getMentoreSegnalante().getId()
        );
    }

    @Test
    void giudiceEstraneoNonLeggeENonValutaImpersonandoQuelloAssegnato()
            throws Exception {
        Account organizzatore = registraEAccedi();
        Account giudice = registraEAccedi();
        Account estraneo = registraEAccedi();

        Hackathon evento = creaEvento(
                organizzatore, giudice, organizzatore,
                -8, -5, -2
        );

        Partecipazione partecipazione = creaPartecipazione(
                evento,
                creaTeam(estraneo)
        );

        Sottomissione sottomissione = new Sottomissione(
                partecipazione,
                "Progetto"
        );
        sottomissioni.salva(sottomissione);

        String base = "/api/valutazioni/hackathons/"
                + evento.getId() + "/sottomissioni";

        mockMvc.perform(get("/api/valutazioni/hackathons")
                        .param("giudiceId", giudice.utente().getId().toString())
                        .session(estraneo.sessione()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(get(base)
                        .param("giudiceId", giudice.utente().getId().toString())
                        .session(estraneo.sessione()))
                .andExpect(status().isNotFound());

        invia(base + "/" + sottomissione.getId(), estraneo, Map.of(
                "giudiceId", giudice.utente().getId(),
                "giudizio", "Ottimo",
                "punteggio", 9
        )).andExpect(status().isNotFound());

        assertNull(sottomissione.getValutazione());

        invia(base + "/" + sottomissione.getId(), giudice, Map.of(
                "giudiceId", estraneo.utente().getId(),
                "giudizio", "Ottimo",
                "punteggio", 9
        )).andExpect(status().isCreated());

        assertEquals(
                giudice.utente().getId(),
                sottomissione.getValutazione().getGiudice().getId()
        );
    }

    @Test
    void organizzatoreEstraneoNonLeggeENonDecideImpersonandoQuelloAssegnato()
            throws Exception {
        Account organizzatore = registraEAccedi();
        Account mentore = registraEAccedi();
        Account estraneo = registraEAccedi();

        Hackathon evento = creaEvento(
                organizzatore, organizzatore, mentore,
                -5, -2, 2
        );

        Partecipazione partecipazione = creaPartecipazione(
                evento,
                creaTeam(estraneo)
        );

        Segnalazione segnalazione = Segnalazione.crea(
                mentore.utente(),
                partecipazione,
                "Violazione"
        );
        segnalazioni.salva(segnalazione);

        String base = "/api/segnalazioni/" + segnalazione.getId();

        mockMvc.perform(get("/api/segnalazioni/da-esaminare")
                        .param(
                                "organizzatoreId",
                                organizzatore.utente().getId().toString()
                        )
                        .session(estraneo.sessione()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(get(base)
                        .param(
                                "organizzatoreId",
                                organizzatore.utente().getId().toString()
                        )
                        .session(estraneo.sessione()))
                .andExpect(status().isNotFound());

        invia(base + "/decisione", estraneo, Map.of(
                "organizzatoreId", organizzatore.utente().getId(),
                "esito", "VIOLAZIONE_CON_ESCLUSIONE",
                "motivazione", "Confermata"
        )).andExpect(status().isNotFound());

        assertEquals(
                StatoSegnalazione.DA_ESAMINARE,
                segnalazione.getStato()
        );
        assertEquals(
                StatoPartecipazione.ATTIVA,
                partecipazione.getStato()
        );

        invia(base + "/decisione", organizzatore, Map.of(
                "organizzatoreId", estraneo.utente().getId(),
                "esito", "ARCHIVIATA",
                "motivazione", "Non fondata"
        )).andExpect(status().isNoContent());

        assertEquals(
                organizzatore.utente().getId(),
                segnalazione.getEsaminatore().getId()
        );
    }

    private Account registraEAccedi() throws Exception {
        String email = "autenticazione-"
                + UUID.randomUUID() + "@example.com";

        var dati = Map.of(
                "email", email,
                "password", "password-valida"
        );

        mockMvc.perform(post("/api/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dati)))
                .andExpect(status().isCreated());

        MockHttpSession sessione = new MockHttpSession();

        mockMvc.perform(post("/api/accesso")
                        .session(sessione)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dati)))
                .andExpect(status().isOk());

        return new Account(utenti.recuperaPerEmail(email), sessione);
    }

    private ResultActions invia(
            String percorso,
            Account account,
            Object dati
    ) throws Exception {
        return mockMvc.perform(post(percorso)
                .session(account.sessione())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dati)));
    }

    private Map<String, Object> datiCreazione(
            Long giudice,
            List<Long> mentori
    ) {
        var dati = new LinkedHashMap<String, Object>();

        dati.put("nome", "Hackathon-" + UUID.randomUUID());
        dati.put("regolamento", "Regolamento");
        dati.put("criteriValutazione", "Criteri");
        dati.put("scadenzaIscrizioni", LocalDate.now().plusDays(5));
        dati.put("dataInizio", LocalDate.now().plusDays(8));
        dati.put("dataFine", LocalDate.now().plusDays(10));
        dati.put("luogo", "Camerino");
        dati.put("importoPremio", new BigDecimal("100.00"));
        dati.put("dimensioneMassimaTeam", 4);
        dati.put("giudiceId", giudice);
        dati.put("mentoriIds", mentori);

        return dati;
    }

    // Le fixture sono preparate nei repository.
    // Le operazioni sotto prova passano tramite HTTP.
    private Team creaTeam(Account account) {
        Team team = Team.crea(
                "Team-" + UUID.randomUUID(),
                account.utente(),
                account.utente()
        );
        teams.salva(team);
        return team;
    }

    private Hackathon creaEvento(
            Account organizzatore,
            Account giudice,
            Account mentore,
            int scadenza,
            int inizio,
            int fine
    ) {
        LocalDate oggi = LocalDate.now();

        Hackathon evento = Hackathon.crea(
                new DatiHackathon(
                        "Hackathon-" + UUID.randomUUID(),
                        "Regolamento",
                        "Criteri",
                        oggi.plusDays(scadenza),
                        oggi.plusDays(inizio),
                        oggi.plusDays(fine),
                        "Camerino",
                        new BigDecimal("100.00"),
                        4
                ),
                organizzatore.utente(),
                giudice.utente(),
                List.of(mentore.utente())
        );

        evento.aggiornaStato(oggi);
        hackathons.salva(evento);
        return evento;
    }

    private Partecipazione creaPartecipazione(
            Hackathon evento,
            Team team
    ) {
        Partecipazione partecipazione = new Partecipazione(
                evento,
                team
        );
        partecipazioni.salva(partecipazione);
        return partecipazione;
    }

    private record Account(
            Utente utente,
            MockHttpSession sessione
    ) {
    }
}