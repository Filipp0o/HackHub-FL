package io.github.filipp0o.hackhub.configuration;

import io.github.filipp0o.hackhub.HackHubApplication;
import io.github.filipp0o.hackhub.application.*;
import io.github.filipp0o.hackhub.application.AccettareInvitoTeamControl.AccettazioneInvitoFallitaException;
import io.github.filipp0o.hackhub.domain.*;
import io.github.filipp0o.hackhub.infrastructure.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.JdbcTransactionManager;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProfiliRepositoryTest {

    private ConfigurableApplicationContext avvia(String profilo, String url) {
        var argomenti = new ArrayList<>(List.of(
                "--spring.profiles.active=" + profilo,
                "--server.port=0",
                "--spring.main.banner-mode=off",
                "--logging.level.root=ERROR"
        ));
        if (url != null) argomenti.add("--spring.datasource.url=" + url);
        return new SpringApplicationBuilder(HackHubApplication.class)
                .web(WebApplicationType.SERVLET)
                .registerShutdownHook(false)
                .run(argomenti.toArray(String[]::new));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "in-memory"})
    void profiloPredefinitoEdEsplicitoUsanoRepositoryInMemoria(String profilo) {
        try (var context = avvia(profilo, null)) {
            verificaRepository(context, Map.of(
                    UtenteRepository.class, InMemoryUtenteRepository.class,
                    TeamRepository.class, InMemoryTeamRepository.class,
                    InvitoRepository.class, InMemoryInvitoRepository.class,
                    HackathonRepository.class, InMemoryHackathonRepository.class,
                    PartecipazioneRepository.class, InMemoryPartecipazioneRepository.class,
                    SottomissioneRepository.class, SottomissioneRepositoryImpl.class,
                    ValutazioneRepository.class, ValutazioneRepositoryImpl.class
            ));
            assertTrue(context.getBeansOfType(DataSource.class).isEmpty());
            Scenario scenario = preparaScenario(context);
            context.getBean(AccettareInvitoTeamControl.class)
                    .richiediAccettazioneInvito(scenario.destinatario(), scenario.invito());
            assertTrue(context.getBean(InvitoRepository.class)
                    .recuperaInvitiRicevuti(scenario.destinatario()).isEmpty());
            assertEquals(2, context.getBean(TeamRepository.class)
                    .recuperaTeam(scenario.destinatario()).numeroMembri());
        }
    }

    @Test
    void persistentUsaIlProxyUc15ERipristinaEntrambeLeScritture() {
        String url = "jdbc:h2:mem:profili-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        try (var context = avvia("persistent", url)) {
            verificaJdbc(context);
            Scenario scenario = preparaScenario(context);
            var control = context.getBean(AccettareInvitoTeamControl.class);
            assertTrue(AopUtils.isAopProxy(control));
            DataSource origine = context.getBean(DataSource.class);
            assertSame(origine, context.getBean(JdbcTransactionManager.class).getDataSource());
            JdbcClient jdbc = JdbcClient.create(origine);
            jdbc.sql("ALTER TABLE team_membro ADD CONSTRAINT prova_errore CHECK (utente_id <> "
                    + scenario.destinatario().getId() + ")").update();

            assertThrows(AccettazioneInvitoFallitaException.class,
                    () -> control.richiediAccettazioneInvito(scenario.destinatario(), scenario.invito()));

            var inviti = context.getBean(InvitoRepository.class);
            var teams = context.getBean(TeamRepository.class);
            assertFalse(jdbc.sql("SELECT accettato FROM invito WHERE id = :id")
                    .param("id", scenario.invito().getId()).query(Boolean.class).single());
            assertEquals(1, inviti.recuperaInvitiRicevuti(scenario.destinatario()).size());
            assertFalse(teams.verificaAppartenenzaTeam(scenario.destinatario()));
            assertEquals(1, teams.recuperaTeam(scenario.responsabile()).numeroMembri());

            jdbc.sql("ALTER TABLE team_membro DROP CONSTRAINT prova_errore").update();
            control.richiediAccettazioneInvito(scenario.destinatario(), scenario.invito());
            assertTrue(inviti.recuperaInvitiRicevuti(scenario.destinatario()).isEmpty());
            assertEquals(scenario.team().getId(), teams.recuperaTeam(scenario.destinatario()).getId());
            assertEquals(2, teams.recuperaTeam(scenario.responsabile()).numeroMembri());
        }
    }

    @Test
    void persistentConservaAccountEAssociazioniDopoIlRiavvio(@TempDir Path cartella) throws Exception {
        String url = "jdbc:h2:file:" + cartella.resolve("hackhub").toAbsolutePath();
        Scenario scenario;
        Long sottomissioneId;
        Long valutazioneId;
        String credenziali = """
                {"email":"visitatore@example.com","password":"Password1!"}
                """;
        try (var context = avvia("persistent", url)) {
            verificaJdbc(context);
            assertEquals(201, post(context, "/api/registrazione", credenziali));
            scenario = preparaScenario(context);
            Sottomissione s = Sottomissione.crea(scenario.partecipazione(), "Soluzione completa");
            context.getBean(SottomissioneRepository.class).salva(s);
            context.getBean(ValutareSottomissioneControl.class).confermaValutazione(
                    s, scenario.giudice(), new DatiValutazione("Ottimo lavoro", new BigDecimal("8.75"))
            );
            sottomissioneId = s.getId();
            valutazioneId = s.getValutazione().getId();
            assertNotNull(sottomissioneId);
            assertNotNull(valutazioneId);
            assertEquals(valutazioneId, context.getBean(SottomissioneRepository.class)
                    .recuperaSottomissione(scenario.partecipazione()).getValutazione().getId());
        }

        try (var context = avvia("persistent", url)) {
            verificaJdbc(context);
            assertEquals(200, post(context, "/api/accesso", credenziali));
            Utente utente = context.getBean(UtenteRepository.class).recuperaPerEmail("visitatore@example.com");
            assertNotNull(utente.getId());
            assertTrue(context.getBean(CodificatorePassword.class)
                    .verifica("Password1!", utente.recuperaPasswordHash()));
            var hackathons = context.getBean(HackathonRepository.class);
            Hackathon h = hackathons.recuperaHackathon(scenario.partecipazione().getHackathon().getId());
            Team team = context.getBean(TeamRepository.class).recuperaTeam(scenario.responsabile());
            Partecipazione p = context.getBean(PartecipazioneRepository.class).recuperaPartecipazione(team, h);
            Sottomissione s = context.getBean(SottomissioneRepository.class).recuperaSottomissione(p);
            assertEquals(sottomissioneId, s.getId());
            assertEquals("Soluzione completa", s.getContenuto());
            assertEquals(valutazioneId, s.getValutazione().getId());
            assertEquals(0, new BigDecimal("8.75").compareTo(s.getValutazione().getPunteggio()));
            assertEquals(scenario.giudice().getId(), h.getGiudice().getId());
            assertEquals(TipoStatoHackathon.IN_VALUTAZIONE, h.getStato());
            assertTrue(hackathons.ottieniHackathonValutabili(scenario.giudice()).isEmpty());
            assertEquals(scenario.invito().getId(), context.getBean(InvitoRepository.class)
                    .recuperaInvitiRicevuti(scenario.destinatario()).getFirst().getId());
        }
    }

    private int post(ConfigurableApplicationContext context, String percorso, String json) throws Exception {
        int porta = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        HttpRequest richiesta = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + percorso))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(richiesta, HttpResponse.BodyHandlers.ofString()).statusCode();
        }
    }

    private Scenario preparaScenario(ConfigurableApplicationContext context) {
        var utenti = context.getBean(UtenteRepository.class);
        Utente organizzatore = Utente.crea("organizzatore@example.com", "hash-organizzatore");
        Utente giudice = Utente.crea("giudice@example.com", "hash-giudice");
        Utente mentore = Utente.crea("mentore@example.com", "hash-mentore");
        Utente responsabile = Utente.crea("responsabile@example.com", "hash-responsabile");
        Utente destinatario = Utente.crea("destinatario@example.com", "hash-destinatario");
        List.of(organizzatore, giudice, mentore, responsabile, destinatario).forEach(utenti::salva);
        Team team = Team.crea("ByteBuilders", responsabile, responsabile);
        context.getBean(TeamRepository.class).salva(team);
        Invito invito = Invito.crea(team, destinatario);
        context.getBean(InvitoRepository.class).salva(invito);
        LocalDate inizio = LocalDate.now().minusDays(10);
        Hackathon h = Hackathon.crea(new DatiHackathon(
                "HackHub", "Regolamento", "Criteri", inizio.minusDays(2),
                inizio, inizio.plusDays(2), "Camerino", new BigDecimal("100.50"), 5
        ), organizzatore, giudice, List.of(mentore));
        context.getBean(HackathonRepository.class).salva(h);
        Partecipazione p = Partecipazione.crea(h, team);
        context.getBean(PartecipazioneRepository.class).salva(p);
        return new Scenario(responsabile, destinatario, giudice, team, invito, p);
    }

    private void verificaJdbc(ConfigurableApplicationContext context) {
        verificaRepository(context, Map.of(
                UtenteRepository.class, JdbcUtenteRepository.class,
                TeamRepository.class, JdbcTeamRepository.class,
                InvitoRepository.class, JdbcInvitoRepository.class,
                HackathonRepository.class, JdbcHackathonRepository.class,
                PartecipazioneRepository.class, JdbcPartecipazioneRepository.class,
                SottomissioneRepository.class, JdbcSottomissioneRepository.class,
                ValutazioneRepository.class, JdbcValutazioneRepository.class
        ));
    }

    private void verificaRepository(ConfigurableApplicationContext context, Map<Class<?>, Class<?>> tipi) {
        tipi.forEach((interfaccia, implementazione) -> {
            assertEquals(1, context.getBeansOfType(interfaccia).size(), interfaccia.getSimpleName());
            assertInstanceOf(implementazione, context.getBean(interfaccia));
        });
    }

    private record Scenario(
            Utente responsabile, Utente destinatario, Utente giudice,
            Team team, Invito invito, Partecipazione partecipazione
    ) { }
}