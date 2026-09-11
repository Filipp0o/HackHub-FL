package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SistemaPagamentoAdapterTest {

    @Test
    void rifiutaResponsabileTeamNullo() {
        SistemaPagamentoAdapter adapter =
                new SistemaPagamentoAdapter();

        assertThrows(
                NullPointerException.class,
                () -> adapter
                        .avviaConfigurazioneBeneficiario(null)
        );
    }

    @Test
    void restituisceRiferimentoOpacoDelBeneficiario() {
        SistemaPagamentoAdapter adapter =
                new SistemaPagamentoAdapter();

        String beneficiaryRef =
                adapter.avviaConfigurazioneBeneficiario(
                        new Utente(1L)
                );

        String uuid =
                beneficiaryRef.substring(
                        "beneficiary-".length()
                );

        assertAll(
                () -> assertNotNull(beneficiaryRef),
                () -> assertFalse(beneficiaryRef.isBlank()),
                () -> assertTrue(
                        beneficiaryRef.startsWith(
                                "beneficiary-"
                        )
                ),
                () -> assertDoesNotThrow(
                        () -> UUID.fromString(uuid)
                )
        );
    }

    @Test
    void generaRiferimentiBeneficiarioDistinti() {
        SistemaPagamentoAdapter adapter =
                new SistemaPagamentoAdapter();

        String primoRiferimento =
                adapter.avviaConfigurazioneBeneficiario(
                        new Utente(1L)
                );

        String secondoRiferimento =
                adapter.avviaConfigurazioneBeneficiario(
                        new Utente(2L)
                );

        assertNotEquals(
                primoRiferimento,
                secondoRiferimento
        );
    }

    @Test
    void rifiutaImportoPremioNullo() {
        SistemaPagamentoAdapter adapter =
                new SistemaPagamentoAdapter();

        assertThrows(
                NullPointerException.class,
                () -> adapter.richiediErogazionePremio(
                        null,
                        "beneficiary-ref", "premio-1"
                        )
        );
    }

    @Test
    void rifiutaImportoPremioNonPositivo() {
        SistemaPagamentoAdapter adapter =
                new SistemaPagamentoAdapter();

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> adapter.richiediErogazionePremio(
                                BigDecimal.ZERO,
                                "beneficiary-ref", "premio-2"
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> adapter.richiediErogazionePremio(
                                BigDecimal.valueOf(-1),
                                "beneficiary-ref", "premio-3"
                        )
                )
        );
    }

    @Test
    void rifiutaRiferimentoBeneficiarioAssente() {
        SistemaPagamentoAdapter adapter =
                new SistemaPagamentoAdapter();

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> adapter.richiediErogazionePremio(
                                BigDecimal.valueOf(1_000),
                                null, "premio-4"
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> adapter.richiediErogazionePremio(
                                BigDecimal.valueOf(1_000),
                                "   ", "premio-5"
                        )
                )
        );
    }

    @Test
    void restituisceRiferimentoOpacoDelPagamento() {
        SistemaPagamentoAdapter adapter =
                new SistemaPagamentoAdapter();

        String paymentRef =
                adapter.richiediErogazionePremio(
                        BigDecimal.valueOf(1_000),
                        "beneficiary-ref", "premio-6"
                        );

        assertAll(
                () -> assertNotNull(paymentRef),
                () -> assertFalse(paymentRef.isBlank()),
                () -> assertTrue(
                        paymentRef.startsWith(
                                "payment-"
                        )
                ),
                () -> assertFalse(
                        paymentRef.contains(
                                "beneficiary-ref"
                        )
                )
        );
    }

    @Test
    void generaRiferimentiPagamentoDistinti() {
        SistemaPagamentoAdapter adapter =
                new SistemaPagamentoAdapter();

        String primoRiferimento =
                adapter.richiediErogazionePremio(
                        BigDecimal.valueOf(1_000),
                        "beneficiary-ref", "premio-7"
                        );

        String secondoRiferimento =
                adapter.richiediErogazionePremio(
                        BigDecimal.valueOf(1_000),
                        "beneficiary-ref", "premio-8"
                        );

        assertNotEquals(
                primoRiferimento,
                secondoRiferimento
        );
    }
}