package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultareHackathonControlTest {

    @Test
    void rifiutaRepositoryNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new ConsultareHackathonControl(
                        null
                )
        );
    }

    @Test
    void recuperaTuttiGliHackathon() {
        Hackathon primo = creaHackathon(
                "Primo Hackathon"
        );

        Hackathon secondo = creaHackathon(
                "Secondo Hackathon"
        );

        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        repository.hackathonRestituiti =
                List.of(
                        primo,
                        secondo
                );

        ConsultareHackathonControl control =
                new ConsultareHackathonControl(
                        repository
                );

        List<Hackathon> risultato =
                control.consultaHackathon();

        assertAll(
                () -> assertEquals(
                        List.of(
                                primo,
                                secondo
                        ),
                        risultato
                ),
                () -> assertEquals(
                        1,
                        repository.numeroConsultazioni
                )
        );
    }

    @Test
    void restituisceListaVuotaQuandoNonEsistonoHackathon() {
        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ConsultareHackathonControl control =
                new ConsultareHackathonControl(
                        repository
                );

        assertEquals(
                List.of(),
                control.consultaHackathon()
        );
    }

    @Test
    void selezionaHackathonTramiteIdentificativo() {
        Hackathon hackathon = creaHackathon(
                "Hackathon selezionato"
        );

        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        repository.hackathonRecuperato =
                hackathon;

        ConsultareHackathonControl control =
                new ConsultareHackathonControl(
                        repository
                );

        Hackathon risultato =
                control.selezionaHackathon(
                        hackathon.getId()
                );

        assertAll(
                () -> assertSame(
                        hackathon,
                        risultato
                ),
                () -> assertEquals(
                        hackathon.getId(),
                        repository.hackathonIdRicevuto
                ),
                () -> assertEquals(
                        1,
                        repository.numeroRecuperi
                )
        );
    }

    @Test
    void rifiutaIdentificativoNullo() {
        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ConsultareHackathonControl control =
                new ConsultareHackathonControl(
                        repository
                );

        assertThrows(
                NullPointerException.class,
                () -> control.selezionaHackathon(
                        null
                )
        );

        assertEquals(
                0,
                repository.numeroRecuperi
        );
    }

    @Test
    void rifiutaHackathonNulloRestituitoDalRepository() {
        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ConsultareHackathonControl control =
                new ConsultareHackathonControl(
                        repository
                );

        assertThrows(
                NullPointerException.class,
                () -> control.selezionaHackathon(
                        1L
                )
        );
    }

    private Hackathon creaHackathon(
            String nome
    ) {
        LocalDate oggi = LocalDate.now();

        DatiHackathon dati =
                new DatiHackathon(
                        nome,
                        "Regolamento",
                        "Criteri di valutazione",
                        oggi.plusDays(1),
                        oggi.plusDays(2),
                        oggi.plusDays(5),
                        "Camerino",
                        BigDecimal.valueOf(1_000),
                        5
                );

        return Hackathon.crea(
                dati,
                new Utente(1L),
                new Utente(2L),
                List.of(
                        new Utente(3L)
                )
        );
    }

    private static class HackathonRepositoryFinto
            implements HackathonRepository {

        private List<Hackathon> hackathonRestituiti =
                List.of();

        private Hackathon hackathonRecuperato;
        private Long hackathonIdRicevuto;

        private int numeroConsultazioni;
        private int numeroRecuperi;

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
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }

        @Override
        public List<Hackathon> ottieniTuttiHackathon() {
            numeroConsultazioni++;
            return hackathonRestituiti;
        }

        @Override
        public Hackathon recuperaHackathon(
                Long hackathonId
        ) {
            hackathonIdRicevuto = hackathonId;
            numeroRecuperi++;

            return hackathonRecuperato;
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
}