package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.SistemaPagamentoGateway;
import io.github.filipp0o.hackhub.domain.Utente;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class SistemaPagamentoAdapter
        implements SistemaPagamentoGateway {

    @Override
    public String avviaConfigurazioneBeneficiario(
            Utente responsabileTeam
    ) {
        Objects.requireNonNull(
                responsabileTeam,
                "Il responsabile del team è obbligatorio"
        );

        return "beneficiary-" + UUID.randomUUID();
    }

    @Override
    public String richiediErogazionePremio(
            BigDecimal importoPremio,
            String beneficiaryRef
    ) {
        BigDecimal importoValido =
                Objects.requireNonNull(
                        importoPremio,
                        "L'importo del premio è obbligatorio"
                );

        if (importoValido.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "L'importo del premio deve essere maggiore di zero"
            );
        }

        if (beneficiaryRef == null
                || beneficiaryRef.isBlank()) {
            throw new IllegalArgumentException(
                    "Il riferimento del beneficiario è obbligatorio"
            );
        }

        return "payment-" + UUID.randomUUID();
    }
}