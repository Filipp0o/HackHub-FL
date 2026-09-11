package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Utente;

import java.math.BigDecimal;

public interface SistemaPagamentoGateway {

    String avviaConfigurazioneBeneficiario(Utente responsabileTeam);

    String richiediErogazionePremio(
            BigDecimal importoPremio,
            String beneficiaryRef,
            String chiaveErogazione
    );

    class ConfigurazioneAnnullataException extends RuntimeException {

        public ConfigurazioneAnnullataException() {
            super("Configurazione annullata");
        }
    }
}