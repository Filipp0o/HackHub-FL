package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.AggiornareSottomissioneControl;
import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.application.SottomissioneRepository;
import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AggiornareSottomissioneBoundaryTest {

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new AggiornareSottomissioneBoundary(
                        null
                )
        );
    }

    @Test
    void selezionaAggiornamentoERestituisceContenutoCorrente() {
        Scenario scenario =
                creaScenario(
                        creaHackathonConScadenzaFutura()
                );

        String contenuto = scenario.boundary
                .selezionaAggiornamentoSottomissione(
                        scenario.utente,
                        scenario.hackathon
                );

        assertAll(
                () -> assertEquals(
                        "Prima versione",
                        contenuto
                ),
                () -> assertSame(
                        scenario.utente,
                        scenario.teamRepository.utenteRicevuto
                ),
                () -> assertSame(
                        scenario.team,
                        scenario.partecipazioneRepository
                                .teamRicevuto
                ),
                () -> assertSame(
                        scenario.partecipazione,
                        scenario.sottomissioneRepository
                                .partecipazioneRicevuta
                )
        );
    }

    @Test
    void richiedeAggiornamentoESalvaSottomissione() {
        Scenario scenario =
                creaScenario(
                        creaHackathonConScadenzaFutura()
                );

        scenario.boundary.richiediAggiornamento(
                scenario.utente,
                scenario.hackathon,
                "Versione aggiornata"
        );

        assertAll(
                () -> assertEquals(
                        "Versione aggiornata",
                        scenario.sottomissione
                                .ottieniContenuto()
                ),
                () -> assertSame(
                        scenario.sottomissione,
                        scenario.sottomissioneRepository
                                .sottomissioneSalvata
                ),
                () -> assertEquals(
                        1,
                        scenario.sottomissioneRepository
                                .numeroSalvataggi
                )
        );
    }

    @Test
    void impedisceAggiornamentoConContenutoVuoto() {
        Scenario scenario =
                creaScenario(
                        creaHackathonConScadenzaFutura()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> scenario.boundary
                        .richiediAggiornamento(
                                scenario.utente,
                                scenario.hackathon,
                                "   "
                        )
        );

        assertAll(
                () -> assertEquals(
                        "Prima versione",
                        scenario.sottomissione
                                .ottieniContenuto()
                ),
                () -> assertEquals(
                        0,
                        scenario.sottomissioneRepository
                                .numeroSalvataggi
                )
        );
    }

    @Test
    void impedisceAggiornamentoDopoLaScadenza() {
        Scenario scenario =
                creaScenario(
                        creaHackathonConScadenzaTrascorsa()
                );

        assertThrows(
                IllegalStateException.class,
                () -> scenario.boundary
                        .richiediAggiornamento(
                                scenario.utente,
                                scenario.hackathon,
                                "Versione aggiornata"
                        )
        );

        assertAll(
                () -> assertEquals(
                        "Prima versione",
                        scenario.sottomissione
                                .ottieniContenuto()
                ),
                () -> assertEquals(
                        0,
                        scenario.sottomissioneRepository
                                .numeroSalvataggi
                )
        );
    }

    private Scenario creaScenario(
            Hackathon hackathon
    ) {
        Utente utente =
                new Utente(1L);

        Team team = Team.crea(
                "ByteBuilders",
                utente,
                utente
        );

        Partecipazione partecipazione =
                Partecipazione.crea(
                        hackathon,
                        team
                );

        Sottomissione sottomissione =
                Sottomissione.crea(
                        partecipazione,
                        "Prima versione"
                );

        TeamRepositoryFinto teamRepository =
                new TeamRepositoryFinto();

        teamRepository.team = team;

        PartecipazioneRepositoryFinto
                partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        partecipazioneRepository.partecipazione =
                partecipazione;

        SottomissioneRepositoryFinto
                sottomissioneRepository =
                new SottomissioneRepositoryFinto();

        sottomissioneRepository.sottomissione =
                sottomissione;

        AggiornareSottomissioneControl control =
                new AggiornareSottomissioneControl(
                        teamRepository,
                        partecipazioneRepository,
                        sottomissioneRepository
                );

        AggiornareSottomissioneBoundary boundary =
                new AggiornareSottomissioneBoundary(
                        control
                );

        return new Scenario(
                utente,
                team,
                hackathon,
                partecipazione,
                sottomissione,
                teamRepository,
                partecipazioneRepository,
                sottomissioneRepository,
                boundary
        );
    }

    private static Hackathon creaHackathonConScadenzaFutura() {
        LocalDate oggi = LocalDate.now();

        return creaHackathon(
                oggi.minusDays(3),
                oggi.minusDays(2),
                oggi.plusDays(3)
        );
    }

    private static Hackathon creaHackathonConScadenzaTrascorsa() {
        LocalDate oggi = LocalDate.now();

        return creaHackathon(
                oggi.minusDays(5),
                oggi.minusDays(4),
                oggi.minusDays(1)
        );
    }

    private static Hackathon creaHackathon(
            LocalDate scadenzaIscrizioni,
            LocalDate dataInizio,
            LocalDate dataFine
    ) {
        DatiHackathon dati =
                new DatiHackathon(
                        "HackHub",
                        "Regolamento",
                        "Criteri",
                        scadenzaIscrizioni,
                        dataInizio,
                        dataFine,
                        "Camerino",
                        BigDecimal.valueOf(1_000),
                        5
                );

        return Hackathon.crea(
                dati,
                new Utente(10L),
                new Utente(20L),
                List.of(
                        new Utente(30L)
                )
        );
    }

    private record Scenario(
            Utente utente,
            Team team,
            Hackathon hackathon,
            Partecipazione partecipazione,
            Sottomissione sottomissione,
            TeamRepositoryFinto teamRepository,
            PartecipazioneRepositoryFinto partecipazioneRepository,
            SottomissioneRepositoryFinto sottomissioneRepository,
            AggiornareSottomissioneBoundary boundary
    ) {
    }

    private static class TeamRepositoryFinto
            implements TeamRepository {

        private Team team;
        private Utente utenteRicevuto;

        @Override
        public boolean verificaAppartenenzaTeam(
                Utente utente
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }

        @Override
        public Team recuperaTeam(
                Utente utente
        ) {
            utenteRicevuto = utente;
            return team;
        }

        @Override
        public void salva(
                Team team
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }

    private static class PartecipazioneRepositoryFinto
            implements PartecipazioneRepository {

        private Partecipazione partecipazione;
        private Team teamRicevuto;

        @Override
        public List<Partecipazione> ottieniPartecipazioni(
                Hackathon hackathon
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }

        @Override
        public List<Partecipazione>
        recuperaPartecipazioniNonEscluse(
                Hackathon hackathon
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }

        @Override
        public boolean esistePartecipazione(
                Team team,
                Hackathon hackathon
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }

        @Override
        public Partecipazione recuperaPartecipazione(
                Team team,
                Hackathon hackathon
        ) {
            teamRicevuto = team;
            return partecipazione;
        }

        @Override
        public void salva(
                Partecipazione partecipazione
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }

    private static class SottomissioneRepositoryFinto
            implements SottomissioneRepository {

        private Sottomissione sottomissione;
        private Partecipazione partecipazioneRicevuta;

        private Sottomissione sottomissioneSalvata;
        private int numeroSalvataggi;

        @Override
        public Sottomissione recuperaSottomissione(
                Partecipazione partecipazione
        ) {
            partecipazioneRicevuta = partecipazione;
            return sottomissione;
        }

        @Override
        public void salva(
                Sottomissione sottomissione
        ) {
            sottomissioneSalvata = sottomissione;
            numeroSalvataggi++;
        }
    }
}