package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.ConsultareHackathonControl;
import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultareHackathonBoundaryTest {

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new ConsultareHackathonBoundary(
                        null
                )
        );
    }

    @Test
    void restituisceRiepilogoDegliHackathon() {
        Hackathon primo = creaHackathon(
                "HackHub Marche",
                "Ancona"
        );

        Hackathon secondo = creaHackathon(
                "HackHub Italia",
                "Roma"
        );

        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        repository.hackathonRestituiti =
                List.of(
                        primo,
                        secondo
                );

        ConsultareHackathonBoundary boundary =
                creaBoundary(repository);

        List<ConsultareHackathonBoundary.RiepilogoHackathon>
                risultato = boundary.consultaHackathon();

        assertAll(
                () -> assertEquals(
                        2,
                        risultato.size()
                ),
                () -> verificaRiepilogo(
                        risultato.getFirst(),
                        primo
                ),
                () -> verificaRiepilogo(
                        risultato.get(1),
                        secondo
                ),
                () -> assertEquals(
                        1,
                        repository.numeroConsultazioni
                )
        );
    }

    @Test
    void restituisceListaVuotaQuandoNonCiSonoHackathon() {
        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ConsultareHackathonBoundary boundary =
                creaBoundary(repository);

        assertAll(
                () -> assertEquals(
                        List.of(),
                        boundary.consultaHackathon()
                ),
                () -> assertEquals(
                        1,
                        repository.numeroConsultazioni
                )
        );
    }

    @Test
    void restituisceInformazioniPubblicheHackathonSelezionato() {
        Hackathon hackathon = creaHackathon(
                "HackHub Marche",
                "Ancona"
        );

        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        repository.hackathonRecuperato =
                hackathon;

        ConsultareHackathonBoundary boundary =
                creaBoundary(repository);

        ConsultareHackathonBoundary.InformazioniHackathon
                risultato = boundary.selezionaHackathon(
                hackathon.getId()
        );

        assertAll(
                () -> assertEquals(
                        hackathon.getId(),
                        risultato.id()
                ),
                () -> assertEquals(
                        hackathon.getNome(),
                        risultato.nome()
                ),
                () -> assertEquals(
                        hackathon.getRegolamento(),
                        risultato.regolamento()
                ),
                () -> assertEquals(
                        hackathon.getCriteriValutazione(),
                        risultato.criteriValutazione()
                ),
                () -> assertEquals(
                        hackathon.getScadenzaIscrizioni(),
                        risultato.scadenzaIscrizioni()
                ),
                () -> assertEquals(
                        hackathon.getDataInizio(),
                        risultato.dataInizio()
                ),
                () -> assertEquals(
                        hackathon.getDataFine(),
                        risultato.dataFine()
                ),
                () -> assertEquals(
                        hackathon.getLuogo(),
                        risultato.luogo()
                ),
                () -> assertEquals(
                        hackathon.getImportoPremio(),
                        risultato.importoPremio()
                ),
                () -> assertEquals(
                        hackathon.getDimensioneMassimaTeam(),
                        risultato.dimensioneMassimaTeam()
                ),
                () -> assertEquals(
                        hackathon.getStato(),
                        risultato.stato()
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
    void restituisceNotFoundPerHackathonAssente() {
        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        repository.hackathonAssente = true;

        ConsultareHackathonBoundary boundary =
                creaBoundary(repository);

        ResponseStatusException eccezione =
                assertThrows(
                        ResponseStatusException.class,
                        () -> boundary.selezionaHackathon(
                                999L
                        )
                );

        assertAll(
                () -> assertEquals(
                        HttpStatus.NOT_FOUND,
                        eccezione.getStatusCode()
                ),
                () -> assertEquals(
                        "Hackathon non trovato",
                        eccezione.getReason()
                ),
                () -> assertEquals(
                        999L,
                        repository.hackathonIdRicevuto
                )
        );
    }

    @Test
    void rifiutaIdentificativoNullo() {
        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ConsultareHackathonBoundary boundary =
                creaBoundary(repository);

        assertThrows(
                NullPointerException.class,
                () -> boundary.selezionaHackathon(
                        null
                )
        );

        assertEquals(
                0,
                repository.numeroRecuperi
        );
    }

    private ConsultareHackathonBoundary creaBoundary(
            HackathonRepository repository
    ) {
        ConsultareHackathonControl control =
                new ConsultareHackathonControl(
                        repository
                );

        return new ConsultareHackathonBoundary(
                control
        );
    }

    private void verificaRiepilogo(
            ConsultareHackathonBoundary.RiepilogoHackathon riepilogo,
            Hackathon hackathon
    ) {
        assertAll(
                () -> assertEquals(
                        hackathon.getId(),
                        riepilogo.id()
                ),
                () -> assertEquals(
                        hackathon.getNome(),
                        riepilogo.nome()
                ),
                () -> assertEquals(
                        hackathon.getStato(),
                        riepilogo.stato()
                ),
                () -> assertEquals(
                        hackathon.getScadenzaIscrizioni(),
                        riepilogo.scadenzaIscrizioni()
                ),
                () -> assertEquals(
                        hackathon.getDataInizio(),
                        riepilogo.dataInizio()
                ),
                () -> assertEquals(
                        hackathon.getDataFine(),
                        riepilogo.dataFine()
                ),
                () -> assertEquals(
                        hackathon.getLuogo(),
                        riepilogo.luogo()
                )
        );
    }

    private Hackathon creaHackathon(
            String nome,
            String luogo
    ) {
        DatiHackathon dati =
                new DatiHackathon(
                        nome,
                        "Regolamento pubblico",
                        "Criteri di valutazione pubblici",
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 9, 12),
                        luogo,
                        BigDecimal.valueOf(2_500),
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
        private boolean hackathonAssente;

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

            if (hackathonAssente) {
                throw new IllegalStateException(
                        "Hackathon non trovato"
                );
            }

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