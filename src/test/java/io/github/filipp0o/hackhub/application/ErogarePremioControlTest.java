package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.RiscossionePremio;
import io.github.filipp0o.hackhub.domain.StatoHackathon;
import io.github.filipp0o.hackhub.domain.StatoRiscossionePremio;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErogarePremioControlTest {

    @Test
    void rifiutaDipendenzeNulle() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new ErogarePremioControl(
                                null,
                                new HackathonRepositoryFinto()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new ErogarePremioControl(
                                new SistemaPagamentoGatewayFinto(),
                                null
                        )
                )
        );
    }

    @Test
    void rifiutaParametriNulli() {
        ErogarePremioControl control =
                new ErogarePremioControl(
                        new SistemaPagamentoGatewayFinto(),
                        new HackathonRepositoryFinto()
                );

        Utente organizzatore = new Utente(1L);
        Hackathon hackathon =
                creaHackathonConRiscossionePronta();

        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.avviaErogazionePremio(
                                null,
                                hackathon
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.avviaErogazionePremio(
                                organizzatore,
                                null
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.confermaErogazionePremio(
                                null,
                                hackathon
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.confermaErogazionePremio(
                                organizzatore,
                                null
                        )
                )
        );
    }

    @Test
    void avviaErogazioneConRiscossionePronta() {
        SistemaPagamentoGatewayFinto gateway =
                new SistemaPagamentoGatewayFinto();
        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ErogarePremioControl control =
                new ErogarePremioControl(
                        gateway,
                        repository
                );

        Hackathon hackathon =
                creaHackathonConRiscossionePronta();

        /*
         * Istanza diversa, ma stesso identificatore:
         * rappresenta lo stesso organizzatore.
         */
        Utente organizzatore = new Utente(1L);

        assertDoesNotThrow(
                () -> control.avviaErogazionePremio(
                        organizzatore,
                        hackathon
                )
        );

        assertAll(
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
    void nonAvviaErogazioneSeOrganizzatoreNonAssegnato() {
        SistemaPagamentoGatewayFinto gateway =
                new SistemaPagamentoGatewayFinto();
        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ErogarePremioControl control =
                new ErogarePremioControl(
                        gateway,
                        repository
                );

        Hackathon hackathon =
                creaHackathonConRiscossionePronta();
        RiscossionePremio riscossione =
                hackathon.getRiscossionePremio();

        Utente altroOrganizzatore = new Utente(99L);

        assertThrows(
                IllegalArgumentException.class,
                () -> control.avviaErogazionePremio(
                        altroOrganizzatore,
                        hackathon
                )
        );

        assertAll(
                () -> assertEquals(
                        0,
                        gateway.numeroRichiesteErogazione
                ),
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
    void nonConfermaErogazioneSeOrganizzatoreNonAssegnato() {
        SistemaPagamentoGatewayFinto gateway =
                new SistemaPagamentoGatewayFinto();
        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ErogarePremioControl control =
                new ErogarePremioControl(
                        gateway,
                        repository
                );

        Hackathon hackathon =
                creaHackathonConRiscossionePronta();
        RiscossionePremio riscossione =
                hackathon.getRiscossionePremio();

        Utente altroOrganizzatore = new Utente(99L);

        assertThrows(
                IllegalArgumentException.class,
                () -> control.confermaErogazionePremio(
                        altroOrganizzatore,
                        hackathon
                )
        );

        assertAll(
                () -> assertEquals(
                        0,
                        gateway.numeroRichiesteErogazione
                ),
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
    void impedisceErogazioneSenzaRiscossione() {
        SistemaPagamentoGatewayFinto gateway =
                new SistemaPagamentoGatewayFinto();
        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ErogarePremioControl control =
                new ErogarePremioControl(
                        gateway,
                        repository
                );

        Utente organizzatore = new Utente(1L);
        Hackathon hackathon = creaHackathonConcluso();

        assertThrows(
                IllegalStateException.class,
                () -> control.confermaErogazionePremio(
                        organizzatore,
                        hackathon
                )
        );

        assertAll(
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
    void nonRichiedePagamentoSeRiscossioneNonPronta() {
        SistemaPagamentoGatewayFinto gateway =
                new SistemaPagamentoGatewayFinto();
        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ErogarePremioControl control =
                new ErogarePremioControl(
                        gateway,
                        repository
                );

        Utente organizzatore = new Utente(1L);
        Hackathon hackathon = creaHackathonConcluso();

        RiscossionePremio riscossione =
                RiscossionePremio.crea(hackathon);

        assertThrows(
                IllegalStateException.class,
                () -> control.confermaErogazionePremio(
                        organizzatore,
                        hackathon
                )
        );

        assertAll(
                () -> assertEquals(
                        StatoRiscossionePremio.DA_CONFIGURARE,
                        riscossione.getStato()
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
    void erogaPremioERegistraPagamento() {
        SistemaPagamentoGatewayFinto gateway =
                new SistemaPagamentoGatewayFinto();
        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ErogarePremioControl control =
                new ErogarePremioControl(
                        gateway,
                        repository
                );

        Hackathon hackathon =
                creaHackathonConRiscossionePronta();
        RiscossionePremio riscossione =
                hackathon.getRiscossionePremio();

        /*
         * Istanza diversa, ma stesso identificatore:
         * rappresenta lo stesso organizzatore.
         */
        Utente organizzatore = new Utente(1L);

        control.confermaErogazionePremio(
                organizzatore,
                hackathon
        );

        assertAll(
                () -> assertEquals(
                        1,
                        gateway.numeroRichiesteErogazione
                ),
                () -> assertEquals(
                        hackathon.getImportoPremio(),
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
    void mantieneRiscossioneProntaSePagamentoFallisce() {
        SistemaPagamentoGatewayFinto gateway =
                new SistemaPagamentoGatewayFinto();
        gateway.fallisceErogazione = true;

        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ErogarePremioControl control =
                new ErogarePremioControl(
                        gateway,
                        repository
                );

        Utente organizzatore = new Utente(1L);
        Hackathon hackathon =
                creaHackathonConRiscossionePronta();
        RiscossionePremio riscossione =
                hackathon.getRiscossionePremio();

        assertThrows(
                IllegalStateException.class,
                () -> control.confermaErogazionePremio(
                        organizzatore,
                        hackathon
                )
        );

        assertAll(
                () -> assertEquals(
                        1,
                        gateway.numeroRichiesteErogazione
                ),
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

    private Hackathon creaHackathonConRiscossionePronta() {
        Hackathon hackathon = creaHackathonConcluso();

        RiscossionePremio riscossione =
                RiscossionePremio.crea(hackathon);

        riscossione.configura("beneficiary-123");

        return hackathon;
    }

    private Hackathon creaHackathonConcluso() {
        Hackathon hackathon = creaHackathonValido();

        portaInValutazione(hackathon);

        Utente responsabile = new Utente(4L);

        Team team = Team.crea(
                "Team vincitore",
                responsabile,
                responsabile
        );

        Partecipazione partecipazione =
                new Partecipazione(
                        hackathon,
                        team
                );

        hackathon.registraPartecipazioneVincitrice(
                partecipazione
        );

        hackathon.concludi();

        return hackathon;
    }

    private Hackathon creaHackathonValido() {
        DatiHackathon dati = new DatiHackathon(
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

        return Hackathon.crea(
                dati,
                new Utente(1L),
                new Utente(2L),
                List.of(new Utente(3L))
        );
    }

    private void portaInValutazione(
            Hackathon hackathon
    ) {
        try {
            Field campoStato =
                    Hackathon.class.getDeclaredField("stato");

            campoStato.setAccessible(true);
            campoStato.set(
                    hackathon,
                    StatoHackathon.IN_VALUTAZIONE
            );
        } catch (ReflectiveOperationException eccezione) {
            throw new AssertionError(
                    "Impossibile predisporre l'hackathon in valutazione",
                    eccezione
            );
        }
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
                        "Pagamento non riuscito"
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

        @Override
        public List<Hackathon> ottieniTuttiHackathon() {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }

        @Override
        public Hackathon recuperaHackathon(
                Long hackathonId
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }
}