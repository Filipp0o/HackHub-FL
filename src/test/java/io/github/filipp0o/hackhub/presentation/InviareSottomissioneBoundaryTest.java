package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.InviareSottomissioneControl;
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

import static org.junit.jupiter.api.Assertions.*;

class InviareSottomissioneBoundaryTest {

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new InviareSottomissioneBoundary(
                        null
                )
        );
    }

    @Test
    void selezionaInvioSottomissioneRestituiscePartecipazione() {
        Utente utente =
                new Utente(1L);

        Team team = Team.crea(
                "ByteBuilders",
                utente,
                utente
        );

        Hackathon hackathon =
                creaHackathonConScadenzaFutura();

        Partecipazione partecipazione =
                Partecipazione.crea(
                        hackathon,
                        team
                );

        TeamRepositoryFinto teamRepository =
                new TeamRepositoryFinto();

        teamRepository.team = team;

        PartecipazioneRepositoryFinto
                partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        partecipazioneRepository.partecipazione =
                partecipazione;

        InviareSottomissioneBoundary boundary =
                creaBoundary(
                        teamRepository,
                        partecipazioneRepository,
                        new SottomissioneRepositoryFinto()
                );

        Partecipazione risultato =
                boundary.selezionaInvioSottomissione(
                        utente,
                        hackathon
                );

        assertAll(
                () -> assertSame(
                        partecipazione,
                        risultato
                ),
                () -> assertSame(
                        utente,
                        teamRepository.utenteRicevuto
                ),
                () -> assertSame(
                        team,
                        partecipazioneRepository.teamRicevuto
                ),
                () -> assertSame(
                        hackathon,
                        partecipazioneRepository.hackathonRicevuto
                )
        );
    }

    @Test
    void inserisceContenutoEInviaSottomissione() {
        Utente responsabile =
                new Utente(1L);

        Team team = Team.crea(
                "ByteBuilders",
                responsabile,
                responsabile
        );

        Hackathon hackathon =
                creaHackathonConScadenzaFutura();

        Partecipazione partecipazione =
                Partecipazione.crea(
                        hackathon,
                        team
                );

        SottomissioneRepositoryFinto
                sottomissioneRepository =
                new SottomissioneRepositoryFinto();

        InviareSottomissioneBoundary boundary =
                creaBoundary(
                        new TeamRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        sottomissioneRepository
                );

        boundary.inserisciContenutoEInvia(
                partecipazione,
                "https://github.com/team/progetto"
        );

        Sottomissione sottomissione =
                sottomissioneRepository
                        .sottomissioneSalvata;

        assertAll(
                () -> assertNotNull(
                        sottomissione
                ),
                () -> assertEquals(
                        "https://github.com/team/progetto",
                        sottomissione.getContenuto()
                ),
                () -> assertSame(
                        partecipazione,
                        sottomissione.getPartecipazione()
                ),
                () -> assertSame(
                        sottomissione,
                        partecipazione.getSottomissione()
                ),
                () -> assertEquals(
                        1,
                        sottomissioneRepository.numeroSalvataggi
                )
        );
    }

    @Test
    void impedisceInvioConContenutoVuoto() {
        Utente responsabile =
                new Utente(1L);

        Team team = Team.crea(
                "ByteBuilders",
                responsabile,
                responsabile
        );

        Partecipazione partecipazione =
                Partecipazione.crea(
                        creaHackathonConScadenzaFutura(),
                        team
                );

        SottomissioneRepositoryFinto
                sottomissioneRepository =
                new SottomissioneRepositoryFinto();

        InviareSottomissioneBoundary boundary =
                creaBoundary(
                        new TeamRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        sottomissioneRepository
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> boundary.inserisciContenutoEInvia(
                        partecipazione,
                        "   "
                )
        );

        assertEquals(
                0,
                sottomissioneRepository.numeroSalvataggi
        );
    }

    @Test
    void impedisceInvioDopoLaScadenza() {
        Utente responsabile =
                new Utente(1L);

        Team team = Team.crea(
                "ByteBuilders",
                responsabile,
                responsabile
        );

        Partecipazione partecipazione =
                Partecipazione.crea(
                        creaHackathonConScadenzaTrascorsa(),
                        team
                );

        SottomissioneRepositoryFinto
                sottomissioneRepository =
                new SottomissioneRepositoryFinto();

        InviareSottomissioneBoundary boundary =
                creaBoundary(
                        new TeamRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        sottomissioneRepository
                );

        assertThrows(
                IllegalStateException.class,
                () -> boundary.inserisciContenutoEInvia(
                        partecipazione,
                        "Repository del progetto"
                )
        );

        assertEquals(
                0,
                sottomissioneRepository.numeroSalvataggi
        );
    }

    private InviareSottomissioneBoundary creaBoundary(
            TeamRepository teamRepository,
            PartecipazioneRepository partecipazioneRepository,
            SottomissioneRepository sottomissioneRepository
    ) {
        InviareSottomissioneControl control =
                new InviareSottomissioneControl(
                        teamRepository,
                        partecipazioneRepository,
                        sottomissioneRepository
                );

        return new InviareSottomissioneBoundary(
                control
        );
    }

    private Hackathon creaHackathonConScadenzaFutura() {
        LocalDate oggi = LocalDate.now();

        return creaHackathon(
                oggi.minusDays(3),
                oggi.minusDays(2),
                oggi.plusDays(3)
        );
    }

    private Hackathon creaHackathonConScadenzaTrascorsa() {
        LocalDate oggi = LocalDate.now();

        return creaHackathon(
                oggi.minusDays(5),
                oggi.minusDays(4),
                oggi.minusDays(1)
        );
    }

    private Hackathon creaHackathon(
            LocalDate scadenzaIscrizioni,
            LocalDate dataInizio,
            LocalDate dataFine
    ) {
        DatiHackathon dati =
                new DatiHackathon(
                        "HackHub",
                        "Regolamento",
                        "Criteri di valutazione",
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
        private Hackathon hackathonRicevuto;

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
            hackathonRicevuto = hackathon;

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

        private Sottomissione sottomissioneSalvata;
        private int numeroSalvataggi;

        @Override
        public void salva(
                Sottomissione sottomissione
        ) {
            sottomissioneSalvata =
                    sottomissione;

            numeroSalvataggi++;
        }
    }
}