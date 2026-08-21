package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiDecisioneSegnalazione;
import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.EsitoSegnalazione;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.NotificaSegnalazione;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Segnalazione;
import io.github.filipp0o.hackhub.domain.StatoPartecipazione;
import io.github.filipp0o.hackhub.domain.StatoSegnalazione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EsaminareSegnalazioneControlTest {

    @Test
    void rifiutaDipendenzeNulle() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new EsaminareSegnalazioneControl(
                                null,
                                new PartecipazioneRepositoryFinto()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new EsaminareSegnalazioneControl(
                                new SegnalazioneRepositoryFinto(),
                                null
                        )
                )
        );
    }

    @Test
    void recuperaSegnalazioniDaEsaminareDalRepository() {
        Utente organizzatore = new Utente(1L);
        Segnalazione segnalazione =
                creaSegnalazione(organizzatore);

        SegnalazioneRepositoryFinto segnalazioneRepository =
                new SegnalazioneRepositoryFinto();

        segnalazioneRepository.segnalazioniDaEsaminare =
                List.of(segnalazione);

        EsaminareSegnalazioneControl control =
                new EsaminareSegnalazioneControl(
                        segnalazioneRepository,
                        new PartecipazioneRepositoryFinto()
                );

        List<Segnalazione> risultato =
                control.avviaEsameSegnalazioni(organizzatore);

        assertAll(
                () -> assertEquals(
                        List.of(segnalazione),
                        risultato
                ),
                () -> assertSame(
                        organizzatore,
                        segnalazioneRepository.organizzatoreRicevuto
                ),
                () -> assertEquals(
                        1,
                        segnalazioneRepository.numeroRecuperi
                )
        );
    }

    @Test
    void selezionaSegnalazioneSoloPerOrganizzatoreAutorizzato() {
        Utente organizzatoreHackathon =
                new Utente(1L);

        Segnalazione segnalazione =
                creaSegnalazione(organizzatoreHackathon);

        EsaminareSegnalazioneControl control =
                new EsaminareSegnalazioneControl(
                        new SegnalazioneRepositoryFinto(),
                        new PartecipazioneRepositoryFinto()
                );

        /*
         * Istanza Java diversa, ma stesso identificatore:
         * rappresenta lo stesso organizzatore.
         */
        Utente organizzatore = new Utente(1L);

        assertSame(
                segnalazione,
                control.selezionaSegnalazione(
                        segnalazione,
                        organizzatore
                )
        );

        Utente altroOrganizzatore = new Utente(99L);

        assertThrows(
                IllegalArgumentException.class,
                () -> control.selezionaSegnalazione(
                        segnalazione,
                        altroOrganizzatore
                )
        );
    }

    @Test
    void apreSegnalazioneDaNotificaESegnaNotificaComeLetta() {
        Utente organizzatoreHackathon =
                new Utente(1L);

        Segnalazione segnalazione =
                creaSegnalazione(organizzatoreHackathon);

        NotificaSegnalazione notifica =
                NotificaSegnalazione.crea(
                        segnalazione,
                        organizzatoreHackathon
                );

        SegnalazioneRepositoryFinto segnalazioneRepository =
                new SegnalazioneRepositoryFinto();

        EsaminareSegnalazioneControl control =
                new EsaminareSegnalazioneControl(
                        segnalazioneRepository,
                        new PartecipazioneRepositoryFinto()
                );

        Utente organizzatore = new Utente(1L);

        Segnalazione risultato =
                control.apriSegnalazioneDaNotifica(
                        notifica,
                        organizzatore
                );

        assertAll(
                () -> assertSame(
                        segnalazione,
                        risultato
                ),
                () -> assertTrue(
                        notifica.getLetta()
                ),
                () -> assertSame(
                        notifica,
                        segnalazioneRepository.notificaSalvata
                ),
                () -> assertEquals(
                        1,
                        segnalazioneRepository.numeroSalvataggiNotifica
                )
        );
    }

    @Test
    void nonApreNotificaDestinataAdAltroOrganizzatore() {
        Utente organizzatore =
                new Utente(1L);

        Segnalazione segnalazione =
                creaSegnalazione(organizzatore);

        NotificaSegnalazione notifica =
                NotificaSegnalazione.crea(
                        segnalazione,
                        organizzatore
                );

        SegnalazioneRepositoryFinto segnalazioneRepository =
                new SegnalazioneRepositoryFinto();

        EsaminareSegnalazioneControl control =
                new EsaminareSegnalazioneControl(
                        segnalazioneRepository,
                        new PartecipazioneRepositoryFinto()
                );

        Utente altroOrganizzatore =
                new Utente(99L);

        assertThrows(
                IllegalArgumentException.class,
                () -> control.apriSegnalazioneDaNotifica(
                        notifica,
                        altroOrganizzatore
                )
        );

        assertAll(
                () -> assertFalse(
                        notifica.getLetta()
                ),
                () -> assertEquals(
                        0,
                        segnalazioneRepository.numeroSalvataggiNotifica
                ),
                () -> assertNull(
                        segnalazioneRepository.notificaSalvata
                )
        );
    }

    @Test
    void verificaDecisione() {
        EsaminareSegnalazioneControl control =
                new EsaminareSegnalazioneControl(
                        new SegnalazioneRepositoryFinto(),
                        new PartecipazioneRepositoryFinto()
                );

        assertAll(
                () -> assertDoesNotThrow(
                        () -> control.verificaDecisione(
                                new DatiDecisioneSegnalazione(
                                        EsitoSegnalazione.ARCHIVIATA,
                                        "Segnalazione non fondata"
                                )
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.verificaDecisione(null)
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.verificaDecisione(
                                new DatiDecisioneSegnalazione(
                                        null,
                                        "Motivazione"
                                )
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> control.verificaDecisione(
                                new DatiDecisioneSegnalazione(
                                        EsitoSegnalazione.ARCHIVIATA,
                                        " "
                                )
                        )
                )
        );
    }

    @Test
    void registraDecisioneSenzaEscluderePartecipazione() {
        Utente organizzatoreHackathon =
                new Utente(1L);

        Segnalazione segnalazione =
                creaSegnalazione(organizzatoreHackathon);

        Partecipazione partecipazione =
                segnalazione.getPartecipazione();

        SegnalazioneRepositoryFinto segnalazioneRepository =
                new SegnalazioneRepositoryFinto();

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        EsaminareSegnalazioneControl control =
                new EsaminareSegnalazioneControl(
                        segnalazioneRepository,
                        partecipazioneRepository
                );

        Utente organizzatore = new Utente(1L);

        DatiDecisioneSegnalazione dati =
                new DatiDecisioneSegnalazione(
                        EsitoSegnalazione.VIOLAZIONE_CONFERMATA,
                        "Violazione confermata senza esclusione"
                );

        control.registraDecisione(
                segnalazione,
                organizzatore,
                dati
        );

        assertAll(
                () -> assertEquals(
                        StatoSegnalazione.ESAMINATA,
                        segnalazione.getStato()
                ),
                () -> assertEquals(
                        EsitoSegnalazione.VIOLAZIONE_CONFERMATA,
                        segnalazione.getEsito()
                ),
                () -> assertEquals(
                        dati.motivazione(),
                        segnalazione.getMotivazione()
                ),
                () -> assertSame(
                        organizzatore,
                        segnalazione.getEsaminatore()
                ),
                () -> assertEquals(
                        StatoPartecipazione.ATTIVA,
                        partecipazione.getStato()
                ),
                () -> assertSame(
                        segnalazione,
                        segnalazioneRepository.segnalazioneSalvata
                ),
                () -> assertEquals(
                        1,
                        segnalazioneRepository.numeroSalvataggiSegnalazione
                ),
                () -> assertEquals(
                        0,
                        partecipazioneRepository.numeroSalvataggi
                )
        );
    }

    @Test
    void registraDecisioneConEsclusione() {
        Utente organizzatoreHackathon =
                new Utente(1L);

        Segnalazione segnalazione =
                creaSegnalazione(organizzatoreHackathon);

        Partecipazione partecipazione =
                segnalazione.getPartecipazione();

        SegnalazioneRepositoryFinto segnalazioneRepository =
                new SegnalazioneRepositoryFinto();

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        EsaminareSegnalazioneControl control =
                new EsaminareSegnalazioneControl(
                        segnalazioneRepository,
                        partecipazioneRepository
                );

        Utente organizzatore = new Utente(1L);

        DatiDecisioneSegnalazione dati =
                new DatiDecisioneSegnalazione(
                        EsitoSegnalazione.VIOLAZIONE_CON_ESCLUSIONE,
                        "Violazione grave"
                );

        control.registraDecisione(
                segnalazione,
                organizzatore,
                dati
        );

        assertAll(
                () -> assertEquals(
                        StatoSegnalazione.ESAMINATA,
                        segnalazione.getStato()
                ),
                () -> assertEquals(
                        EsitoSegnalazione.VIOLAZIONE_CON_ESCLUSIONE,
                        segnalazione.getEsito()
                ),
                () -> assertEquals(
                        StatoPartecipazione.ESCLUSA,
                        partecipazione.getStato()
                ),
                () -> assertSame(
                        partecipazione,
                        partecipazioneRepository.partecipazioneSalvata
                ),
                () -> assertEquals(
                        1,
                        partecipazioneRepository.numeroSalvataggi
                ),
                () -> assertSame(
                        segnalazione,
                        segnalazioneRepository.segnalazioneSalvata
                ),
                () -> assertEquals(
                        1,
                        segnalazioneRepository.numeroSalvataggiSegnalazione
                )
        );
    }

    @Test
    void nonRegistraDecisioneNonValidaONonAutorizzata() {
        Utente organizzatore =
                new Utente(1L);

        Segnalazione segnalazione =
                creaSegnalazione(organizzatore);

        SegnalazioneRepositoryFinto segnalazioneRepository =
                new SegnalazioneRepositoryFinto();

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        EsaminareSegnalazioneControl control =
                new EsaminareSegnalazioneControl(
                        segnalazioneRepository,
                        partecipazioneRepository
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> control.registraDecisione(
                        segnalazione,
                        new Utente(99L),
                        new DatiDecisioneSegnalazione(
                                EsitoSegnalazione.ARCHIVIATA,
                                "Motivazione"
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> control.registraDecisione(
                        segnalazione,
                        organizzatore,
                        new DatiDecisioneSegnalazione(
                                EsitoSegnalazione.ARCHIVIATA,
                                " "
                        )
                )
        );

        assertAll(
                () -> assertEquals(
                        StatoSegnalazione.DA_ESAMINARE,
                        segnalazione.getStato()
                ),
                () -> assertEquals(
                        StatoPartecipazione.ATTIVA,
                        segnalazione
                                .getPartecipazione()
                                .getStato()
                ),
                () -> assertEquals(
                        0,
                        segnalazioneRepository.numeroSalvataggiSegnalazione
                ),
                () -> assertEquals(
                        0,
                        partecipazioneRepository.numeroSalvataggi
                )
        );
    }

    private Segnalazione creaSegnalazione(
            Utente organizzatore
    ) {
        Hackathon hackathon =
                creaHackathon(organizzatore);

        Partecipazione partecipazione =
                creaPartecipazione(hackathon);

        return Segnalazione.crea(
                new Utente(3L),
                partecipazione,
                "Possibile violazione del regolamento"
        );
    }

    private Hackathon creaHackathon(
            Utente organizzatore
    ) {
        DatiHackathon dati =
                new DatiHackathon(
                        "HackHub 2026",
                        "Regolamento ufficiale",
                        "Qualità, completezza e innovazione",
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 12),
                        "Roma",
                        BigDecimal.valueOf(5000),
                        5
                );

        Hackathon hackathon = Hackathon.crea(
                dati,
                organizzatore,
                new Utente(2L),
                List.of(new Utente(3L))
        );

        hackathon.aggiornaStato(dati.dataInizio());

        return hackathon;
    }

    private Partecipazione creaPartecipazione(
            Hackathon hackathon
    ) {
        Utente responsabile =
                new Utente(10L);

        Team team = Team.crea(
                "Team segnalato",
                responsabile,
                responsabile
        );

        return new Partecipazione(
                hackathon,
                team
        );
    }

    private static class SegnalazioneRepositoryFinto
            implements SegnalazioneRepository {

        private List<Segnalazione> segnalazioniDaEsaminare =
                List.of();

        private Utente organizzatoreRicevuto;

        private Segnalazione segnalazioneSalvata;
        private NotificaSegnalazione notificaSalvata;

        private int numeroRecuperi;
        private int numeroSalvataggiSegnalazione;
        private int numeroSalvataggiNotifica;

        @Override
        public List<Segnalazione> ottieniSegnalazioniDaEsaminare(
                Utente organizzatore
        ) {
            organizzatoreRicevuto = organizzatore;
            numeroRecuperi++;
            return segnalazioniDaEsaminare;
        }

        @Override
        public void salva(
                Segnalazione segnalazione
        ) {
            segnalazioneSalvata = segnalazione;
            numeroSalvataggiSegnalazione++;
        }

        @Override
        public void salvaConNotifica(
                Segnalazione segnalazione,
                NotificaSegnalazione notifica
        ) {
        }

        @Override
        public void salvaNotifica(
                NotificaSegnalazione notifica
        ) {
            notificaSalvata = notifica;
            numeroSalvataggiNotifica++;
        }
    }

    private static class PartecipazioneRepositoryFinto
            implements PartecipazioneRepository {

        private Partecipazione partecipazioneSalvata;
        private int numeroSalvataggi;

        @Override
        public List<Partecipazione> ottieniPartecipazioni(
                Hackathon hackathon
        ) {
            return List.of();
        }

        @Override
        public List<Partecipazione>
        recuperaPartecipazioniNonEscluse(
                Hackathon hackathon
        ) {
            return List.of();
        }

        @Override
        public void salva(
                Partecipazione partecipazione
        ) {
            partecipazioneSalvata = partecipazione;
            numeroSalvataggi++;
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
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }
}