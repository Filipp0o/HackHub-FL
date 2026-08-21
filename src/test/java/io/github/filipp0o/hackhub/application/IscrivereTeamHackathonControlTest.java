package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.StatoPartecipazione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IscrivereTeamHackathonControlTest {

    @Test
    void rifiutaDipendenzeNulle() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new IscrivereTeamHackathonControl(
                                null,
                                new TeamRepositoryFinto(),
                                new PartecipazioneRepositoryFinto()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new IscrivereTeamHackathonControl(
                                new HackathonRepositoryFinto(),
                                null,
                                new PartecipazioneRepositoryFinto()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new IscrivereTeamHackathonControl(
                                new HackathonRepositoryFinto(),
                                new TeamRepositoryFinto(),
                                null
                        )
                )
        );
    }

    @Test
    void recuperaHackathonApertiAlleIscrizioni() {
        Hackathon primo =
                creaHackathonAperto("Primo Hackathon");

        Hackathon secondo =
                creaHackathonAperto("Secondo Hackathon");

        HackathonRepositoryFinto hackathonRepository =
                new HackathonRepositoryFinto();

        hackathonRepository.hackathonAperti =
                List.of(primo, secondo);

        IscrivereTeamHackathonControl control =
                new IscrivereTeamHackathonControl(
                        hackathonRepository,
                        new TeamRepositoryFinto(),
                        new PartecipazioneRepositoryFinto()
                );

        List<Hackathon> risultato =
                control.avviaIscrizione();

        assertAll(
                () -> assertEquals(
                        List.of(primo, secondo),
                        risultato
                ),
                () -> assertEquals(
                        1,
                        hackathonRepository.numeroRecuperiAperti
                )
        );
    }

    @Test
    void rifiutaUtenteOHackathonNulliDuranteLaVerifica() {
        IscrivereTeamHackathonControl control =
                new IscrivereTeamHackathonControl(
                        new HackathonRepositoryFinto(),
                        new TeamRepositoryFinto(),
                        new PartecipazioneRepositoryFinto()
                );

        Hackathon hackathon =
                creaHackathonAperto("Hackathon");

        Utente utente =
                new Utente(1L);

        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.verificaIscrizione(
                                null,
                                hackathon
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.verificaIscrizione(
                                utente,
                                null
                        )
                )
        );
    }

    @Test
    void verificaIscrizioneAmmissibileERestituisceTeam() {
        Utente utente =
                new Utente(1L);

        Team team =
                Team.crea(
                        "ByteBuilders",
                        utente,
                        utente
                );

        Hackathon hackathon =
                creaHackathonAperto("Hackathon");

        TeamRepositoryFinto teamRepository =
                new TeamRepositoryFinto();

        teamRepository.team =
                team;

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        IscrivereTeamHackathonControl control =
                new IscrivereTeamHackathonControl(
                        new HackathonRepositoryFinto(),
                        teamRepository,
                        partecipazioneRepository
                );

        Team risultato =
                control.verificaIscrizione(
                        utente,
                        hackathon
                );

        assertAll(
                () -> assertSame(
                        team,
                        risultato
                ),
                () -> assertSame(
                        utente,
                        teamRepository.utenteRicevuto
                ),
                () -> assertSame(
                        team,
                        partecipazioneRepository.teamVerificato
                ),
                () -> assertSame(
                        hackathon,
                        partecipazioneRepository.hackathonVerificato
                ),
                () -> assertEquals(
                        1,
                        partecipazioneRepository.numeroVerificheEsistenza
                )
        );
    }

    @Test
    void impedisceIscrizioneSeScadenzaIscrizioniTrascorsa() {
        Utente utente =
                new Utente(1L);

        Team team =
                Team.crea(
                        "ByteBuilders",
                        utente,
                        utente
                );

        TeamRepositoryFinto teamRepository =
                new TeamRepositoryFinto();

        teamRepository.team =
                team;

        Hackathon hackathon =
                creaHackathonConIscrizioniScadute(
                        "Hackathon scaduto"
                );

        IscrivereTeamHackathonControl control =
                new IscrivereTeamHackathonControl(
                        new HackathonRepositoryFinto(),
                        teamRepository,
                        new PartecipazioneRepositoryFinto()
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.verificaIscrizione(
                        utente,
                        hackathon
                )
        );
    }

    @Test
    void impedisceIscrizioneSeTeamGiaIscritto() {
        Utente utente =
                new Utente(1L);

        Team team =
                Team.crea(
                        "ByteBuilders",
                        utente,
                        utente
                );

        Hackathon hackathon =
                creaHackathonAperto("Hackathon");

        TeamRepositoryFinto teamRepository =
                new TeamRepositoryFinto();

        teamRepository.team =
                team;

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        partecipazioneRepository.partecipazioneEsistente =
                true;

        IscrivereTeamHackathonControl control =
                new IscrivereTeamHackathonControl(
                        new HackathonRepositoryFinto(),
                        teamRepository,
                        partecipazioneRepository
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.verificaIscrizione(
                        utente,
                        hackathon
                )
        );
    }

    @Test
    void rifiutaTeamOHackathonNulliDuranteLaConferma() {
        IscrivereTeamHackathonControl control =
                new IscrivereTeamHackathonControl(
                        new HackathonRepositoryFinto(),
                        new TeamRepositoryFinto(),
                        new PartecipazioneRepositoryFinto()
                );

        Utente utente =
                new Utente(1L);

        Team team =
                Team.crea(
                        "ByteBuilders",
                        utente,
                        utente
                );

        Hackathon hackathon =
                creaHackathonAperto("Hackathon");

        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.confermaIscrizione(
                                null,
                                hackathon
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.confermaIscrizione(
                                team,
                                null
                        )
                )
        );
    }

    @Test
    void creaESalvaPartecipazioneAllaConferma() {
        Utente utente =
                new Utente(1L);

        Team team =
                Team.crea(
                        "ByteBuilders",
                        utente,
                        utente
                );

        Hackathon hackathon =
                creaHackathonAperto("Hackathon");

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        IscrivereTeamHackathonControl control =
                new IscrivereTeamHackathonControl(
                        new HackathonRepositoryFinto(),
                        new TeamRepositoryFinto(),
                        partecipazioneRepository
                );

        control.confermaIscrizione(
                team,
                hackathon
        );

        Partecipazione partecipazione =
                partecipazioneRepository.partecipazioneSalvata;

        assertAll(
                () -> assertNotNull(
                        partecipazione
                ),
                () -> assertSame(
                        team,
                        partecipazione.getTeam()
                ),
                () -> assertSame(
                        hackathon,
                        partecipazione.getHackathon()
                ),
                () -> assertEquals(
                        StatoPartecipazione.ATTIVA,
                        partecipazione.getStato()
                ),
                () -> assertEquals(
                        1,
                        partecipazioneRepository.numeroSalvataggi
                )
        );
    }

    @Test
    void impedisceConfermaSeTeamGiaIscritto() {
        Utente utente =
                new Utente(1L);

        Team team =
                Team.crea(
                        "ByteBuilders",
                        utente,
                        utente
                );

        Hackathon hackathon =
                creaHackathonAperto("Hackathon");

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        partecipazioneRepository.partecipazioneEsistente =
                true;

        IscrivereTeamHackathonControl control =
                new IscrivereTeamHackathonControl(
                        new HackathonRepositoryFinto(),
                        new TeamRepositoryFinto(),
                        partecipazioneRepository
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.confermaIscrizione(
                        team,
                        hackathon
                )
        );

        assertEquals(
                0,
                partecipazioneRepository.numeroSalvataggi
        );
    }

    private static Hackathon creaHackathonAperto(
            String nome
    ) {
        LocalDate oggi =
                LocalDate.now();

        return creaHackathon(
                nome,
                oggi.plusDays(1),
                oggi.plusDays(2),
                oggi.plusDays(5)
        );
    }

    private static Hackathon creaHackathonConIscrizioniScadute(
            String nome
    ) {
        LocalDate oggi =
                LocalDate.now();

        return creaHackathon(
                nome,
                oggi.minusDays(1),
                oggi.plusDays(1),
                oggi.plusDays(5)
        );
    }

    private static Hackathon creaHackathon(
            String nome,
            LocalDate scadenzaIscrizioni,
            LocalDate dataInizio,
            LocalDate dataFine
    ) {
        DatiHackathon dati =
                new DatiHackathon(
                        nome,
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

    private static class HackathonRepositoryFinto
            implements HackathonRepository {

        private List<Hackathon> hackathonAperti =
                List.of();

        private int numeroRecuperiAperti;

        @Override
        public List<Hackathon> ottieniHackathonValutabili(
                Utente giudice
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }

        @Override
        public List<Hackathon> ottieniHackathonSegnalabili(
                Utente mentore
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }

        @Override
        public List<Hackathon>
        ottieniHackathonApertiAlleIscrizioni() {
            numeroRecuperiAperti++;
            return hackathonAperti;
        }

        @Override
        public void salva(
                Hackathon hackathon
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
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

        private boolean partecipazioneEsistente;

        private Team teamVerificato;
        private Hackathon hackathonVerificato;

        private int numeroVerificheEsistenza;

        private Partecipazione partecipazioneSalvata;
        private int numeroSalvataggi;

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
            teamVerificato =
                    team;

            hackathonVerificato =
                    hackathon;

            numeroVerificheEsistenza++;

            return partecipazioneEsistente;
        }

        @Override
        public void salva(
                Partecipazione partecipazione
        ) {
            partecipazioneSalvata =
                    partecipazione;

            numeroSalvataggi++;
        }

        @Override
        public Partecipazione recuperaPartecipazione(
                Team team,
                Hackathon hackathon
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }
}