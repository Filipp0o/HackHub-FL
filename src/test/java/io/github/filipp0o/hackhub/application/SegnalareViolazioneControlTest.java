package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.NotificaSegnalazione;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Segnalazione;
import io.github.filipp0o.hackhub.domain.StatoSegnalazione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SegnalareViolazioneControlTest {

    @Test
    void rifiutaDipendenzeNulle() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new SegnalareViolazioneControl(
                                null,
                                new PartecipazioneRepositoryFinto(),
                                new SegnalazioneRepositoryFinto()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new SegnalareViolazioneControl(
                                new HackathonRepositoryFinto(),
                                null,
                                new SegnalazioneRepositoryFinto()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new SegnalareViolazioneControl(
                                new HackathonRepositoryFinto(),
                                new PartecipazioneRepositoryFinto(),
                                null
                        )
                )
        );
    }

    @Test
    void recuperaHackathonSegnalabiliDalRepository() {
        Utente organizzatore = new Utente(1L);
        Utente mentore = new Utente(3L);

        Hackathon hackathon =
                creaHackathon(organizzatore, mentore);

        HackathonRepositoryFinto hackathonRepository =
                new HackathonRepositoryFinto();

        hackathonRepository.hackathonSegnalabili =
                List.of(hackathon);

        SegnalareViolazioneControl control =
                new SegnalareViolazioneControl(
                        hackathonRepository,
                        new PartecipazioneRepositoryFinto(),
                        new SegnalazioneRepositoryFinto()
                );

        List<Hackathon> risultato =
                control.avviaSegnalazioneViolazione(mentore);

        assertAll(
                () -> assertEquals(
                        List.of(hackathon),
                        risultato
                ),
                () -> assertSame(
                        mentore,
                        hackathonRepository.mentoreRicevuto
                ),
                () -> assertEquals(
                        1,
                        hackathonRepository.numeroRecuperiSegnalabili
                )
        );
    }

    @Test
    void recuperaPartecipazioniDellHackathonSelezionato() {
        Utente organizzatore = new Utente(1L);
        Utente mentore = new Utente(3L);

        Hackathon hackathon =
                creaHackathon(organizzatore, mentore);

        Partecipazione prima =
                creaPartecipazione(hackathon, 10L);

        Partecipazione seconda =
                creaPartecipazione(hackathon, 11L);

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        partecipazioneRepository.partecipazioni =
                List.of(prima, seconda);

        SegnalareViolazioneControl control =
                new SegnalareViolazioneControl(
                        new HackathonRepositoryFinto(),
                        partecipazioneRepository,
                        new SegnalazioneRepositoryFinto()
                );

        List<Partecipazione> risultato =
                control.selezionaHackathon(hackathon);

        assertAll(
                () -> assertEquals(
                        List.of(prima, seconda),
                        risultato
                ),
                () -> assertSame(
                        hackathon,
                        partecipazioneRepository.hackathonRicevuto
                ),
                () -> assertEquals(
                        1,
                        partecipazioneRepository.numeroRecuperi
                )
        );
    }

    @Test
    void restituisceRegolamentoQuandoSelezionaTeam() {
        Utente organizzatore = new Utente(1L);
        Utente mentore = new Utente(3L);

        Hackathon hackathon =
                creaHackathon(organizzatore, mentore);

        Partecipazione partecipazione =
                creaPartecipazione(hackathon, 10L);

        SegnalareViolazioneControl control =
                new SegnalareViolazioneControl(
                        new HackathonRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        new SegnalazioneRepositoryFinto()
                );

        String regolamento =
                control.selezionaTeam(partecipazione);

        assertEquals(
                "Regolamento ufficiale",
                regolamento
        );
    }

    @Test
    void verificaDescrizione() {
        SegnalareViolazioneControl control =
                new SegnalareViolazioneControl(
                        new HackathonRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        new SegnalazioneRepositoryFinto()
                );

        assertAll(
                () -> assertDoesNotThrow(
                        () -> control.verificaDescrizione(
                                "Uso di materiale non consentito"
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> control.verificaDescrizione(null)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> control.verificaDescrizione(" ")
                )
        );
    }

    @Test
    void registraSegnalazioneENotificaOrganizzatore() {
        Utente organizzatore = new Utente(1L);

        /*
         * Questo è il Mentore memorizzato nell'Hackathon.
         */
        Utente mentoreAssegnato = new Utente(3L);

        Hackathon hackathon =
                creaHackathon(
                        organizzatore,
                        mentoreAssegnato
                );

        Partecipazione partecipazione =
                creaPartecipazione(hackathon, 10L);

        SegnalazioneRepositoryFinto segnalazioneRepository =
                new SegnalazioneRepositoryFinto();

        SegnalareViolazioneControl control =
                new SegnalareViolazioneControl(
                        new HackathonRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        segnalazioneRepository
                );

        /*
         * Istanza Java diversa ma stesso identificatore:
         * rappresenta lo stesso Mentore.
         */
        Utente mentore = new Utente(3L);

        String descrizione =
                "Violazione del regolamento";

        control.registraSegnalazioneConNotifica(
                mentore,
                partecipazione,
                descrizione
        );

        Segnalazione segnalazione =
                segnalazioneRepository.segnalazioneSalvata;

        NotificaSegnalazione notifica =
                segnalazioneRepository.notificaSalvata;

        assertAll(
                () -> assertNotNull(segnalazione),
                () -> assertNotNull(notifica),
                () -> assertEquals(
                        1,
                        segnalazioneRepository.numeroSalvataggiConNotifica
                ),
                () -> assertEquals(
                        descrizione,
                        segnalazione.getDescrizione()
                ),
                () -> assertEquals(
                        StatoSegnalazione.DA_ESAMINARE,
                        segnalazione.getStato()
                ),
                () -> assertSame(
                        mentore,
                        segnalazione.getMentoreSegnalante()
                ),
                () -> assertSame(
                        partecipazione,
                        segnalazione.getPartecipazione()
                ),
                () -> assertSame(
                        organizzatore,
                        notifica.getDestinatario()
                ),
                () -> assertSame(
                        segnalazione,
                        notifica.getSegnalazione()
                ),
                () -> assertSame(
                        notifica,
                        segnalazione.getNotificaSegnalazione()
                ),
                () -> assertFalse(
                        notifica.getLetta()
                )
        );
    }

    @Test
    void nonRegistraSegnalazioneSeMentoreNonAssegnato() {
        Utente organizzatore = new Utente(1L);
        Utente mentoreAssegnato = new Utente(3L);

        Hackathon hackathon =
                creaHackathon(
                        organizzatore,
                        mentoreAssegnato
                );

        Partecipazione partecipazione =
                creaPartecipazione(hackathon, 10L);

        SegnalazioneRepositoryFinto segnalazioneRepository =
                new SegnalazioneRepositoryFinto();

        SegnalareViolazioneControl control =
                new SegnalareViolazioneControl(
                        new HackathonRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        segnalazioneRepository
                );

        Utente altroMentore = new Utente(99L);

        assertThrows(
                IllegalArgumentException.class,
                () -> control.registraSegnalazioneConNotifica(
                        altroMentore,
                        partecipazione,
                        "Violazione"
                )
        );

        assertAll(
                () -> assertNull(
                        segnalazioneRepository.segnalazioneSalvata
                ),
                () -> assertNull(
                        segnalazioneRepository.notificaSalvata
                ),
                () -> assertEquals(
                        0,
                        segnalazioneRepository.numeroSalvataggiConNotifica
                )
        );
    }

    @Test
    void nonRegistraSegnalazioneSeDescrizioneNonValida() {
        Utente organizzatore = new Utente(1L);
        Utente mentore = new Utente(3L);

        Hackathon hackathon =
                creaHackathon(organizzatore, mentore);

        Partecipazione partecipazione =
                creaPartecipazione(hackathon, 10L);

        SegnalazioneRepositoryFinto segnalazioneRepository =
                new SegnalazioneRepositoryFinto();

        SegnalareViolazioneControl control =
                new SegnalareViolazioneControl(
                        new HackathonRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        segnalazioneRepository
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> control.registraSegnalazioneConNotifica(
                        mentore,
                        partecipazione,
                        " "
                )
        );

        assertEquals(
                0,
                segnalazioneRepository.numeroSalvataggiConNotifica
        );
    }

    private Hackathon creaHackathon(
            Utente organizzatore,
            Utente mentore
    ) {
        LocalDate oggi = LocalDate.now();

        DatiHackathon dati = new DatiHackathon(
                "HackHub 2026",
                "Regolamento ufficiale",
                "Qualità, completezza e innovazione",
                oggi.minusDays(3),
                oggi.minusDays(2),
                oggi.plusDays(2),
                "Roma",
                BigDecimal.valueOf(5000),
                5
        );

        return Hackathon.crea(
                dati,
                organizzatore,
                new Utente(2L),
                List.of(mentore)
        );
    }

    private Partecipazione creaPartecipazione(
            Hackathon hackathon,
            long idResponsabile
    ) {
        Utente responsabile =
                new Utente(idResponsabile);

        Team team = Team.crea(
                "Team " + idResponsabile,
                responsabile,
                responsabile
        );

        return new Partecipazione(
                hackathon,
                team
        );
    }

    private static class HackathonRepositoryFinto
            implements HackathonRepository {

        private List<Hackathon> hackathonSegnalabili =
                List.of();

        private Utente mentoreRicevuto;
        private int numeroRecuperiSegnalabili;

        @Override
        public List<Hackathon> ottieniHackathonValutabili(
                Utente giudice
        ) {
            return List.of();
        }

        @Override
        public List<Hackathon> ottieniHackathonSegnalabili(
                Utente mentore
        ) {
            mentoreRicevuto = mentore;
            numeroRecuperiSegnalabili++;
            return hackathonSegnalabili;
        }

        @Override
        public void salva(Hackathon hackathon) {
        }

        @Override
        public List<Hackathon> ottieniHackathonApertiAlleIscrizioni() {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }

    private static class PartecipazioneRepositoryFinto
            implements PartecipazioneRepository {

        private List<Partecipazione> partecipazioni =
                List.of();

        private Hackathon hackathonRicevuto;
        private int numeroRecuperi;

        @Override
        public List<Partecipazione> ottieniPartecipazioni(
                Hackathon hackathon
        ) {
            hackathonRicevuto = hackathon;
            numeroRecuperi++;
            return partecipazioni;
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

    private static class SegnalazioneRepositoryFinto
            implements SegnalazioneRepository {

        private Segnalazione segnalazioneSalvata;
        private NotificaSegnalazione notificaSalvata;
        private int numeroSalvataggiConNotifica;

        @Override
        public List<Segnalazione> ottieniSegnalazioniDaEsaminare(
                Utente organizzatore
        ) {
            return List.of();
        }

        @Override
        public void salva(
                Segnalazione segnalazione
        ) {
        }

        @Override
        public void salvaConNotifica(
                Segnalazione segnalazione,
                NotificaSegnalazione notifica
        ) {
            segnalazioneSalvata = segnalazione;
            notificaSalvata = notifica;
            numeroSalvataggiConNotifica++;
        }

        @Override
        public void salvaNotifica(
                NotificaSegnalazione notifica
        ) {
        }
    }
}
