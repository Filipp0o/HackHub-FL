package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.ErogarePremioControl;
import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.SistemaPagamentoGateway;
import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.RiscossionePremio;
import io.github.filipp0o.hackhub.domain.StatoRiscossionePremio;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ErogarePremioBoundaryTest {

    private Utente organizzatore;
    private Hackathon hackathon;
    private RiscossionePremio riscossione;
    private SistemaPagamentoGatewayFinto gateway;
    private HackathonRepositoryFinto repository;
    private ErogarePremioBoundary boundary;

    @BeforeEach
    void configuraBoundary() {
        organizzatore = new Utente(1L);
        hackathon = creaHackathonConRiscossionePronta();
        riscossione = hackathon.getRiscossionePremio();
        gateway = new SistemaPagamentoGatewayFinto();
        repository = new HackathonRepositoryFinto();

        ErogarePremioControl control =
                new ErogarePremioControl(
                        gateway,
                        repository
                );

        boundary = new ErogarePremioBoundary(control);
    }

    @Test
    void mostraRiepilogoErogazione() {
        ErogarePremioBoundary.RiepilogoErogazione
                risultato = boundary
                .selezionaErogazionePremio(
                        organizzatore,
                        hackathon
                );

        assertAll(
                () -> assertEquals(
                        "Team vincitore",
                        risultato.nomeTeamVincitore()
                ),
                () -> assertEquals(
                        4L,
                        risultato.responsabileTeamId()
                ),
                () -> assertEquals(
                        BigDecimal.valueOf(5000),
                        risultato.importoPremio()
                ),
                () -> assertEquals(
                        0,
                        gateway.numeroRichiesteErogazione
                ),
                () -> assertNull(
                        repository.hackathonSalvato
                )
        );
    }

    @Test
    void confermaErogazioneDelPremio() {
        boundary.confermaErogazionePremio(
                organizzatore,
                hackathon
        );

        assertAll(
                () -> assertEquals(
                        1,
                        gateway.numeroRichiesteErogazione
                ),
                () -> assertEquals(
                        BigDecimal.valueOf(5000),
                        gateway.importoRicevuto
                ),
                () -> assertEquals(
                        "beneficiary-123",
                        gateway.beneficiaryRefRicevuto
                ),
                () -> assertEquals(
                        StatoRiscossionePremio.EROGATA,
                        riscossione.getStato()
                ),
                () -> assertEquals(
                        "payment-123",
                        riscossione.getPaymentRef()
                ),
                () -> assertSame(
                        hackathon,
                        repository.hackathonSalvato
                )
        );
    }

    @Test
    void mantieneRiscossioneProntaSeErogazioneFallisce() {
        gateway.fallisceErogazione = true;

        assertThrows(
                IllegalStateException.class,
                () -> boundary.confermaErogazionePremio(
                        organizzatore,
                        hackathon
                )
        );

        assertAll(
                () -> assertEquals(
                        StatoRiscossionePremio.PRONTA,
                        riscossione.getStato()
                ),
                () -> assertNull(
                        riscossione.getPaymentRef()
                ),
                () -> assertNull(
                        repository.hackathonSalvato
                )
        );
    }

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new ErogarePremioBoundary(null)
        );
    }

    private Hackathon creaHackathonConRiscossionePronta() {
        LocalDate oggi = LocalDate.now();

        DatiHackathon dati = new DatiHackathon(
                "HackHub 2026",
                "Regolamento ufficiale",
                "Criteri di valutazione",
                oggi.minusDays(10),
                oggi.minusDays(5),
                oggi.minusDays(1),
                "Camerino",
                BigDecimal.valueOf(5000),
                5
        );

        Hackathon risultato = Hackathon.crea(
                dati,
                organizzatore,
                new Utente(2L),
                List.of(new Utente(3L))
        );

        risultato.aggiornaStato(oggi);

        Utente responsabile = new Utente(4L);

        Team team = Team.crea(
                "Team vincitore",
                responsabile,
                responsabile
        );

        Partecipazione partecipazione =
                new Partecipazione(
                        risultato,
                        team
                );

        risultato.registraPartecipazioneVincitrice(
                partecipazione
        );

        risultato.concludi();

        RiscossionePremio premio =
                RiscossionePremio.crea(risultato);

        premio.configura("beneficiary-123");

        return risultato;
    }

    private static class SistemaPagamentoGatewayFinto
            implements SistemaPagamentoGateway {

        private boolean fallisceErogazione;
        private int numeroRichiesteErogazione;
        private BigDecimal importoRicevuto;
        private String beneficiaryRefRicevuto;

        @Override
        public String avviaConfigurazioneBeneficiario(
                Utente responsabileTeam
        ) {
            return "beneficiary-123";
        }

        @Override
        public String richiediErogazionePremio(
                BigDecimal importoPremio,
                String beneficiaryRef
        ) {
            numeroRichiesteErogazione++;
            importoRicevuto = importoPremio;
            beneficiaryRefRicevuto = beneficiaryRef;

            if (fallisceErogazione) {
                throw new IllegalStateException(
                        "Erogazione non completata"
                );
            }

            return "payment-123";
        }
    }

    private static class HackathonRepositoryFinto
            implements HackathonRepository {

        private Hackathon hackathonSalvato;

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
        public void salva(
                Hackathon hackathon
        ) {
            hackathonSalvato = hackathon;
        }

        @Override
        public List<Hackathon> ottieniHackathonApertiAlleIscrizioni() {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }
}