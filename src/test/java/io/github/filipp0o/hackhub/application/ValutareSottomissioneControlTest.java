package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.DatiValutazione;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;
import io.github.filipp0o.hackhub.domain.StatoHackathon;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.domain.Valutazione;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValutareSottomissioneControlTest {

    @Test
    void rifiutaDipendenzeNulle() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new ValutareSottomissioneControl(
                                null,
                                new PartecipazioneRepositoryFinto(),
                                new ValutazioneRepositoryFinto()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new ValutareSottomissioneControl(
                                new HackathonRepositoryFinto(),
                                null,
                                new ValutazioneRepositoryFinto()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new ValutareSottomissioneControl(
                                new HackathonRepositoryFinto(),
                                new PartecipazioneRepositoryFinto(),
                                null
                        )
                )
        );
    }

    @Test
    void recuperaHackathonValutabiliDalRepository() {
        Utente giudice = new Utente(2L);
        Hackathon hackathon =
                creaHackathonInValutazione(giudice);

        HackathonRepositoryFinto hackathonRepository =
                new HackathonRepositoryFinto();

        hackathonRepository.hackathonValutabili =
                List.of(hackathon);

        ValutareSottomissioneControl control =
                new ValutareSottomissioneControl(
                        hackathonRepository,
                        new PartecipazioneRepositoryFinto(),
                        new ValutazioneRepositoryFinto()
                );

        List<Hackathon> risultato =
                control.avviaValutazioneSottomissione(giudice);

        assertAll(
                () -> assertEquals(
                        List.of(hackathon),
                        risultato
                ),
                () -> assertSame(
                        giudice,
                        hackathonRepository.giudiceRicevuto
                ),
                () -> assertEquals(
                        1,
                        hackathonRepository.numeroRecuperiValutabili
                )
        );
    }

    @Test
    void restituisceSoloSottomissioniNonValutate() {
        Utente giudice = new Utente(2L);
        Hackathon hackathon =
                creaHackathonInValutazione(giudice);

        Partecipazione nonValutata =
                creaPartecipazione(hackathon, 10L);

        Sottomissione sottomissioneNonValutata =
                new Sottomissione(
                        nonValutata,
                        "Sottomissione da valutare"
                );

        Partecipazione senzaSottomissione =
                creaPartecipazione(hackathon, 11L);

        Partecipazione giaValutata =
                creaPartecipazione(hackathon, 12L);

        Sottomissione sottomissioneGiaValutata =
                new Sottomissione(
                        giaValutata,
                        "Sottomissione già valutata"
                );

        Valutazione.crea(
                sottomissioneGiaValutata,
                giudice,
                creaDatiValutazioneValidi()
        );

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        partecipazioneRepository.partecipazioni =
                List.of(
                        nonValutata,
                        senzaSottomissione,
                        giaValutata
                );

        ValutareSottomissioneControl control =
                new ValutareSottomissioneControl(
                        new HackathonRepositoryFinto(),
                        partecipazioneRepository,
                        new ValutazioneRepositoryFinto()
                );

        List<Sottomissione> risultato =
                control.selezionaHackathon(hackathon);

        assertAll(
                () -> assertEquals(
                        List.of(sottomissioneNonValutata),
                        risultato
                ),
                () -> assertEquals(
                        StatoHackathon.IN_VALUTAZIONE,
                        hackathon.getStato()
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
    void rifiutaHackathonNonInValutazione() {
        Utente giudice = new Utente(2L);
        Hackathon hackathon =
                creaHackathonFuturo(giudice);

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        ValutareSottomissioneControl control =
                new ValutareSottomissioneControl(
                        new HackathonRepositoryFinto(),
                        partecipazioneRepository,
                        new ValutazioneRepositoryFinto()
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.selezionaHackathon(hackathon)
        );

        assertEquals(
                0,
                partecipazioneRepository.numeroRecuperi
        );
    }

    @Test
    void rifiutaSottomissioneGiaValutata() {
        Utente giudice = new Utente(2L);
        Hackathon hackathon =
                creaHackathonInValutazione(giudice);

        Partecipazione partecipazione =
                creaPartecipazione(hackathon, 10L);

        Sottomissione sottomissione =
                new Sottomissione(
                        partecipazione,
                        "Sottomissione"
                );

        Valutazione.crea(
                sottomissione,
                giudice,
                creaDatiValutazioneValidi()
        );

        ValutareSottomissioneControl control =
                new ValutareSottomissioneControl(
                        new HackathonRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        new ValutazioneRepositoryFinto()
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.selezionaSottomissione(
                        sottomissione
                )
        );
    }

    @Test
    void verificaDatiValutazione() {
        ValutareSottomissioneControl control =
                new ValutareSottomissioneControl(
                        new HackathonRepositoryFinto(),
                        new PartecipazioneRepositoryFinto(),
                        new ValutazioneRepositoryFinto()
                );

        assertAll(
                () -> assertDoesNotThrow(
                        () -> control.verificaDatiValutazione(
                                creaDatiValutazioneValidi()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.verificaDatiValutazione(null)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> control.verificaDatiValutazione(
                                new DatiValutazione(
                                        " ",
                                        BigDecimal.valueOf(8)
                                )
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> control.verificaDatiValutazione(
                                new DatiValutazione(
                                        "Buon lavoro",
                                        BigDecimal.valueOf(11)
                                )
                        )
                )
        );
    }

    @Test
    void confermaValutazioneESalvaValutazione() {
        Utente giudiceAssegnato = new Utente(2L);
        Hackathon hackathon =
                creaHackathonInValutazione(giudiceAssegnato);

        Partecipazione partecipazione =
                creaPartecipazione(hackathon, 10L);

        Sottomissione sottomissione =
                new Sottomissione(
                        partecipazione,
                        "Sottomissione finale"
                );

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        ValutazioneRepositoryFinto valutazioneRepository =
                new ValutazioneRepositoryFinto();

        ValutareSottomissioneControl control =
                new ValutareSottomissioneControl(
                        new HackathonRepositoryFinto(),
                        partecipazioneRepository,
                        valutazioneRepository
                );

        DatiValutazione dati =
                creaDatiValutazioneValidi();

        /*
         * Istanza diversa, ma stesso identificatore:
         * rappresenta lo stesso giudice recuperato
         * in un altro contesto.
         */
        Utente giudice = new Utente(2L);

        control.confermaValutazione(
                sottomissione,
                giudice,
                dati
        );

        Valutazione valutazione =
                sottomissione.getValutazione();

        assertAll(
                () -> assertNotNull(valutazione),
                () -> assertEquals(
                        dati.giudizio(),
                        valutazione.getGiudizio()
                ),
                () -> assertEquals(
                        dati.punteggio(),
                        valutazione.getPunteggio()
                ),
                () -> assertSame(
                        giudice,
                        valutazione.getGiudice()
                ),
                () -> assertSame(
                        sottomissione,
                        valutazione.getSottomissione()
                ),
                () -> assertSame(
                        valutazione,
                        valutazioneRepository.valutazioneSalvata
                ),
                () -> assertEquals(
                        1,
                        valutazioneRepository.numeroSalvataggi
                ),
                () -> assertEquals(
                        0,
                        partecipazioneRepository.numeroSalvataggi
                )
        );
    }

    @Test
    void nonConfermaValutazioneSeGiudiceNonAssegnato() {
        Utente giudiceAssegnato = new Utente(2L);
        Hackathon hackathon =
                creaHackathonInValutazione(giudiceAssegnato);

        Partecipazione partecipazione =
                creaPartecipazione(hackathon, 10L);

        Sottomissione sottomissione =
                new Sottomissione(
                        partecipazione,
                        "Sottomissione"
                );

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        ValutazioneRepositoryFinto valutazioneRepository =
                new ValutazioneRepositoryFinto();

        ValutareSottomissioneControl control =
                new ValutareSottomissioneControl(
                        new HackathonRepositoryFinto(),
                        partecipazioneRepository,
                        valutazioneRepository
                );

        Utente altroGiudice = new Utente(99L);

        assertThrows(
                IllegalArgumentException.class,
                () -> control.confermaValutazione(
                        sottomissione,
                        altroGiudice,
                        creaDatiValutazioneValidi()
                )
        );

        assertAll(
                () -> assertNull(
                        sottomissione.getValutazione()
                ),
                () -> assertEquals(
                        0,
                        valutazioneRepository.numeroSalvataggi
                ),
                () -> assertEquals(
                        0,
                        partecipazioneRepository.numeroSalvataggi
                )
        );
    }

    private Hackathon creaHackathonInValutazione(
            Utente giudice
    ) {
        DatiHackathon dati = new DatiHackathon(
                "Hackathon passato",
                "Regolamento",
                "Qualità, completezza e innovazione",
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 1, 10),
                LocalDate.of(2020, 1, 12),
                "Roma",
                BigDecimal.valueOf(5000),
                5
        );

        return Hackathon.crea(
                dati,
                new Utente(1L),
                giudice,
                List.of(new Utente(3L))
        );
    }

    private Hackathon creaHackathonFuturo(
            Utente giudice
    ) {
        DatiHackathon dati = new DatiHackathon(
                "Hackathon futuro",
                "Regolamento",
                "Qualità, completezza e innovazione",
                LocalDate.of(2999, 1, 1),
                LocalDate.of(2999, 1, 10),
                LocalDate.of(2999, 1, 12),
                "Roma",
                BigDecimal.valueOf(5000),
                5
        );

        return Hackathon.crea(
                dati,
                new Utente(1L),
                giudice,
                List.of(new Utente(3L))
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

    private DatiValutazione creaDatiValutazioneValidi() {
        return new DatiValutazione(
                "Buona sottomissione",
                BigDecimal.valueOf(8)
        );
    }

    private static class HackathonRepositoryFinto
            implements HackathonRepository {

        private List<Hackathon> hackathonValutabili =
                List.of();

        private Utente giudiceRicevuto;
        private int numeroRecuperiValutabili;

        @Override
        public List<Hackathon> ottieniHackathonValutabili(
                Utente giudice
        ) {
            giudiceRicevuto = giudice;
            numeroRecuperiValutabili++;
            return hackathonValutabili;
        }

        @Override
        public List<Hackathon> ottieniHackathonSegnalabili(
                Utente mentore
        ) {
            return List.of();
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
        private int numeroSalvataggi;

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
    }

    private static class ValutazioneRepositoryFinto
            implements ValutazioneRepository {

        private Valutazione valutazioneSalvata;
        private int numeroSalvataggi;

        @Override
        public void salva(
                Valutazione valutazione
        ) {
            valutazioneSalvata = valutazione;
            numeroSalvataggi++;
        }
    }
}