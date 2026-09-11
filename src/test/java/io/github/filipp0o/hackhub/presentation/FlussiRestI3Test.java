package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.*;
import io.github.filipp0o.hackhub.domain.*;
import io.github.filipp0o.hackhub.infrastructure.SistemaPagamentoAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class FlussiRestI3Test {

    final Utente organizzatore = new Utente(1L);
    final Utente giudice = new Utente(2L);
    final Utente mentore = new Utente(3L);
    final Utente responsabile = new Utente(4L);

    final SessioneUtente sessione = new SessioneUtente();

    final HackathonRepository hr = mock(HackathonRepository.class);
    final PartecipazioneRepository pr = mock(PartecipazioneRepository.class);
    final TeamRepository tr = mock(TeamRepository.class);
    final SottomissioneRepository sr = mock(SottomissioneRepository.class);
    final SegnalazioneRepository sgr = mock(SegnalazioneRepository.class);

    final SistemaPagamentoGateway gateway =
            spy(new SistemaPagamentoAdapter());

    Hackathon h;
    Partecipazione p;
    MockMvc mvc;

    @BeforeEach
    void prepara() {
        LocalDate oggi = LocalDate.now();

        h = Hackathon.crea(
                new DatiHackathon(
                        "H", "R", "C",
                        oggi.minusDays(5),
                        oggi.minusDays(3),
                        oggi.plusDays(2),
                        "Roma", BigDecimal.TEN, 5
                ),
                organizzatore,
                giudice,
                List.of(mentore)
        );

        h.assegnaId(10L);
        h.aggiornaStato(oggi);

        Team team = Team.crea("Team", responsabile, responsabile);

        p = new Partecipazione(h, team);
        p.assegnaId(20L);

        when(hr.recuperaHackathon(10L)).thenReturn(h);
        when(tr.recuperaTeam(responsabile)).thenReturn(team);
        when(pr.recuperaPartecipazione(team, h)).thenReturn(p);
        when(pr.ottieniPartecipazioni(h)).thenReturn(List.of(p));
        when(pr.recuperaPartecipazioniNonEscluse(h)).thenReturn(List.of(p));
        when(sr.recuperaSottomissione(p))
                .thenAnswer(invocazione -> p.getSottomissione());

        mvc = standaloneSetup(
                new InviareSottomissioneBoundary(
                        new InviareSottomissioneControl(tr, pr, sr),
                        sessione,
                        hr
                ),
                new AggiornareSottomissioneBoundary(
                        new AggiornareSottomissioneControl(tr, pr, sr),
                        sessione,
                        hr
                ),
                new ProclamareTeamVincitoreBoundary(
                        new ProclamareTeamVincitoreControl(pr, hr, sgr),
                        sessione,
                        hr,
                        pr
                ),
                new EsaminareSegnalazioneBoundary(
                        new EsaminareSegnalazioneControl(sgr, pr),
                        sessione
                ),
                new ErogarePremioBoundary(
                        new ErogarePremioControl(gateway, hr),
                        sessione,
                        hr
                ),
                new ConfigurareRiscossionePremioBoundary(
                        new ConfigurareRiscossionePremioControl(gateway, hr),
                        sessione,
                        hr
                )
        ).setControllerAdvice(new ErroriRichiestaHandler()).build();
    }

    @Test
    void invioCorrezioneAggiornamentoEConflitto() throws Exception {
        sessione.registra(responsabile);
        String uri = "/api/hackathons/10/sottomissione";

        mvc.perform(get(uri + "/invio"))
                .andExpect(status().isOk());

        mvc.perform(post(uri)
                        .contentType("application/json")
                        .content("{\"contenuto\":\" \"}"))
                .andExpect(status().isBadRequest());

        assertNull(p.getSottomissione());

        mvc.perform(post(uri)
                        .contentType("application/json")
                        .content("{\"contenuto\":\"prima\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get(uri))
                .andExpect(jsonPath("$.contenuto").value("prima"));

        mvc.perform(put(uri)
                        .contentType("application/json")
                        .content("{\"contenuto\":\"dopo\"}"))
                .andExpect(status().isNoContent());

        assertEquals("dopo", p.getSottomissione().getContenuto());

        mvc.perform(post(uri)
                        .contentType("application/json")
                        .content("{\"contenuto\":\"duplicata\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void erroriSalvataggioSottomissioneSono500ERipristinano() throws Exception {
        sessione.registra(responsabile);

        doThrow(new IllegalArgumentException("database"))
                .when(sr).salva(any());

        String uri = "/api/hackathons/10/sottomissione";

        mvc.perform(post(uri)
                        .contentType("application/json")
                        .content("{\"contenuto\":\"prima\"}"))
                .andExpect(status().isInternalServerError());

        assertNull(p.getSottomissione());

        Sottomissione.crea(p, "originale");

        mvc.perform(put(uri)
                        .contentType("application/json")
                        .content("{\"contenuto\":\"nuova\"}"))
                .andExpect(status().isInternalServerError());

        assertEquals("originale", p.getSottomissione().getContenuto());
    }

    @Test
    void sessioneRichiestaPerTuttiISeiCasi() throws Exception {
        for (String uri : List.of(
                "/sottomissione/invio",
                "/sottomissione",
                "/proclamazione/team-ammissibili",
                "/premio/erogazione"
        )) {
            mvc.perform(get("/api/hackathons/10" + uri))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(get("/api/segnalazioni/da-esaminare"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/hackathons/10/premio/configurazione"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verificaDecisioneNonRegistraEAperturaNotificaRegistraLettura()
            throws Exception {
        sessione.registra(organizzatore);

        Segnalazione s = Segnalazione.crea(mentore, p, "Violazione");
        s.assegnaId(30L);

        NotificaSegnalazione n = NotificaSegnalazione.crea(s, organizzatore);
        n.assegnaId(40L);

        when(sgr.ottieniSegnalazioniDaEsaminare(organizzatore))
                .thenReturn(List.of(s));
        when(sgr.ottieniNotificheRicevute(organizzatore))
                .thenReturn(List.of(n));

        mvc.perform(get("/api/segnalazioni/notifiche"))
                .andExpect(status().isOk());

        assertFalse(n.getLetta());

        mvc.perform(post("/api/segnalazioni/notifiche/40/apertura"))
                .andExpect(status().isOk());

        assertTrue(n.getLetta());

        String uri = "/api/segnalazioni/30/decisione";

        mvc.perform(post(uri + "/verifica")
                        .contentType("application/json")
                        .content("{\"motivazione\":\"Motivo\"}"))
                .andExpect(status().isBadRequest());

        String dati = """
                {
                  "esito": "VIOLAZIONE_CON_ESCLUSIONE",
                  "motivazione": "Motivo"
                }
                """;

        mvc.perform(post(uri + "/verifica")
                        .contentType("application/json")
                        .content(dati))
                .andExpect(status().isOk());

        assertEquals(StatoSegnalazione.DA_ESAMINARE, s.getStato());
        verify(sgr, never()).salva(any());

        mvc.perform(post(uri)
                        .contentType("application/json")
                        .content(dati))
                .andExpect(status().isNoContent());

        assertEquals(StatoPartecipazione.ESCLUSA, p.getStato());
    }

    @Test
    void proclamazioneRiepilogoConflittoERipristino() throws Exception {
        LocalDate oggi = LocalDate.now();

        h = Hackathon.crea(
                new DatiHackathon(
                        "H", "R", "C",
                        oggi.minusDays(10),
                        oggi.minusDays(5),
                        oggi.minusDays(1),
                        "Roma", BigDecimal.TEN, 5
                ),
                organizzatore,
                giudice,
                List.of(mentore)
        );

        h.assegnaId(10L);
        h.aggiornaStato(oggi);

        p = new Partecipazione(
                h,
                Team.crea("Team", responsabile, responsabile)
        );
        p.assegnaId(20L);

        Sottomissione s = Sottomissione.crea(p, "Consegna");

        Valutazione.crea(
                s,
                giudice,
                new DatiValutazione("Buona", BigDecimal.TEN)
        );

        when(hr.recuperaHackathon(10L)).thenReturn(h);
        when(pr.ottieniPartecipazioni(h)).thenReturn(List.of(p));
        when(pr.recuperaPartecipazioniNonEscluse(h)).thenReturn(List.of(p));

        sessione.registra(organizzatore);

        String uri = "/api/hackathons/10/proclamazione";

        mvc.perform(get(uri + "/team-ammissibili"))
                .andExpect(status().isOk());

        mvc.perform(get(uri + "/riepilogo")
                        .param("partecipazioneId", "20"))
                .andExpect(status().isOk());

        assertNull(h.getVincitrice());

        Segnalazione segnalazione =
                Segnalazione.crea(mentore, p, "Da esaminare");

        when(sgr.ottieniSegnalazioniDaEsaminare(organizzatore))
                .thenReturn(List.of(segnalazione));

        mvc.perform(post(uri)
                        .contentType("application/json")
                        .content("{\"partecipazioneId\":20}"))
                .andExpect(status().isConflict());

        when(sgr.ottieniSegnalazioniDaEsaminare(organizzatore))
                .thenReturn(List.of());

        doThrow(new IllegalStateException("database"))
                .when(hr).salva(h);

        mvc.perform(post(uri)
                        .contentType("application/json")
                        .content("{\"partecipazioneId\":20}"))
                .andExpect(status().isInternalServerError());

        assertNull(h.getVincitrice());
        assertNull(h.getRiscossionePremio());

        doNothing().when(hr).salva(h);

        mvc.perform(post(uri)
                        .contentType("application/json")
                        .content("{\"partecipazioneId\":20}"))
                .andExpect(status().isNoContent());

        assertNotNull(h.getVincitrice());
    }

    @Test
    void configurazioneErogazioneERipetizione() throws Exception {
        concluso();
        sessione.registra(responsabile);

        mvc.perform(post("/api/hackathons/10/premio/configurazione"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurata").value(true));

        sessione.registra(organizzatore);

        mvc.perform(get("/api/hackathons/10/premio/erogazione"))
                .andExpect(status().isOk());

        verify(gateway, never())
                .richiediErogazionePremio(any(), any(), any());

        mvc.perform(post("/api/hackathons/10/premio/erogazione"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/hackathons/10/premio/erogazione"))
                .andExpect(status().isConflict());
    }

    @Test
    void configurazioneAnnullataENegativa() throws Exception {
        concluso();
        sessione.registra(responsabile);

        doThrow(new SistemaPagamentoGateway.ConfigurazioneAnnullataException())
                .when(gateway).avviaConfigurazioneBeneficiario(any());

        mvc.perform(post("/api/hackathons/10/premio/configurazione"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurata").value(false));

        assertEquals(
                StatoRiscossionePremio.DA_CONFIGURARE,
                h.getRiscossionePremio().getStato()
        );

        doThrow(new IllegalStateException("Verifica fallita"))
                .when(gateway).avviaConfigurazioneBeneficiario(any());

        mvc.perform(post("/api/hackathons/10/premio/configurazione"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void risorseMancantiEAccessoNonAutorizzato() throws Exception {
        sessione.registra(organizzatore);

        when(hr.recuperaHackathon(99L))
                .thenThrow(new IllegalStateException("Hackathon non trovato"));

        mvc.perform(get("/api/hackathons/99/premio/erogazione"))
                .andExpect(status().isNotFound());

        sessione.registra(responsabile);

        mvc.perform(get("/api/hackathons/10/proclamazione/team-ammissibili"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/hackathons/10/premio/erogazione"))
                .andExpect(status().isForbidden());
    }

    private void concluso() {
        h.aggiornaStato(h.getDataFine().plusDays(1));
        h.registraPartecipazioneVincitrice(p);
        h.concludi();
        RiscossionePremio.crea(h);
    }
}