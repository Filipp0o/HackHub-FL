package io.github.filipp0o.hackhub.application;

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

class InviareSottomissioneControlTest {

    @Test
    void rifiutaDipendenzeNulle() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new InviareSottomissioneControl(
                                null,
                                new PartecipazioneRepositoryFinto(),
                                new SottomissioneRepositoryFinto()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new InviareSottomissioneControl(
                                new TeamRepositoryFinto(),
                                null,
                                new SottomissioneRepositoryFinto()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new InviareSottomissioneControl(
                                new TeamRepositoryFinto(),
                                new PartecipazioneRepositoryFinto(),
                                null
                        )
                )
        );
    }

    @Test
    void avviaInvioRecuperaPartecipazioneDelTeam() {
        Utente utente =
                new Utente(1L);

        Team team =
                Team.crea(
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

        teamRepository.team =
                team;

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        partecipazioneRepository.partecipazione =
                partecipazione;

        InviareSottomissioneControl control =
                new InviareSottomissioneControl(
                        teamRepository,
                        partecipazioneRepository,
                        new SottomissioneRepositoryFinto()
                );

        Partecipazione risultato =
                control.avviaInvioSottomissione(
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
    void rifiutaUtenteOHackathonNulliDuranteAvvio() {
        InviareSottomissioneControl control =
                new InviareSottomissioneControl(
                        new TeamRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        new SottomissioneRepositoryFinto()
                );

        Utente utente =
                new Utente(1L);

        Hackathon hackathon =
                creaHackathonConScadenzaFutura();

        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.avviaInvioSottomissione(
                                null,
                                hackathon
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.avviaInvioSottomissione(
                                utente,
                                null
                        )
                )
        );
    }

    @Test
    void impedisceAvvioSePartecipazioneEsclusa() {
        Utente utente =
                new Utente(1L);

        Team team =
                Team.crea(
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

        partecipazione.escludi();

        TeamRepositoryFinto teamRepository =
                new TeamRepositoryFinto();

        teamRepository.team =
                team;

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        partecipazioneRepository.partecipazione =
                partecipazione;

        InviareSottomissioneControl control =
                new InviareSottomissioneControl(
                        teamRepository,
                        partecipazioneRepository,
                        new SottomissioneRepositoryFinto()
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.avviaInvioSottomissione(
                        utente,
                        hackathon
                )
        );
    }

    @Test
    void impedisceAvvioSeEsisteGiaSottomissione() {
        Utente utente =
                new Utente(1L);

        Team team =
                Team.crea(
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

        Sottomissione.crea(
                partecipazione,
                "Prima sottomissione"
        );

        TeamRepositoryFinto teamRepository =
                new TeamRepositoryFinto();

        teamRepository.team =
                team;

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        partecipazioneRepository.partecipazione =
                partecipazione;

        InviareSottomissioneControl control =
                new InviareSottomissioneControl(
                        teamRepository,
                        partecipazioneRepository,
                        new SottomissioneRepositoryFinto()
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.avviaInvioSottomissione(
                        utente,
                        hackathon
                )
        );
    }

    @Test
    void rifiutaContenutoNulloOVuoto() {
        InviareSottomissioneControl control =
                new InviareSottomissioneControl(
                        new TeamRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        new SottomissioneRepositoryFinto()
                );

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> control.verificaContenuto(null)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> control.verificaContenuto("")
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> control.verificaContenuto("   ")
                )
        );
    }

    @Test
    void accettaContenutoValido() {
        InviareSottomissioneControl control =
                new InviareSottomissioneControl(
                        new TeamRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        new SottomissioneRepositoryFinto()
                );

        assertDoesNotThrow(
                () -> control.verificaContenuto(
                        "https://github.com/team/progetto"
                )
        );
    }

    @Test
    void creaESalvaSottomissione() {
        Utente utente =
                new Utente(1L);

        Team team =
                Team.crea(
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

        SottomissioneRepositoryFinto sottomissioneRepository =
                new SottomissioneRepositoryFinto();

        InviareSottomissioneControl control =
                new InviareSottomissioneControl(
                        new TeamRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        sottomissioneRepository
                );

        control.inviaSottomissione(
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
                () -> assertSame(
                        partecipazione,
                        sottomissione.getPartecipazione()
                ),
                () -> assertEquals(
                        "https://github.com/team/progetto",
                        sottomissione.getContenuto()
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
    void impedisceInvioSeScadenzaTrascorsa() {
        Utente utente =
                new Utente(1L);

        Team team =
                Team.crea(
                        "ByteBuilders",
                        utente,
                        utente
                );

        Hackathon hackathon =
                creaHackathonConScadenzaTrascorsa();

        Partecipazione partecipazione =
                Partecipazione.crea(
                        hackathon,
                        team
                );

        SottomissioneRepositoryFinto sottomissioneRepository =
                new SottomissioneRepositoryFinto();

        InviareSottomissioneControl control =
                new InviareSottomissioneControl(
                        new TeamRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        sottomissioneRepository
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.inviaSottomissione(
                        partecipazione,
                        "contenuto"
                )
        );

        assertEquals(
                0,
                sottomissioneRepository.numeroSalvataggi
        );
    }

    @Test
    void impedisceInvioDirettoSeEsisteGiaSottomissione() {
        Utente utente =
                new Utente(1L);

        Team team =
                Team.crea(
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

        Sottomissione.crea(
                partecipazione,
                "Prima sottomissione"
        );

        SottomissioneRepositoryFinto sottomissioneRepository =
                new SottomissioneRepositoryFinto();

        InviareSottomissioneControl control =
                new InviareSottomissioneControl(
                        new TeamRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        sottomissioneRepository
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.inviaSottomissione(
                        partecipazione,
                        "Seconda sottomissione"
                )
        );

        assertEquals(
                0,
                sottomissioneRepository.numeroSalvataggi
        );
    }

    private static Hackathon creaHackathonConScadenzaFutura() {
        LocalDate oggi =
                LocalDate.now();

        return creaHackathon(
                oggi.minusDays(3),
                oggi.minusDays(2),
                oggi.plusDays(3)
        );
    }

    private static Hackathon creaHackathonConScadenzaTrascorsa() {
        LocalDate oggi =
                LocalDate.now();

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
            utenteRicevuto =
                    utente;

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
            teamRicevuto =
                    team;

            hackathonRicevuto =
                    hackathon;

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