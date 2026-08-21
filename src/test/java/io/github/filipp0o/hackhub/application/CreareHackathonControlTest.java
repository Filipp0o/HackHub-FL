package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.StatoHackathon;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreareHackathonControlTest {

    @Test
    void rifiutaDipendenzeNulle() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new CreareHackathonControl(
                                null,
                                new HackathonRepositoryFinto()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new CreareHackathonControl(
                                new UtenteRepositoryFinto(),
                                null
                        )
                )
        );
    }

    @Test
    void recuperaUtentiAssegnabiliDalRepository() {
        Utente primoUtente = new Utente(1L);
        Utente secondoUtente = new Utente(2L);

        UtenteRepositoryFinto utenteRepository =
                new UtenteRepositoryFinto();

        utenteRepository.utentiAssegnabili =
                List.of(primoUtente, secondoUtente);

        CreareHackathonControl control =
                new CreareHackathonControl(
                        utenteRepository,
                        new HackathonRepositoryFinto()
                );

        List<Utente> risultato =
                control.recuperaUtentiAssegnabili();

        assertAll(
                () -> assertEquals(
                        List.of(primoUtente, secondoUtente),
                        risultato
                ),
                () -> assertEquals(
                        1,
                        utenteRepository.numeroRecuperi
                )
        );
    }

    @Test
    void verificaInformazioniEStaffValidiSenzaErrori() {
        CreareHackathonControl control =
                new CreareHackathonControl(
                        new UtenteRepositoryFinto(),
                        new HackathonRepositoryFinto()
                );

        List<String> errori =
                control.verificaInformazioniEStaff(
                        creaDatiValidi(),
                        new Utente(2L),
                        List.of(new Utente(3L))
                );

        assertTrue(errori.isEmpty());
    }

    @Test
    void rilevaInformazioniEStaffNonValidi() {
        CreareHackathonControl control =
                new CreareHackathonControl(
                        new UtenteRepositoryFinto(),
                        new HackathonRepositoryFinto()
                );

        DatiHackathon datiNonValidi =
                new DatiHackathon(
                        " ",
                        " ",
                        " ",
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 9),
                        " ",
                        BigDecimal.ZERO,
                        0
                );

        List<String> errori =
                control.verificaInformazioniEStaff(
                        datiNonValidi,
                        null,
                        List.of()
                );

        assertAll(
                () -> assertTrue(
                        errori.contains(
                                "Il nome è obbligatorio"
                        )
                ),
                () -> assertTrue(
                        errori.contains(
                                "Il regolamento è obbligatorio"
                        )
                ),
                () -> assertTrue(
                        errori.contains(
                                "I criteri di valutazione sono obbligatori"
                        )
                ),
                () -> assertTrue(
                        errori.contains(
                                "Il luogo è obbligatorio"
                        )
                ),
                () -> assertTrue(
                        errori.contains(
                                "La scadenza delle iscrizioni deve precedere la data di inizio"
                        )
                ),
                () -> assertTrue(
                        errori.contains(
                                "La data di fine deve essere successiva alla data di inizio"
                        )
                ),
                () -> assertTrue(
                        errori.contains(
                                "L'importo del premio deve essere maggiore di zero"
                        )
                ),
                () -> assertTrue(
                        errori.contains(
                                "La dimensione massima del team deve essere maggiore di zero"
                        )
                ),
                () -> assertTrue(
                        errori.contains(
                                "Il giudice è obbligatorio"
                        )
                ),
                () -> assertTrue(
                        errori.contains(
                                "Deve essere assegnato almeno un mentore"
                        )
                )
        );
    }

    @Test
    void rilevaDatiHackathonMancanti() {
        CreareHackathonControl control =
                new CreareHackathonControl(
                        new UtenteRepositoryFinto(),
                        new HackathonRepositoryFinto()
                );

        List<String> errori =
                control.verificaInformazioniEStaff(
                        null,
                        new Utente(2L),
                        List.of(new Utente(3L))
                );

        assertEquals(
                List.of(
                        "I dati dell'hackathon sono obbligatori"
                ),
                errori
        );
    }

    @Test
    void rilevaMentoriMancanti() {
        CreareHackathonControl control =
                new CreareHackathonControl(
                        new UtenteRepositoryFinto(),
                        new HackathonRepositoryFinto()
                );

        List<String> errori =
                control.verificaInformazioniEStaff(
                        creaDatiValidi(),
                        new Utente(2L),
                        null
                );

        assertEquals(
                List.of(
                        "La lista dei mentori è obbligatoria"
                ),
                errori
        );
    }

    @Test
    void creaESalvaHackathonConDatiEStaffSelezionati() {
        UtenteRepositoryFinto utenteRepository =
                new UtenteRepositoryFinto();

        HackathonRepositoryFinto hackathonRepository =
                new HackathonRepositoryFinto();

        CreareHackathonControl control =
                new CreareHackathonControl(
                        utenteRepository,
                        hackathonRepository
                );

        DatiHackathon dati = creaDatiValidi();
        Utente organizzatore = new Utente(1L);
        Utente giudice = new Utente(2L);

        List<Utente> mentori =
                List.of(
                        new Utente(3L),
                        new Utente(4L)
                );

        control.crea(
                dati,
                organizzatore,
                giudice,
                mentori
        );

        Hackathon hackathonSalvato =
                hackathonRepository.hackathonSalvato;

        assertAll(
                () -> assertNotNull(hackathonSalvato),
                () -> assertEquals(
                        1,
                        hackathonRepository.numeroSalvataggi
                ),
                () -> assertEquals(
                        dati.nome(),
                        hackathonSalvato.getNome()
                ),
                () -> assertEquals(
                        dati.regolamento(),
                        hackathonSalvato.getRegolamento()
                ),
                () -> assertEquals(
                        dati.criteriValutazione(),
                        hackathonSalvato.getCriteriValutazione()
                ),
                () -> assertEquals(
                        dati.scadenzaIscrizioni(),
                        hackathonSalvato.getScadenzaIscrizioni()
                ),
                () -> assertEquals(
                        dati.dataInizio(),
                        hackathonSalvato.getDataInizio()
                ),
                () -> assertEquals(
                        dati.dataFine(),
                        hackathonSalvato.getDataFine()
                ),
                () -> assertEquals(
                        dati.luogo(),
                        hackathonSalvato.getLuogo()
                ),
                () -> assertEquals(
                        dati.importoPremio(),
                        hackathonSalvato.getImportoPremio()
                ),
                () -> assertEquals(
                        dati.dimensioneMassimaTeam(),
                        hackathonSalvato.getDimensioneMassimaTeam()
                ),
                () -> assertSame(
                        organizzatore,
                        hackathonSalvato.getOrganizzatore()
                ),
                () -> assertSame(
                        giudice,
                        hackathonSalvato.getGiudice()
                ),
                () -> assertEquals(
                        mentori,
                        hackathonSalvato.getMentori()
                ),
                () -> assertEquals(
                        StatoHackathon.IN_ISCRIZIONE,
                        hackathonSalvato.getStato()
                )
        );
    }

    @Test
    void nonSalvaHackathonSeCreazioneFallisce() {
        HackathonRepositoryFinto hackathonRepository =
                new HackathonRepositoryFinto();

        CreareHackathonControl control =
                new CreareHackathonControl(
                        new UtenteRepositoryFinto(),
                        hackathonRepository
                );

        DatiHackathon dati = creaDatiValidi();
        Utente organizzatore = new Utente(1L);
        Utente giudice = new Utente(2L);

        assertThrows(
                IllegalArgumentException.class,
                () -> control.crea(
                        dati,
                        organizzatore,
                        giudice,
                        List.of()
                )
        );

        assertAll(
                () -> assertEquals(
                        0,
                        hackathonRepository.numeroSalvataggi
                ),
                () -> assertNull(
                        hackathonRepository.hackathonSalvato
                )
        );
    }

    private DatiHackathon creaDatiValidi() {
        return new DatiHackathon(
                "HackHub 2026",
                "Regolamento ufficiale",
                "Criteri di valutazione",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 12),
                "Roma",
                BigDecimal.valueOf(5000),
                5
        );
    }

    private static class UtenteRepositoryFinto
            implements UtenteRepository {

        private List<Utente> utentiAssegnabili =
                List.of();

        private int numeroRecuperi;

        @Override
        public List<Utente> recuperaUtentiAssegnabili() {
            numeroRecuperi++;
            return utentiAssegnabili;
        }
    }

    private static class HackathonRepositoryFinto
            implements HackathonRepository {

        private Hackathon hackathonSalvato;
        private int numeroSalvataggi;

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
            return List.of();
        }

        @Override
        public void salva(Hackathon hackathon) {
            hackathonSalvato = hackathon;
            numeroSalvataggi++;
        }

        @Override
        public List<Hackathon> ottieniHackathonApertiAlleIscrizioni() {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }
}