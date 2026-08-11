package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiscossionePremioTest {

    @Test
    void creaRiscossioneDaConfigurareECollegaHackathon() {
        Hackathon hackathon = creaHackathonValido();

        assertNull(hackathon.getRiscossionePremio());

        RiscossionePremio riscossione =
                RiscossionePremio.crea(hackathon);

        assertAll(
                () -> assertSame(
                        hackathon,
                        riscossione.getHackathon()
                ),
                () -> assertSame(
                        riscossione,
                        hackathon.getRiscossionePremio()
                ),
                () -> assertEquals(
                        StatoRiscossionePremio.DA_CONFIGURARE,
                        riscossione.getStato()
                ),
                () -> assertNull(riscossione.getBeneficiaryRef()),
                () -> assertNull(riscossione.getPaymentRef())
        );
    }

    @Test
    void rifiutaHackathonNullo() {
        assertThrows(
                NullPointerException.class,
                () -> RiscossionePremio.crea(null)
        );
    }

    @Test
    void impedisceSecondaRiscossioneDelloStessoHackathon() {
        Hackathon hackathon = creaHackathonValido();

        RiscossionePremio primaRiscossione =
                RiscossionePremio.crea(hackathon);

        assertThrows(
                IllegalStateException.class,
                () -> RiscossionePremio.crea(hackathon)
        );

        assertSame(
                primaRiscossione,
                hackathon.getRiscossionePremio()
        );
    }

    @Test
    void configuraRiscossioneValida() {
        RiscossionePremio riscossione = creaRiscossione();

        riscossione.configura("beneficiary-123");

        assertAll(
                () -> assertEquals(
                        "beneficiary-123",
                        riscossione.getBeneficiaryRef()
                ),
                () -> assertEquals(
                        StatoRiscossionePremio.PRONTA,
                        riscossione.getStato()
                ),
                () -> assertNull(riscossione.getPaymentRef())
        );
    }

    @Test
    void rifiutaBeneficiaryRefNulloOVuoto() {
        for (String beneficiaryRef : new String[]{null, "", "   "}) {
            RiscossionePremio riscossione = creaRiscossione();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> riscossione.configura(beneficiaryRef)
            );

            assertAll(
                    () -> assertEquals(
                            StatoRiscossionePremio.DA_CONFIGURARE,
                            riscossione.getStato()
                    ),
                    () -> assertNull(riscossione.getBeneficiaryRef())
            );
        }
    }

    @Test
    void impedisceUnaSecondaConfigurazione() {
        RiscossionePremio riscossione = creaRiscossione();
        riscossione.configura("beneficiary-123");

        assertThrows(
                IllegalStateException.class,
                () -> riscossione.configura("beneficiary-456")
        );

        assertAll(
                () -> assertEquals(
                        StatoRiscossionePremio.PRONTA,
                        riscossione.getStato()
                ),
                () -> assertEquals(
                        "beneficiary-123",
                        riscossione.getBeneficiaryRef()
                )
        );
    }

    @Test
    void impedisceErogazionePrimaDellaConfigurazione() {
        RiscossionePremio riscossione = creaRiscossione();

        assertThrows(
                IllegalStateException.class,
                () -> riscossione.registraErogazione("payment-123")
        );

        assertAll(
                () -> assertEquals(
                        StatoRiscossionePremio.DA_CONFIGURARE,
                        riscossione.getStato()
                ),
                () -> assertNull(riscossione.getPaymentRef())
        );
    }

    @Test
    void registraErogazioneValida() {
        RiscossionePremio riscossione = creaRiscossione();
        riscossione.configura("beneficiary-123");

        riscossione.registraErogazione("payment-123");

        assertAll(
                () -> assertEquals(
                        StatoRiscossionePremio.EROGATA,
                        riscossione.getStato()
                ),
                () -> assertEquals(
                        "beneficiary-123",
                        riscossione.getBeneficiaryRef()
                ),
                () -> assertEquals(
                        "payment-123",
                        riscossione.getPaymentRef()
                )
        );
    }

    @Test
    void rifiutaPaymentRefNulloOVuoto() {
        for (String paymentRef : new String[]{null, "", "   "}) {
            RiscossionePremio riscossione = creaRiscossione();
            riscossione.configura("beneficiary-123");

            assertThrows(
                    IllegalArgumentException.class,
                    () -> riscossione.registraErogazione(paymentRef)
            );

            assertAll(
                    () -> assertEquals(
                            StatoRiscossionePremio.PRONTA,
                            riscossione.getStato()
                    ),
                    () -> assertNull(riscossione.getPaymentRef())
            );
        }
    }

    @Test
    void impedisceUnaSecondaErogazione() {
        RiscossionePremio riscossione = creaRiscossione();
        riscossione.configura("beneficiary-123");
        riscossione.registraErogazione("payment-123");

        assertThrows(
                IllegalStateException.class,
                () -> riscossione.registraErogazione("payment-456")
        );

        assertAll(
                () -> assertEquals(
                        StatoRiscossionePremio.EROGATA,
                        riscossione.getStato()
                ),
                () -> assertEquals(
                        "payment-123",
                        riscossione.getPaymentRef()
                )
        );
    }

    private RiscossionePremio creaRiscossione() {
        return RiscossionePremio.crea(
                creaHackathonValido()
        );
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
}